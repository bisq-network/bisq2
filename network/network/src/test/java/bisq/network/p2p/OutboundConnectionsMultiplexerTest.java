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

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.p2p.node.*;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWorkService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

@Slf4j
public class OutboundConnectionsMultiplexerTest {

    private final Path tmpDir = FileUtils.createTempDir();

    public OutboundConnectionsMultiplexerTest() throws IOException {
    }

    @Test
    void startServerAndConnectAfterItsReady() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ArrayList<Transport.Type> supportedTransportTypes = new ArrayList<>();
        supportedTransportTypes.add(Transport.Type.CLEAR);

        Address address = Address.localHost(NetworkUtils.findFreeSystemPort());
        Capability serverCapability = new Capability(address, supportedTransportTypes);
        ServerChannel serverChannel = new ServerChannel(
                serverCapability,
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
                Address outboundAddress = Address.localHost(NetworkUtils.findFreeSystemPort());
                Capability outboundCapability = new Capability(outboundAddress, supportedTransportTypes);
                Selector selector = SelectorProvider.provider().openSelector();

                var outboundConnectionManager = new OutboundConnectionManager(
                        authorizationService,
                        mock(BanList.class),
                        Load.INITIAL_LOAD,
                        outboundCapability,
                        selector
                );
                var connectionMultiplexer = new OutboundConnectionMultiplexer(outboundConnectionManager);
                connectionMultiplexer.start();

                CompletableFuture<OutboundConnectionChannel> connection = connectionMultiplexer.getConnection(serverCapability);
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

    private AuthorizationService createAuthorizationService() throws IOException {
        Path persistenceBaseDir = Files.createTempDirectory(tmpDir, "persistence");
        String baseDir = persistenceBaseDir.toAbsolutePath().toString();

        PersistenceService persistenceService = new PersistenceService(baseDir);
        SecurityService securityService = new SecurityService(persistenceService);
        securityService.initialize();

        ProofOfWorkService proofOfWorkService = securityService.getProofOfWorkService();
        return new AuthorizationService(proofOfWorkService);
    }
}
