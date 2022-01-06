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
public class ClearNetIntegrationTest extends BaseTest {
   /* @Override
    protected Set<NetworkType> getMySupportedNetworks() {
        return Sets.newHashSet(NetworkType.CLEAR);
    }

    @Override
    protected int getTimeout() {
        return 10;
    }

    @Override
    protected NetworkServiceConfig getNetworkConfig(Config.Role role) {
        return Config.getClearNetNetworkConfig(role);
    }

    @Override
    protected Address getPeerAddress(Config.Role role) {
        return Address.localHost(getNetworkConfig(role).getNodeId().getServerPort());
    }

    @Test
    public void testInitializeServer() throws InterruptedException {
        super.testInitializeServer(2);
        alice.shutdown();
        bob.shutdown();
    }

    @Test
    public void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        super.testConfidentialSend();
        alice.shutdown();
        bob.shutdown();
    }

    @Test
    public void testStartOfMultipleIds() throws InterruptedException {
        NetworkType networkType = NetworkType.CLEAR;
        Set<NetworkType> mySupportedNetworks = getMySupportedNetworks();
        startOfMultipleIds(networkType, mySupportedNetworks);
    }

    @Test
    public void testSendMsgWithMultipleIds() throws InterruptedException, GeneralSecurityException {
        NetworkType networkType = NetworkType.CLEAR;
        Set<NetworkType> mySupportedNetworks = getMySupportedNetworks();
        sendMsgWithMultipleIds(networkType, mySupportedNetworks);
    }*/

    /*
    @Test
    public void testStartOfMultipleIds() throws InterruptedException {
        NetworkType networkType = NetworkType.CLEAR;
        ArrayList<NetworkType> networkTypes = Lists.newArrayList(networkType);

        String baseDirNameAlice = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Alice";
        NetworkId networkIdAlice1 = new NetworkId(baseDirNameAlice, "id_alice_1", 1111, networkTypes);
        alice = new P2pNode(new NetworkConfig(networkIdAlice1, networkType), getMySupportedNetworks(), storage, Config.aliceKeyRepository1);

        NetworkId networkIdAlice2 = new NetworkId(baseDirNameAlice, "id_alice_2", 1112, networkTypes);
        P2pNode alice2 = new P2pNode(new NetworkConfig(networkIdAlice2, networkType), getMySupportedNetworks(), storage, Config.aliceKeyRepository1);

        String baseDirNameBob = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Bob";
        NetworkId networkIdBob1 = new NetworkId(baseDirNameBob, "id_bob_1", 2222, networkTypes);
        bob = new P2pNode(new NetworkConfig(networkIdBob1, networkType), getMySupportedNetworks(), storage, Config.bobKeyRepository1);

        CountDownLatch serversReadyLatch = new CountDownLatch(3);
        alice.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        alice2.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }*/
}
