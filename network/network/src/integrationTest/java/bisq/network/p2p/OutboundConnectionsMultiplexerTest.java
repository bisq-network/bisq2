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

package bisq.network.p2p;

import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileUtils;
import bisq.common.network.DefaultClearNetLocalAddressFacade;
import bisq.common.util.NetworkUtils;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.p2p.node.*;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.BanList;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Slf4j
public class OutboundConnectionsMultiplexerTest {

    private final Path tmpDir = FileUtils.createTempDir();

    public OutboundConnectionsMultiplexerTest() throws IOException {
    }

    @Test
    void startServerAndConnectAfterItsReady() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ArrayList<TransportType> supportedTransportTypes = new ArrayList<>();
        supportedTransportTypes.add(TransportType.CLEAR);

        Address serverAddress = DefaultClearNetLocalAddressFacade.toLocalHostAddress(NetworkUtils.findFreeSystemPort());
        Capability serverCapability = createCapability(serverAddress, supportedTransportTypes);
        ServerChannel serverChannel = new ServerChannel(
                serverCapability,
                new NetworkLoad(),
                mock(BanList.class),
                createAuthorizationService(),
                mock(Node.class),
                ServerSocketChannel.open()
        );

        var countDownLatch = new CountDownLatch(1);
        serverChannel.setOnServerReadyListener(Optional.of(countDownLatch::countDown));

        serverChannel.start();

        var futureTask = new FutureTask<>(() -> {
            try {
                boolean serverStartedSuccessfully = countDownLatch.await(10, TimeUnit.SECONDS);
                if (!serverStartedSuccessfully) {
                    fail("Server start failed.");
                }

                AuthorizationService authorizationService = createAuthorizationService();
                Address outboundAddress = DefaultClearNetLocalAddressFacade.toLocalHostAddress(NetworkUtils.findFreeSystemPort());
                Capability outboundCapability = createCapability(outboundAddress, supportedTransportTypes);
                Selector selector = SelectorProvider.provider().openSelector();

                var outboundConnectionManager = new OutboundConnectionManager(
                        authorizationService,
                        mock(BanList.class),
                        new NetworkLoad(),
                        outboundCapability,
                        mock(Node.class),
                        selector
                );
                var connectionMultiplexer = new OutboundConnectionMultiplexer(outboundConnectionManager);
                connectionMultiplexer.start();

                CompletableFuture<OutboundConnectionChannel> connection = connectionMultiplexer.getConnection(serverAddress);
                return connection.get(1, TimeUnit.MINUTES);


            } catch (Exception e) {
                log.error("ERROR: ", e);
            }

            return null;
        });
        var thread = new Thread(futureTask);
        thread.start();

        OutboundConnectionChannel outboundConnectionChannel = futureTask.get(30, TimeUnit.SECONDS);
        assertThat(outboundConnectionChannel).isNotNull();
    }

  /*  private AuthorizationService createAuthorizationService() throws IOException {
        Path persistenceBaseDir = Files.createTempDirectory(tmpDir, "persistence");
        String baseDir = persistenceBaseDir.toAbsolutePath().toString();

        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService, mock(SecurityService.Config.class));
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
    }*/

    private AuthorizationService createAuthorizationService() {
        return new AuthorizationService(new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH));
    }

    private static Capability createCapability(Address address, List<TransportType> supportedTransportTypes) {
        return new Capability(Capability.VERSION, address, supportedTransportTypes, new ArrayList<>(), ApplicationVersion.getVersion().getVersionAsString());
    }
}
