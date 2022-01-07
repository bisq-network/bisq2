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

import lombok.extern.slf4j.Slf4j;

//TODO Test commented out as network layer has changed. not sure yet the test will be reactivate/rewritten or delete later.
// leave it for now...
@Slf4j
public class I2pIntegrationTest extends BaseTest {
   /* @Override
    protected int getTimeout() {
        return 320;
    }

    @Override
    protected Set<NetworkType> getMySupportedNetworks() {
        return Sets.newHashSet(NetworkType.I2P);
    }

    @Override
    protected NetworkServiceConfig getNetworkConfig(Config.Role role) {
        return Config.getI2pNetworkConfig(role);
    }

    @Override
    protected Address getPeerAddress(Config.Role role) {
        P2pServiceNode networkNode;
        String persisted = "undefined";
        switch (role) {
            case Alice:
                networkNode = this.alice;
                persisted = "TAxJNls3m3moGJU4hS7KxewxVLs1emS2N6WmYegpJk9mX7VSYwY-1j2fvnGBJptM~3oZIJ-HChfmb1GtxOL14zkV~exl6ZRXpZnbD4i6oP2TZg5eD8H5vOu0q-NBC2xjxGKPXOGGUmDi1bzqzcumOYSzJI57OVlOLNo8bkGfiLJuFhVWQk-PquQvFmJcp5fAYfgNJCL0eE9YpCSXc2SepopD02rr-WLmkl3qKH1xzS8jlMSN0-PS35TzU1YSzSzFmjUP7llmNCAINu5z9fZiUMObMhpbrGJCR2-k~0g-ujVnZ-0PuBIncaOAYWPloCeeKt7NMo406Wtjxjmz4VARUcGbqoe11lqyzMKD33LKPVIa56XFJRjg~d7j3dmTo-kR0aarLWZlDKf6AjTisLGNPEmvSzY4Z91dHVBqNWy6PNtXpqeETss3YdHUnGKZlFOdxDrRJpTuk56itDuvwwEpWWP1iZhBEap4IPJaHCIGsEObf0TW4lVwUk2BwyI9Ue7SBQAEAAcAAA==";
                break;
            case Bob:
                networkNode = this.bob;
                persisted = "QXOufFlBKxOJOpYqo5mXqOotCaUwkrfUphjX4Qz9IOmKjcmIFdNKBBTi5fmdJkKzsQozWu~fYA~5aoy42YCioEsa42YL2ewPbg6xgkwMIb~aPOTXnfJh1kSDItQBrVSv9rX3uYSvj-j-tT48cHkO4aBcCNm61-u8goBd8ANOqVaS-a1v5Jde~8dY-V-D3cDqFgHxP8D6YamN3shEJQP4BhEglfbdXf~hneja77uGFu20h-B5yBIOqelu6~wEIG2t85q~lyRfL~grC1y524HeWV4Pe8xtbXatcBsF5DSKGUjZXy0vXHrbHiasGTSJS9qpVMS9ue6NZ3dyAfIVv2qh0CltLTaqj~83YvuOGuOpOfU3ZTSP2yhHJo7kUe7rjiA7DGZ3D6-I0q1mbzvlnSoe4ftlcXqnzWglem-8zuxdlW-e38~0ac-hAxde8-4HhUHzE32~8xp~-r4CWNxQ4qWOexlXJH9s8pcdCelSnvQ7J8H2CDIEQ5deXkYBp6LTIVvyBQAEAAcAAA==";
                break;
            case Carol:
            default:
                networkNode = this.carol;
                break;
        }
        Optional<Address> optionalAddress = networkNode.findMyAddress();
        if (optionalAddress.isPresent()) {
            return optionalAddress.get();
        } else {
            return new Address(persisted, -1);
        }
    }

    // @Test
    public void testInitializeServer() throws InterruptedException {
        try {
            super.testInitializeServer(1);
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }

    @Test
    public void testStartOfMultipleIds() throws InterruptedException {
        NetworkType networkType = NetworkType.I2P;
        Set<NetworkType> mySupportedNetworks = getMySupportedNetworks();
        startOfMultipleIds(networkType, mySupportedNetworks);
    }

    // @Test
    public void testSendMsgWithMultipleIds() throws InterruptedException, GeneralSecurityException {
        NetworkType networkType = NetworkType.I2P;
        Set<NetworkType> mySupportedNetworks = getMySupportedNetworks();
        sendMsgWithMultipleIds(networkType, mySupportedNetworks);
    }

    //  @Test
    public void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        try {
            super.testConfidentialSend();
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }

    // First msg 560 ms, others 15-380 ms
    // First msg 524 ms, others 20-380 ms, average 150ms
    // Total: 61077 ms / 17012 ms / 17487 ms / 5825 ms
    //  @Test
    public void repeatedSend() throws InterruptedException, GeneralSecurityException {
        long ts = System.currentTimeMillis();
        testInitializeServer(2);

        int numMsg = 2;
        Map<Integer, Long> tsMap = new HashMap<>();
        CountDownLatch receivedLatch = new CountDownLatch(numMsg);
        bob.addMessageListener((proto, connection) -> {
            assertTrue(proto instanceof MockMessage);
            // assertEquals(((MockMessage) proto).getMsg(), msg);
            int key = Integer.parseInt(((MockMessage) proto).getMsg());
            if (tsMap.containsKey(key)) {
                log.info("Sending msg {} took {}", key, System.currentTimeMillis() - tsMap.get(key));
            }
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(numMsg);

        Address peerAddress = getPeerAddress(Config.Role.Bob);
        send(sentLatch, peerAddress, new AtomicInteger(0), tsMap);
        boolean sent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(sent);

        boolean received = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(received);

        alice.shutdown();
        bob.shutdown();
        log.info("Took {} ms", System.currentTimeMillis() - ts);
    }

    private void send(CountDownLatch sentLatch, Address peerAddress, AtomicInteger i, Map<Integer, Long> tsMap) throws GeneralSecurityException {
        tsMap.put(i.get(), System.currentTimeMillis());
        log.info("Send msg {}", i.get());
        MultiAddress multiAddress = new MultiAddress(peerAddress, Config.keyPairBob1.getPublic(), "default");
        //todo
        String connectionId = "nodeId";
        alice.confidentialSend(new MockMessage(String.valueOf(i.get())), multiAddress, Config.keyPairAlice1, connectionId)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                        if (sentLatch.getCount() > 0) {
                            try {
                                i.incrementAndGet();
                                send(sentLatch, peerAddress, i, tsMap);
                            } catch (GeneralSecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        fail();
                    }
                });
    }*/
}
