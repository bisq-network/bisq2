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

package bisq.network.tor.local_network.torrc;

import bisq.network.tor.common.torrc.*;
import bisq.network.tor.local_network.TorNode;

import java.util.Optional;

public class TestNetworkTorrcGeneratorFactory {
    public static TorrcConfigGenerator directoryTorrcGenerator(TorNode directoryNode) {
        var testNetworkTorrcGenerator = testNetworkTorrcGenerator(directoryNode);
        return new DirectoryAuthorityTorrcGenerator(testNetworkTorrcGenerator, directoryNode.getNickname());
    }

    public static TorrcConfigGenerator relayTorrcGenerator(TorNode relayNode) {
        var testNetworkTorrcGenerator = testNetworkTorrcGenerator(relayNode);
        return new RelayTorrcGenerator(testNetworkTorrcGenerator);
    }

    public static TorrcConfigGenerator clientTorrcGenerator(TorNode clientNode) {
        var testNetworkTorrcGenerator = testNetworkTorrcGenerator(clientNode);
        return ClientTorrcGenerator.builder()
                .baseTorrcConfigGenerator(testNetworkTorrcGenerator)
                .build();
    }

    private static TorrcConfigGenerator testNetworkTorrcGenerator(TorNode torNode) {
        return TestNetworkTorrcGenerator.builder()
                .baseTorrcConfigGenerator(baseTorrcGenerator(torNode))
                .nickname(Optional.of(torNode.getNickname()))
                .orPort(Optional.of(torNode.getOrPort()))
                .dirPort(Optional.of(torNode.getDirPort()))
                .build();
    }

    private static TorrcConfigGenerator baseTorrcGenerator(TorNode torNode) {
        return BaseTorrcGenerator.builder()
                .dataDirPath(torNode.getDataDir())
                .hashedControlPassword(
                        torNode.getControlConnectionPassword()
                                .getHashedPassword())
                .build();
    }
}
