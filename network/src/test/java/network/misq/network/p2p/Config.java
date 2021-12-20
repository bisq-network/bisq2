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

//TODO Commented out as network layer has changed. not sure yet the test will be reactivate/rewritten or delete later.
// leave it for now...
@Slf4j
public abstract class Config {
   /* protected static KeyPair keyPairAlice1, keyPairBob1, keyPairAlice2, keyPairBob2;

    static {
        try {
            keyPairAlice1 = KeyGeneration.generateKeyPair();
            keyPairBob1 = KeyGeneration.generateKeyPair();
            keyPairAlice2 = KeyGeneration.generateKeyPair();
            keyPairBob2 = KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    protected enum Role {
        Alice,
        Bob,
        Carol
    }

    final static KeyPairRepository aliceKeyPairSupplier1 = new KeyPairRepository(new KeyPairRepository.Options(""));
    final static KeyPairRepository aliceKeyPairSupplier2 = new KeyPairRepository(new KeyPairRepository.Options(""));
    final static KeyPairRepository bobKeyPairSupplier1 = new KeyPairRepository(new KeyPairRepository.Options(""));
    final static KeyPairRepository bobKeyPairSupplier2 = new KeyPairRepository(new KeyPairRepository.Options(""));

    static {
        aliceKeyPairSupplier1.add(keyPairAlice1, "default");
        aliceKeyPairSupplier2.add(keyPairAlice2, "default");
        bobKeyPairSupplier1.add(keyPairBob1, "default");
        bobKeyPairSupplier2.add(keyPairBob2, "default");
    }

    static NetworkServiceConfig getI2pNetworkConfig(Role role) {
        // Session in I2P need to be unique even if we use 2 diff SAM instances. So we add role name to default server nodeId.
        return getI2pNetworkConfig(role, "default" + role);
    }

    static NetworkServiceConfig getI2pNetworkConfig(Role role, String id) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NodeId nodeId = new NodeId(id, -1, Sets.newHashSet(NetworkType.I2P));
        return new NetworkServiceConfig(baseDirName, nodeId, NetworkType.I2P);
    }

    static NetworkServiceConfig getTorNetworkConfig(Role role) {
        return getTorNetworkConfig(role, "default", 9999);
    }

    static NetworkServiceConfig getTorNetworkConfig(Role role, String id, int serverPort) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NodeId nodeId = new NodeId(id, serverPort, Sets.newHashSet(NetworkType.TOR));
        return new NetworkServiceConfig(baseDirName, nodeId, NetworkType.TOR);
    }

    static NetworkServiceConfig getClearNetNetworkConfig(Role role) {
        int serverPort;
        switch (role) {
            case Alice:
                serverPort = 1111;
                break;
            case Bob:
                serverPort = 2222;
                break;
            case Carol:
            default:
                serverPort = 3333;
                break;
        }
        return getClearNetNetworkConfig(role, "default", serverPort);
    }

    static NetworkServiceConfig getClearNetNetworkConfig(Role role, String id, int serverPort) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NodeId nodeId = new NodeId(id, serverPort, Sets.newHashSet(NetworkType.CLEAR));
        return new NetworkServiceConfig(baseDirName, nodeId, NetworkType.CLEAR);
    }*/
}
