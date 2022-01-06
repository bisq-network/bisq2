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

package bisq.network.p2p.services.peergroup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PeerExchangeBase {
   /* protected Node nodeSeed, node1, node2, node3;
    protected PeerExchangeManager peerExchangeManagerSeed, peerExchangeManager1, peerExchangeManager2, peerExchangeManager3;
    protected PeerGroup peerGroupSeed, peerGroupNode1, peerGroupNode2, peerGroupNode3;
    protected Storage storage = new Storage("");
    protected List<P2pServiceNode> nodes;
    protected final int numSeeNodesAtBoostrap = 1;
    protected final int numPersistedPeersAtBoostrap = 8;
    protected int numReportedPeersAtBoostrap = 20;
    protected int minNumReportedPeers = 20;
    protected final int minNumConnectedPeers = 30;
    protected final int maxNumConnectedPeers = 40;
    protected final int repeatPeerExchangeDelay = 200;
    protected Map<Integer, Triple<PeerExchangeManager, PeerGroup, Node>> tuples;


    // Seed node only, so num connection and num reported will be 0
    protected void bootstrapSeedNode() throws InterruptedException {
        NetworkServiceConfig networkServiceConfig = getNetworkConfig(1000);
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(networkServiceConfig).whenComplete((tuple, e) -> {
            peerExchangeManagerSeed = tuple.first();
            peerGroupSeed = tuple.second();
            nodeSeed = tuple.third();
            log.info("bootstrap seed");
            peerExchangeManagerSeed.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("seed bootstrapped");
                            latch.countDown();
                            assertEquals(0, peerGroupSeed.getConnections().size());
                            assertEquals(0, peerGroupSeed.getReportedPeers().size());
                        }
                    });
        });
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);
    }

    *//*
     * n1 []-> s []-> n1
     *//*
    protected void bootstrapNode1() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(getNetworkConfig(2001)).whenComplete((tuple, e) -> {
            peerExchangeManager1 = tuple.first();
            peerGroupNode1 = tuple.second();
            node1 = tuple.third();
            log.info("bootstrap node1");
            // n1->s
            peerExchangeManager1.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("node1 bootstrapped");

                            latch.countDown();
                            assertEquals(1, peerGroupNode1.getConnections().size());
                            assertEquals(0, peerGroupNode1.getReportedPeers().size());

                            assertEquals(1, peerGroupSeed.getConnections().size());
                            assertEquals(0, peerGroupSeed.getReportedPeers().size());
                        }
                    });
        });

        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);
    }

    *//*
     * n1 []-> s []-> n1
     * n2 []-> s [n1]-> n2
     * n2 []-> n1 []-> n2
     *//*
    protected void bootstrapNode2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(getNetworkConfig(2002))
                .whenComplete((tuple, e) -> {
                    peerExchangeManager2 = tuple.first();
                    peerGroupNode2 = tuple.second();
                    node2 = tuple.third();
                    log.info("bootstrap node2");
                    // n2->s
                    peerExchangeManager2.bootstrap()
                            .whenComplete((success, t) -> {
                                if (success && t == null) {
                                    log.info("node2 bootstrapped");
                                    latch.countDown();
                                    assertEquals(1, peerGroupNode2.getConnections().size());
                                    assertEquals(1, peerGroupNode2.getReportedPeers().size());

                                    assertEquals(1, peerGroupNode1.getConnections().size());
                                    assertEquals(0, peerGroupNode1.getReportedPeers().size());

                                    assertEquals(2, peerGroupSeed.getConnections().size());
                                    assertEquals(0, peerGroupSeed.getReportedPeers().size());
                                }
                            });
                });
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        Set<Integer> reportedPeerPorts = peerGroupNode2.getReportedPeers().stream()
                .map(peer -> peer.getAddress().getPort()).collect(Collectors.toSet());
        assertTrue(reportedPeerPorts.contains(2001));

        // n2->n1
        peerExchangeManager2.bootstrap()
                .whenComplete((success, t) -> {
                    if (success && t == null) {
                        log.info("node2 bootstrapped again");
                        latch.countDown();
                        assertEquals(2, peerGroupNode2.getConnections().size());
                        assertEquals(1, peerGroupNode2.getReportedPeers().size());

                        assertEquals(2, peerGroupNode1.getConnections().size());
                        assertEquals(0, peerGroupNode1.getReportedPeers().size());

                        assertEquals(2, peerGroupSeed.getConnections().size());
                        assertEquals(0, peerGroupSeed.getReportedPeers().size());
                    }
                });
    }

    *//*
     * n1 []-> s []-> n1
     * n2 []-> s [n1]-> n2
     * n2 []-> n1 []-> n2
     *
     * n3 []-> s [n1, n2]-> n3
     * n3 [n2]-> n1 [n2]-> n3
     * n3 [n1]-> n2 [n1]-> n3
     *//*
    protected void bootstrapNode3() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(getNetworkConfig(2003)).whenComplete((tuple, e) -> {
            peerExchangeManager3 = tuple.first();
            peerGroupNode3 = tuple.second();
            node3 = tuple.third();
            log.info("bootstrap node3");
            // n3 -> s
            peerExchangeManager3.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("node3 bootstrapped");
                            latch.countDown();
                            assertEquals(1, peerGroupNode3.getConnections().size());
                            assertEquals(2, peerGroupNode3.getReportedPeers().size());

                            assertEquals(2, peerGroupNode2.getConnections().size());
                            assertEquals(1, peerGroupNode2.getReportedPeers().size());

                            assertEquals(2, peerGroupNode1.getConnections().size());
                            assertEquals(0, peerGroupNode1.getReportedPeers().size());

                            assertEquals(3, peerGroupSeed.getConnections().size());
                            assertEquals(0, peerGroupSeed.getReportedPeers().size());
                        }
                    });
        });
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        Set<Integer> reportedPeerPorts = peerGroupNode3.getReportedPeers().stream()
                .map(peer -> peer.getAddress().getPort()).collect(Collectors.toSet());
        assertTrue(reportedPeerPorts.contains(2001));
        assertTrue(reportedPeerPorts.contains(2002));

        // n3-> n1, n3-> n2
        peerExchangeManager3.bootstrap()
                .whenComplete((success, t) -> {
                    if (success && t == null) {
                        log.info("node2 bootstrapped again");
                        latch.countDown();
                        assertEquals(2, peerGroupNode2.getConnections().size());
                        assertEquals(1, peerGroupNode2.getReportedPeers().size());

                        assertEquals(2, peerGroupNode1.getConnections().size());
                        assertEquals(0, peerGroupNode1.getReportedPeers().size());

                        assertEquals(2, peerGroupSeed.getConnections().size());
                        assertEquals(0, peerGroupSeed.getReportedPeers().size());
                    }
                });
    }

    protected void bootstrapNodes(int numNodes) throws InterruptedException {
        numReportedPeersAtBoostrap = 20;
        minNumReportedPeers = 30;
        tuples = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(numNodes);
        for (int i = 0; i < numNodes; i++) {
            int counter = i;
            getTuple(getNetworkConfig(2000 + counter)).whenComplete((tuple, e) -> {
                tuples.put(counter, tuple);
                PeerExchangeManager node = tuple.first();
                log.info("bootstrap node {}", counter);
                node.bootstrap()
                        .whenComplete((success, t) -> {
                            if (success && t == null) {
                                log.info("Bootstrap completed: node {}", counter);
                                latch.countDown();
                            }
                        });

                CountDownLatch latchRepeat = new CountDownLatch(tuples.size() - 1);
                for (int j = 0; j < tuples.size(); j++) {
                   *//* if (j == 0) {
                        continue;
                    }*//*
                    int finalJ = j;
                    tuples.get(j).first().bootstrap()
                            .whenComplete((success, t) -> {
                                if (success && t == null) {
                                    log.info("Repeated bootstrap completed: node {}", finalJ);
                                    latchRepeat.countDown();
                                }
                            });
                }
                try {
                    boolean repeatedBootstrapped = latchRepeat.await(5, TimeUnit.SECONDS);
                    assertTrue(repeatedBootstrapped);
                } catch (InterruptedException ignore) {
                }
            });

            // Make sequence predictable
            Thread.sleep(50);
        }
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        CountDownLatch seedRepeat = new CountDownLatch(1);
        peerExchangeManagerSeed.bootstrap().whenComplete((s, e) -> {
            seedRepeat.countDown();
        });
        boolean seedRepeatRepeatedBootstrapped = seedRepeat.await(5, TimeUnit.SECONDS);
        assertTrue(seedRepeatRepeatedBootstrapped);


        tuples.forEach((key, value) -> {
            PeerGroup peerGroup = value.second();
            log.info("node 200{}, numConnection={}, numReported={}", key, peerGroup.getConnections(), peerGroup.getReportedPeers().size());
        });
        Thread.sleep(100);

       *//* CountDownLatch latch2 = new CountDownLatch(1);
        tuples.get(1).first.bootstrap()
                .whenComplete((success, t) -> {
                    if (success && t == null) {
                        log.info("node2 bootstrapped again");
                        latch2.countDown();
                    }
                });
*//*

     *//*  for (int i = 0; i < numNodes; i++) {
            Tuple2<PeerExchangeManager, PeerGroup> tuple = tuples.get(i);
            PeerExchangeManager node = tuple.first();
            log.info("Repeated bootstrap node {}", i);
            int c = i;
            node.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("Repeated bootstrap completed: node {} ", c);
                            latch2.countDown();
                        }
                    });

            // Make sequence predictable
            Thread.sleep(50);
        }*//*
     *//*  boolean bootstrapped2 = latch2.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped2);*//*
        tuples.forEach((key, value) -> {
            PeerGroup peerGroup = value.second();
            log.info("Repeated: node 200{}, numConnection={}, numReported={}", key, peerGroup.getConnections().size(), peerGroup.getReportedPeers().size());
        });

    }

    protected CompletableFuture<Triple<PeerExchangeManager, PeerGroup, Node>> getTuple(NetworkServiceConfig networkServiceConfig) {
        NetworkProxyConfig networkProxyConfig = new NetworkProxyConfig(networkServiceConfig.getBaseDirPath());
        Node.Config nodeConfig = new Node.Config(networkServiceConfig.getSelectedNetworkType(),
                networkServiceConfig.getNodeId().getSupportedNetworkTypes(),
                new UnrestrictedAuthorizationService(),
                networkProxyConfig);

        NodeRepository nodeRepository = new NodeRepository(nodeConfig);
        Node defaultNode = nodeRepository.getOrCreateNode(Node.DEFAULT_NODE_ID);
        PeerConfig peerConfig = networkServiceConfig.getPeerConfig();
        PeerGroup peerGroup = new PeerGroup(defaultNode, peerConfig, networkServiceConfig.getNodeId().getServerPort());
        DefaultPeerExchangeStrategy peerExchangeStrategy = new DefaultPeerExchangeStrategy(peerGroup, peerConfig);
        return defaultNode.initializeServer(networkServiceConfig.getNodeId().getServerPort())
                .thenApply(e -> new Triple<>(new PeerExchangeManager(defaultNode, peerExchangeStrategy), peerGroup, defaultNode));
    }

    protected void shutDownSeed() {
        peerExchangeManagerSeed.shutdown();
    }

    protected void shutDownNode1() {
        peerExchangeManager1.shutdown();
    }

    protected void shutDownNode2() {
        peerExchangeManager2.shutdown();
    }

    protected void shutDownNode3() {
        peerExchangeManager3.shutdown();
    }

    protected void shutDownNodes() {
        nodes.forEach(P2pServiceNode::shutdown);
    }

    protected NetworkServiceConfig getNetworkConfig(int serverPort,
                                                    List<Address> seedNodes,
                                                    int repeatPeerExchangeDelay,
                                                    int minNumConnectedPeers,
                                                    int maxNumConnectedPeers) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/bisq_test_node_" + serverPort;
        PeerExchangeConfig peerExchangeConfig = new PeerExchangeConfig(numSeeNodesAtBoostrap,
                numPersistedPeersAtBoostrap,
                numReportedPeersAtBoostrap,
                repeatPeerExchangeDelay);
        PeerConfig peerConfig = new PeerConfig(peerExchangeConfig,
                seedNodes,
                minNumConnectedPeers,
                maxNumConnectedPeers,
                minNumReportedPeers);

        NodeId nodeId = new NodeId("default", serverPort, Sets.newHashSet(NetworkType.CLEAR));
        Set<Service> services = Set.of(Service.OVERLAY, Service.DATA, Service.CONFIDENTIAL, Service.RELAY);
        return new NetworkServiceConfig(baseDirName, nodeId, NetworkType.CLEAR, peerConfig, services);
    }

    protected NetworkServiceConfig getNetworkConfig(int serverPort) {
        List<Address> seedNodes = Arrays.asList(Address.localHost(1000));
        return getNetworkConfig(serverPort,
                seedNodes,
                repeatPeerExchangeDelay,
                minNumConnectedPeers,
                maxNumConnectedPeers);
    }*/
}
