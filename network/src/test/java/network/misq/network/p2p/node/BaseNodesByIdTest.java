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

package network.misq.network.p2p.node;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.BaseNetworkTest;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.services.peergroup.BanList;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public abstract class BaseNodesByIdTest extends BaseNetworkTest {
    protected int numNodes;

    void test_messageRoundTrip(Node.Config config) throws InterruptedException {
        BanList banList = new BanList();
        NodesById nodesById = new NodesById(banList, config);
        long ts = System.currentTimeMillis();
        numNodes = 5;
        int numRepeats = 1;
        for (int i = 0; i < numRepeats; i++) {
            log.error("Iteration {}", i);
            doMessageRoundTrip(numNodes, nodesById);
        }
        log.error("MessageRoundTrip for {} nodes repeated {} times took {} ms", numNodes, numRepeats, System.currentTimeMillis() - ts);
    }

    private void doMessageRoundTrip(int numNodes, NodesById nodesById) throws InterruptedException {
        long ts = System.currentTimeMillis();
        CountDownLatch initializeServerLatch = new CountDownLatch(numNodes);
        CountDownLatch sendPongLatch = new CountDownLatch(numNodes);
        CountDownLatch receivedPongLatch = new CountDownLatch(numNodes);
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "node_" + i;
            int finalI = i;
            int serverPort = 1000 + i;
            nodesById.initializeServer(nodeId, serverPort);
            initializeServerLatch.countDown();
            nodesById.addNodeListener(nodeId, (message, connection, id) -> {
                log.info("Received " + message.toString());
                if (message instanceof ClearNetNodesByIdIntegrationTest.Ping) {
                    ClearNetNodesByIdIntegrationTest.Pong pong = new ClearNetNodesByIdIntegrationTest.Pong("Pong from " + finalI + " to " + connection.getPeerAddress().getPort());
                    log.info("Send pong " + pong);
                    nodesById.send(nodeId, pong, connection);
                    sendPongLatch.countDown();
                } else if (message instanceof ClearNetNodesByIdIntegrationTest.Pong) {
                    receivedPongLatch.countDown();
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
            Address receiverNodeAddress = nodesById.findMyAddress(receiverNodeId).get();
            ClearNetNodesByIdIntegrationTest.Ping ping = new ClearNetNodesByIdIntegrationTest.Ping("Ping from " + nodesById.findMyAddress(nodeId) + " to " + receiverNodeAddress);
            log.info("Send ping " + ping);
            nodesById.send(nodeId, ping, receiverNodeAddress);
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

    void test_initializeServer(Node.Config nodeConfig) throws InterruptedException {
        BanList banList = new BanList();
        NodesById nodesById = new NodesById(banList, nodeConfig);
        initializeServers(2, nodesById);
        nodesById.shutdown().join();
    }

    private void initializeServers(int numNodes, NodesById nodesById) {
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "node_" + i;
            int serverPort = 1000 + i;
            nodesById.initializeServer(nodeId, serverPort);
        }
        nodesById.shutdown().join();
        assertTrue(true);
    }

    @ToString
    public static class Ping implements Message {
        public final String msg;

        public Ping(String msg) {
            this.msg = msg;
        }
    }

    @ToString
    public static class Pong implements Message {
        public final String msg;

        public Pong(String msg) {
            this.msg = msg;
        }
    }
}