/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.bisq1_bridge;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.threading.ExecutorFactory;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.services.BsqBlockGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.BurningmanGrpcService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Bisq1BridgeService implements Service, Node.Listener {
    @Getter
    public static class Config {
        private final int grpcServicePort;
        private final int initialDelayInSeconds;
        private final int throttleDelayInSeconds;
        private final int numConnectionsForRepublish;

        public Config(int grpcServicePort,
                      int initialDelayInSeconds,
                      int throttleDelayInSeconds,
                      int numConnectionsForRepublish) {
            this.grpcServicePort = grpcServicePort;
            this.initialDelayInSeconds = initialDelayInSeconds;
            this.throttleDelayInSeconds = throttleDelayInSeconds;
            this.numConnectionsForRepublish = numConnectionsForRepublish;
        }

        public static Bisq1BridgeService.Config from(com.typesafe.config.Config config) {
            com.typesafe.config.Config grpcService = config.getConfig("grpcService");
            return new Bisq1BridgeService.Config(grpcService.getInt("port"),
                    config.getInt("initialDelayInSeconds"),
                    config.getInt("throttleDelayInSeconds"),
                    config.getInt("numConnectionsForRepublish"));
        }
    }

    private final GrpcClient grpcClient;
    private final Bisq1BridgeService.Config config;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final BsqBlockGrpcService bsqBlockGrpcService;
    private final BurningmanGrpcService burningmanGrpcService;
    private final Bisq1BridgeRequestService bisq1BridgeRequestService;
    private final BlockingQueue<AuthorizedDistributedData> queue = new LinkedBlockingQueue<>(10000);
    @Nullable
    private volatile ScheduledExecutorService executor;
    private final Object executorLock = new Object();

    public Bisq1BridgeService(Config config,
                              PersistenceService persistenceService,
                              IdentityService identityService,
                              NetworkService networkService,
                              AuthorizedBondedRolesService authorizedBondedRolesService,
                              PrivateKey authorizedPrivateKey,
                              PublicKey authorizedPublicKey,
                              boolean staticPublicKeysProvided,
                              AuthorizedOracleNode myAuthorizedOracleNode) {

        grpcClient = new GrpcClient(config.getGrpcServicePort());
        this.config = config;
        this.identityService = identityService;
        this.networkService = networkService;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;

        bsqBlockGrpcService = new BsqBlockGrpcService(staticPublicKeysProvided,
                grpcClient,
                queue);

        burningmanGrpcService = new BurningmanGrpcService(staticPublicKeysProvided,
                grpcClient,
                queue);

        bisq1BridgeRequestService = new Bisq1BridgeRequestService(persistenceService,
                identityService,
                networkService,
                authorizedBondedRolesService,
                authorizedPrivateKey,
                authorizedPublicKey,
                staticPublicKeysProvided,
                myAuthorizedOracleNode,
                grpcClient);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        networkService.addDefaultNodeListener(this);

        return grpcClient.initialize()
                .thenCompose(result -> bisq1BridgeRequestService.initialize())
                .thenCompose(result -> bsqBlockGrpcService.initialize())
                .thenCompose(result -> burningmanGrpcService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");

        networkService.removeDefaultNodeListener(this);

        queue.clear();

        ScheduledExecutorService toShutdown;
        synchronized (executorLock) {
            toShutdown = executor;
            executor = null;
        }
        if (toShutdown != null) {
            ExecutorFactory.shutdownAndAwaitTermination(toShutdown, 100);
        }

        return burningmanGrpcService.shutdown()
                .thenCompose(result -> bsqBlockGrpcService.shutdown())
                .thenCompose(result -> bisq1BridgeRequestService.shutdown())
                .thenCompose(result -> grpcClient.shutdown());
    }

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
    }

    @Override
    public void onConnection(Connection connection) {
        // We ensure with the null check against executor that we only start the ScheduledExecutorService once.
        if (executor != null) return;

        synchronized (executorLock) {
            if (executor != null) return;
            int numAllConnections = networkService.getNumConnectionsOnAllTransports();
            if (numAllConnections >= config.getNumConnectionsForRepublish()) {
                networkService.removeDefaultNodeListener(this);

                ScheduledExecutorService tmp = ExecutorFactory.newSingleThreadScheduledExecutor("Bisq1BridgePublisher");
                tmp.scheduleWithFixedDelay(() -> {
                    AuthorizedDistributedData data = queue.poll();
                    if (data != null) {
                        publishAuthorizedData(data);
                    }
                }, config.getInitialDelayInSeconds(), config.getThrottleDelayInSeconds(), TimeUnit.SECONDS);
                executor = tmp;
            }
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    private void publishAuthorizedData(AuthorizedDistributedData data) {
        Identity identity = identityService.getOrCreateDefaultIdentity();
        networkService.publishAuthorizedData(data,
                identity.getNetworkIdWithKeyPair().getKeyPair(),
                authorizedPrivateKey,
                authorizedPublicKey);
    }
}