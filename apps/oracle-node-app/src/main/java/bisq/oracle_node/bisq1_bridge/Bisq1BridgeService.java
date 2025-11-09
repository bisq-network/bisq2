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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.PublishDateAware;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.services.BsqBlockGrpcService;
import bisq.oracle_node.bisq1_bridge.grpc.services.BurningmanGrpcService;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Processes the queues for republishing data on a short interval (1 sec by default).
 * For AuthorizedProofOfBurnData and AuthorizedBondedReputationData we check if the associated user profile is in our
 * network DB and only publish those. Data from inactive user profiles got swapped to the end of the queues, thus they
 * will get re-evaluated at the next scheduler run.
 * Republishing of AuthorizedAccountAgeData, AuthorizedSignedWitnessData and AuthorizedTimestampData is done by the
 * user by re-doing the requests before TTL expires.
 */
@Slf4j
public class Bisq1BridgeService implements Service, Node.Listener, DataService.Listener {
    @Getter
    public static class Config {
        private final int grpcServicePort;
        private final int initialDelayInSeconds; // 120 sec by default
        private final int throttleDelayInSeconds; // 1 sec by default
        private final int numConnectionsForRepublish;
        private final boolean ignorePublishAgeCheck;

        public Config(int grpcServicePort,
                      int initialDelayInSeconds,
                      int throttleDelayInSeconds,
                      int numConnectionsForRepublish,
                      boolean ignorePublishAgeCheck) {
            this.grpcServicePort = grpcServicePort;
            this.initialDelayInSeconds = initialDelayInSeconds;
            this.throttleDelayInSeconds = throttleDelayInSeconds;
            this.numConnectionsForRepublish = numConnectionsForRepublish;
            this.ignorePublishAgeCheck = ignorePublishAgeCheck;
        }

        public static Bisq1BridgeService.Config from(com.typesafe.config.Config config) {
            com.typesafe.config.Config grpcService = config.getConfig("grpcService");
            return new Bisq1BridgeService.Config(grpcService.getInt("port"),
                    config.getInt("initialDelayInSeconds"),
                    config.getInt("throttleDelayInSeconds"),
                    config.getInt("numConnectionsForRepublish"),
                    config.getBoolean("ignorePublishAgeCheck"));
        }
    }

    private final GrpcClient grpcClient;
    private final Bisq1BridgeService.Config config;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final PrivateKey authorizedPrivateKey;
    private final PublicKey authorizedPublicKey;
    private final BsqBlockGrpcService bsqBlockGrpcService;
    private final BurningmanGrpcService burningmanGrpcService;
    private final AuthorizedOracleNode myAuthorizedOracleNode;
    private final Bisq1BridgeRequestService bisq1BridgeRequestService;
    private final BlockingQueue<AuthorizedBondedRole> authorizedBondedRoleQueue = new LinkedBlockingQueue<>(10000);
    private final Set<ByteArray> userProfileProofOfBurnHashes = new ObservableSet<>();
    private final Set<ByteArray> userProfileBondedReputationHashes = new ObservableSet<>();

    @Nullable
    private KeyPair keyPair;
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
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        this.myAuthorizedOracleNode = myAuthorizedOracleNode;
        this.authorizedPrivateKey = authorizedPrivateKey;
        this.authorizedPublicKey = authorizedPublicKey;

        bsqBlockGrpcService = new BsqBlockGrpcService(staticPublicKeysProvided, grpcClient);

        burningmanGrpcService = new BurningmanGrpcService(staticPublicKeysProvided, grpcClient);

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

        networkService.addDataServiceListener(this);
        String storageKey = UserProfile.class.getSimpleName();
        networkService.getDataService()
                .stream() // turns Optional<DataService> into Stream<DataService>
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(UserProfile.class::isInstance)
                                .map(UserProfile.class::cast)
                )
                .forEach(this::handleUserProfileAdded);

        return grpcClient.initialize()
                .thenCompose(result -> bisq1BridgeRequestService.initialize())
                .thenCompose(result -> bsqBlockGrpcService.initialize());
        // v.2.1.7 does not know AuthorizedBurningmanListByBlock thus we do not
        // .thenCompose(result -> burningmanGrpcService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");

        networkService.removeDefaultNodeListener(this);
        networkService.removeDataServiceListener(this);

        ScheduledExecutorService toShutdown;
        synchronized (executorLock) {
            toShutdown = executor;
            executor = null;
        }
        if (toShutdown != null) {
            ExecutorFactory.shutdownAndAwaitTermination(toShutdown, 100);
        }

        // v.2.1.7 does not know AuthorizedBurningmanListByBlock thus we do not use burningmanGrpcService yet
        //burningmanGrpcService.shutdown()
        return bsqBlockGrpcService.shutdown()
                .thenCompose(result -> bisq1BridgeRequestService.shutdown())
                .thenCompose(result -> grpcClient.shutdown());
    }


    /* --------------------------------------------------------------------- */
    // Node.Listener
    /* --------------------------------------------------------------------- */

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
                tmp.scheduleWithFixedDelay(this::maybePublish, config.getInitialDelayInSeconds(), config.getThrottleDelayInSeconds(), TimeUnit.SECONDS);
                executor = tmp;

                republishAuthorizedBondedRoles();
            }
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            handleUserProfileAdded(userProfile);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            handleUserProfileRemoved(userProfile);
        }
    }


    // The OracleNodeService has scheduler for republishing oracle data every 50 days and is also used to call our
    // republishAuthorizedBondedRoles. AuthorizedBondedRole has a 100 day TTL.
    public void republishAuthorizedBondedRoles() {
        Set<AuthorizedBondedRole> bannedAuthorizedBondedRoleSet = authorizedBondedRolesService.getBannedAuthorizedBondedRoleStream().collect(Collectors.toSet());

        String storageKey = AuthorizedBondedRole.class.getSimpleName();
        String myAuthorizedOracleNodeProfileId = myAuthorizedOracleNode.getProfileId();
        networkService.getDataService()
                .stream() // turns Optional<DataService> into Stream<DataService>
                .flatMap(dataService ->
                        dataService.getAuthenticatedPayloadStreamByStoreName(storageKey)
                                .map(AuthenticatedData::getDistributedData)
                                .filter(AuthorizedBondedRole.class::isInstance)
                                .map(AuthorizedBondedRole.class::cast)
                                .filter(authorizedBondedRole -> !bannedAuthorizedBondedRoleSet.contains(authorizedBondedRole))
                                .filter(authorizedBondedRole -> authorizedBondedRole.getAuthorizingOracleNode().isPresent())
                                .filter(authorizedBondedRole -> authorizedBondedRole.getAuthorizingOracleNode().get().getProfileId().equals(myAuthorizedOracleNodeProfileId))
                )
                .forEach(authorizedBondedRoleQueue::offer);
    }

    private void maybePublish() {
        //  Highest priority: AuthorizedBondedRole
        AuthorizedDistributedData data = authorizedBondedRoleQueue.poll();

        if (data == null) {
            // Next: AuthorizedProofOfBurnData (only from active profiles)
            data = this.pollIfActive(bsqBlockGrpcService.getAuthorizedProofOfBurnDataQueue());
        }

        if (data == null) {
            // Then: AuthorizedBondedReputationData (only from active profiles)
            data = pollIfActive(bsqBlockGrpcService.getAuthorizedBondedReputationDataQueue());
        }

        if (data == null) {
            // Finally: AuthorizedBurningmanListByBlock
            data = burningmanGrpcService.getAuthorizedBurningmanListByBlockQueue().poll();
        }

        if (data != null) {
            if (!config.isIgnorePublishAgeCheck() && data instanceof PublishDateAware publishDateAware) {
                long publishAge = System.currentTimeMillis() - publishDateAware.getPublishDate();
                if (publishAge > data.getMetaData().getTtl() / 2) {
                    publishAuthorizedData(data);
                }
            } else {
                publishAuthorizedData(data);
            }
        }
    }

    private <T extends AuthorizedDistributedData> T pollIfActive(BlockingQueue<T> queue) {
        T data = queue.poll();
        if (data == null) {
            return null;
        }
        if (userProfileInactive(data)) {
            // Reinsert inactive data to the end (reordering intentionally)
            queue.offer(data);
            return null;
        }
        return data;
    }

    private boolean userProfileInactive(AuthorizedDistributedData data) {
        if (data instanceof AuthorizedProofOfBurnData authorizedProofOfBurnData) {
            return !userProfileProofOfBurnHashes.contains(new ByteArray(authorizedProofOfBurnData.getHash()));
        } else if (data instanceof AuthorizedBondedReputationData authorizedBondedReputationData) {
            return !userProfileBondedReputationHashes.contains(new ByteArray(authorizedBondedReputationData.getHash()));
        } else {
            // Unexpected as we get called only for AuthorizedProofOfBurnData and AuthorizedBondedReputationData
            return false;
        }
    }

    private void publishAuthorizedData(AuthorizedDistributedData data) {
        if (keyPair == null) {
            Identity identity = identityService.getOrCreateDefaultIdentity();
            keyPair = identity.getNetworkIdWithKeyPair().getKeyPair();
        }

        networkService.publishAuthorizedData(data,
                keyPair,
                authorizedPrivateKey,
                authorizedPublicKey);
    }

    private void handleUserProfileAdded(UserProfile userProfile) {
        userProfileProofOfBurnHashes.add(userProfile.getProofOfBurnKey());
        userProfileBondedReputationHashes.add(userProfile.getBondedReputationKey());
    }

    private void handleUserProfileRemoved(UserProfile userProfile) {
        userProfileProofOfBurnHashes.remove(userProfile.getProofOfBurnKey());
        userProfileBondedReputationHashes.remove(userProfile.getProofOfBurnKey());
    }
}