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

package network.misq.network.p2p;

import lombok.extern.slf4j.Slf4j;

//TODO Test commented out as network layer has changed. not sure yet the test will be reactivate/rewritten or delete later.
// leave it for now...
@Slf4j
public abstract class BaseTest {

    /*protected final Storage storage = new Storage("");
    protected P2pServiceNode alice, bob, carol;

    protected abstract int getTimeout();

    protected abstract Set<NetworkType> getMySupportedNetworks();

    protected abstract NetworkServiceConfig getNetworkConfig(Config.Role role);

    protected abstract Address getPeerAddress(Config.Role role);

    protected void testBootstrapSolo(int count) throws InterruptedException {
        alice = new P2pServiceNode(getNetworkConfig(Config.Role.Alice), storage, Config.aliceKeyPairSupplier1);
        CountDownLatch bootstrappedLatch = new CountDownLatch(count);
        alice.initializeOverlay().whenComplete((success, t) -> {
            if (success && t == null) {
                bootstrappedLatch.countDown();
            }
        });

        boolean bootstrapped = bootstrappedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(bootstrapped);
        alice.shutdown();
    }

    protected void testInitializeServer(int serversReadyLatchCount) throws InterruptedException {
        testInitializeServer(serversReadyLatchCount,
                getNetworkConfig(Config.Role.Alice), getNetworkConfig(Config.Role.Bob));
    }

    protected void testInitializeServer(int serversReadyLatchCount,
                                        NetworkServiceConfig networkServiceConfigAlice,
                                        NetworkServiceConfig networkServiceConfigBob) throws InterruptedException {
        alice = new P2pServiceNode(networkServiceConfigAlice, storage, Config.aliceKeyPairSupplier1);
        bob = new P2pServiceNode(networkServiceConfigBob, storage, Config.bobKeyPairSupplier1);
        CountDownLatch serversReadyLatch = new CountDownLatch(serversReadyLatchCount);
        alice.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    protected void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        testInitializeServer(2);
        String msg = "hello";
        CountDownLatch receivedLatch = new CountDownLatch(1);
        bob.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            assertEquals(((MockMessage) message).getMsg(), msg);
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(1);

        Address peerAddress = getPeerAddress(Config.Role.Bob);
        MultiAddress multiAddress = new MultiAddress(peerAddress, Config.keyPairBob1.getPublic(), "default");
        //todo
        String connectionId = "aliceConnectionId";
        alice.confidentialSend(new MockMessage(msg), multiAddress, Config.keyPairAlice1, connectionId)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        boolean sent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(sent);

        boolean received = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(received);
    }

    protected void startOfMultipleIds(NetworkType networkType, Set<NetworkType> networkTypes) throws InterruptedException {
        String baseDirNameAlice = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Alice";
        NodeId nodeIdAlice1 = new NodeId("id_alice_1", 1111, networkTypes);
        alice = new P2pServiceNode(new NetworkServiceConfig(baseDirNameAlice, nodeIdAlice1, networkType), storage, Config.aliceKeyPairSupplier1);

        NodeId nodeIdAlice2 = new NodeId("id_alice_2", 1112, networkTypes);
        P2pServiceNode alice2 = new P2pServiceNode(new NetworkServiceConfig(baseDirNameAlice, nodeIdAlice2, networkType), storage, Config.aliceKeyPairSupplier1);

        String baseDirNameBob = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Bob";
        NodeId nodeIdBob1 = new NodeId("id_bob_1", 2222, networkTypes);
        bob = new P2pServiceNode(new NetworkServiceConfig(baseDirNameBob, nodeIdBob1, networkType), storage, Config.bobKeyPairSupplier1);

        CountDownLatch serversReadyLatch = new CountDownLatch(3);
        alice.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();

            // As alice1 and alice2 use same parent dir it could fail at first run
            // as the mkdir call would be called in concurrent mode and exists() could return false.
            alice2.initializeServer().whenComplete((result1, throwable1) -> {
                assertNotNull(result1);
                serversReadyLatch.countDown();
            });
        });

        bob.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    protected void sendMsgWithMultipleIds(NetworkType networkType,
                                          Set<NetworkType> networkTypes)
            throws InterruptedException, GeneralSecurityException {
        String baseDirNameAlice = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Alice";
        NodeId nodeIdAlice1 = new NodeId("id_alice_1", 1111, networkTypes);
        P2pServiceNode alice1 = new P2pServiceNode(new NetworkServiceConfig(baseDirNameAlice, nodeIdAlice1, networkType), storage, Config.aliceKeyPairSupplier1);

        NodeId nodeIdAlice2 = new NodeId("id_alice_2", 1112, networkTypes);
        P2pServiceNode alice2 = new P2pServiceNode(new NetworkServiceConfig(baseDirNameAlice, nodeIdAlice2, networkType), storage, Config.aliceKeyPairSupplier2);

        String baseDirNameBob = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Bob";
        NodeId nodeIdBob1 = new NodeId("id_bob_1", 2222, networkTypes);
        P2pServiceNode bob1 = new P2pServiceNode(new NetworkServiceConfig(baseDirNameBob, nodeIdBob1, networkType), storage, Config.bobKeyPairSupplier1);

        NodeId nodeIdBob2 = new NodeId("id_bob_2", 2223, networkTypes);
        P2pServiceNode bob2 = new P2pServiceNode(new NetworkServiceConfig(baseDirNameAlice, nodeIdBob2, networkType), storage, Config.bobKeyPairSupplier2);

        CountDownLatch serversReadyLatch = new CountDownLatch(4);
        alice1.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        alice2.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob1.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob2.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
        String alice1ToBob1Msg = "alice1ToBob1Msg";
        String alice1ToBob2Msg = "alice1ToBob2Msg";
        String alice2ToBob1Msg = "alice2ToBob1Msg";
        String alice2ToBob2Msg = "alice2ToBob2Msg";
        String bob1ToAlice1Msg = "bob1ToAlice1Msg";
        String bob1ToAlice2Msg = "bob1ToAlice2Msg";
        String bob2ToAlice1Msg = "bob2ToAlice1Msg";
        String bob2ToAlice2Msg = "bob2ToAlice2Msg";

        Address alive1Address = alice1.findMyAddress().get();
        Address alive2Address = alice2.findMyAddress().get();
        Address bob1Address = bob1.findMyAddress().get();
        Address bob2Address = bob2.findMyAddress().get();

        CountDownLatch receivedLatch = new CountDownLatch(8);
        alice1.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.info("alice1 {} {}", message, connection);
            if (connection.getPeerAddress().equals(bob1Address)) {
                assertEquals(((MockMessage) message).getMsg(), bob1ToAlice1Msg);
            } else if (connection.getPeerAddress().equals(bob2Address)) {
                assertEquals(((MockMessage) message).getMsg(), bob2ToAlice1Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });
        alice2.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.info("alice2 {} {}", message, connection);
            if (connection.getPeerAddress().equals(bob1Address)) {
                assertEquals(((MockMessage) message).getMsg(), bob1ToAlice2Msg);
            } else if (connection.getPeerAddress().equals(bob2Address)) {
                assertEquals(((MockMessage) message).getMsg(), bob2ToAlice2Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });


        bob1.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.info("bob1 {} {}", message, connection);
            String expected = ((MockMessage) message).getMsg();
            Address connectionPeerAddress = connection.getPeerAddress();
            if (connectionPeerAddress.equals(alive1Address)) {
                assertEquals(expected, alice1ToBob1Msg);
            } else if (connectionPeerAddress.equals(alive2Address)) {
                assertEquals(expected, alice2ToBob1Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });
        bob2.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.info("bob2 {} {}", message, connection);
            if (connection.getPeerAddress().equals(alive1Address)) {
                assertEquals(((MockMessage) message).getMsg(), alice1ToBob2Msg);
            } else if (connection.getPeerAddress().equals(alive2Address)) {
                assertEquals(((MockMessage) message).getMsg(), alice2ToBob2Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });

        CountDownLatch sentLatch = new CountDownLatch(8);
        MultiAddress bob1MultiAddress = new MultiAddress(bob1Address, Config.keyPairBob1.getPublic(), "default");
        alice1.confidentialSend(new MockMessage(alice1ToBob1Msg), bob1MultiAddress, Config.keyPairAlice1, nodeIdAlice1.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        MultiAddress bob2MultiAddress = new MultiAddress(bob2Address, Config.keyPairBob2.getPublic(), "default");
        alice1.confidentialSend(new MockMessage(alice1ToBob2Msg), bob2MultiAddress, Config.keyPairAlice1, nodeIdAlice1.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        alice2.confidentialSend(new MockMessage(alice2ToBob1Msg), bob1MultiAddress, Config.keyPairAlice2, nodeIdAlice2.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        alice2.confidentialSend(new MockMessage(alice2ToBob2Msg), bob2MultiAddress, Config.keyPairAlice2, nodeIdAlice2.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        MultiAddress alive1MultiAddress = new MultiAddress(alive1Address, Config.keyPairAlice1.getPublic(), "default");
        bob1.confidentialSend(new MockMessage(bob1ToAlice1Msg), alive1MultiAddress, Config.keyPairBob1, nodeIdBob1.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        MultiAddress alive2MultiAddress = new MultiAddress(alive2Address, Config.keyPairAlice2.getPublic(), "default");
        bob1.confidentialSend(new MockMessage(bob1ToAlice2Msg), alive2MultiAddress, Config.keyPairBob1, nodeIdBob1.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob2.confidentialSend(new MockMessage(bob2ToAlice1Msg), alive1MultiAddress, Config.keyPairBob2, nodeIdBob2.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob2.confidentialSend(new MockMessage(bob2ToAlice2Msg), alive2MultiAddress, Config.keyPairBob2,  nodeIdBob2.getConnectionId())
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        boolean allSent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(allSent);
        boolean allReceived = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(allReceived);

        alice1.shutdown();
        alice2.shutdown();
        bob1.shutdown();
        bob2.shutdown();
    }*/
}
