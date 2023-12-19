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

package bisq.network.p2p.node;

import bisq.network.identity.NetworkId;
import bisq.network.p2p.BaseNetworkTest;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peergroup.BanList;
import bisq.network.p2p.services.peergroup.keepalive.Ping;
import bisq.network.p2p.services.peergroup.keepalive.Pong;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import bisq.security.pow.HashCashService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@Slf4j
public abstract class BaseNodesByIdTest extends BaseNetworkTest {
    protected int numNodes;

    void test_messageRoundTrip(Node.Config nodeConfig) throws InterruptedException {
        BanList banList = new BanList();
        TransportService transportService = TransportService.create(nodeConfig.getTransportType(), nodeConfig.getTransportConfig());
        PersistenceService persistenceService = new PersistenceService("");
        KeyBundleService keyBundleService = new KeyBundleService(persistenceService, mock(KeyBundleService.Config.class));

        NodesById nodesById = new NodesById(banList, nodeConfig, keyBundleService, transportService, new NetworkLoadService(), new AuthorizationService(new HashCashService()));
        long ts = System.currentTimeMillis();
        numNodes = 5;
        int numRepeats = 1;
        for (int i = 0; i < numRepeats; i++) {
            doMessageRoundTrip(numNodes, nodesById, transportService);
        }
        log.error("MessageRoundTrip for {} nodes repeated {} times took {} ms", numNodes, numRepeats, System.currentTimeMillis() - ts);
    }

    private void doMessageRoundTrip(int numNodes, NodesById nodesById, TransportService transportService) throws InterruptedException {
        long ts = System.currentTimeMillis();
        CountDownLatch initializeServerLatch = new CountDownLatch(numNodes);
        CountDownLatch sendPongLatch = new CountDownLatch(numNodes);
        CountDownLatch receivedPongLatch = new CountDownLatch(numNodes);
        transportService.initialize();
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "node_" + i;
            int finalI = i;
            int serverPort = 3000 + i;
            //nodesById.getInitializedNode(nodeId, serverPort);
            initializeServerLatch.countDown();
            nodesById.addNodeListener(new Node.Listener() {
                @Override
                public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
                    log.info("Received " + envelopePayloadMessage.toString());
                    if (envelopePayloadMessage instanceof Ping) {
                        Pong pong = new Pong(((Ping) envelopePayloadMessage).getNonce());
                        log.info("Send pong " + pong);
                        //nodesById.send(nodeId, pong, connection);
                        sendPongLatch.countDown();
                    } else if (envelopePayloadMessage instanceof Pong) {
                        receivedPongLatch.countDown();
                    }
                }

                @Override
                public void onConnection(Connection connection) {
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                }
            });
        }
        log.error("init started {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        assertTrue(initializeServerLatch.await(getTimeout(), TimeUnit.SECONDS));
        log.error("init completed after {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        CountDownLatch sendPingLatch = new CountDownLatch(numNodes);
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "node_" + i;
            int receiverIndex = (i + 1) % numNodes;
            String receiverNodeId = "node_" + receiverIndex;
            //Address receiverNodeAddress = nodesById.findMyAddress(receiverNodeId).orElseThrow();
            Ping ping = new Ping(1);
            log.info("Send ping " + ping);
            //nodesById.send(nodeId, ping, receiverNodeAddress);
            log.info("Send ping completed " + ping);
            sendPingLatch.countDown();
        }
        log.error("Send ping took {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        assertTrue(sendPingLatch.await(getTimeout(), TimeUnit.SECONDS));
        log.error("Send ping completed after {} ms", System.currentTimeMillis() - ts);


        ts = System.currentTimeMillis();
        assertTrue(sendPongLatch.await(getTimeout(), TimeUnit.SECONDS));
        log.error("Send pong completed after {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        assertTrue(receivedPongLatch.await(getTimeout(), TimeUnit.SECONDS));
        log.error("Receive pong completed after {} ms", System.currentTimeMillis() - ts);


        ts = System.currentTimeMillis();
        nodesById.shutdown().join();
        log.error("shutdown took {} ms", System.currentTimeMillis() - ts);
    }

    void test_initializeServer(Node.Config nodeConfig) {
        BanList banList = new BanList();
        TransportService transportService = TransportService.create(nodeConfig.getTransportType(), nodeConfig.getTransportConfig());
        PersistenceService persistenceService = new PersistenceService("");
        KeyBundleService keyBundleService = new KeyBundleService(persistenceService, mock(KeyBundleService.Config.class));
        NodesById nodesById = new NodesById(banList, nodeConfig, keyBundleService, transportService, new NetworkLoadService(), new AuthorizationService(new HashCashService()));
        initializeServers(2, nodesById);
        nodesById.shutdown().join();
    }

    private void initializeServers(int numNodes, NodesById nodesById) {
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "node_" + i;
            int serverPort = 3000 + i;
            //nodesById.getInitializedNode(nodeId, serverPort);
        }
        nodesById.shutdown().join();
        assertTrue(true);
    }
}