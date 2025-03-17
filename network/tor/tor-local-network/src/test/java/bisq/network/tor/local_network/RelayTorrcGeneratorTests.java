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

package bisq.network.tor.local_network;

import bisq.network.tor.common.torrc.DirectoryAuthority;
import bisq.network.tor.common.torrc.TorrcConfigGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.network.tor.local_network.TorNode;
import bisq.network.tor.local_network.torrc.TestNetworkTorrcGeneratorFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Disabled
public class RelayTorrcGeneratorTests {
    @Test
    void basicTest(@TempDir Path tempDir) {
        Path relayAPath = tempDir.resolve("RELAY_A");
        assertThat(relayAPath.toFile().mkdir()).isTrue();

        TorNode firstRelay = spy(
                TorNode.builder()
                        .type(TorNode.Type.RELAY)
                        .nickname("A")
                        .dataDir(relayAPath)

                        .orPort(2)
                        .dirPort(3)

                        .build()
        );

        doReturn(Optional.of("AAAA_fp"))
                .when(firstRelay)
                .getAuthorityIdentityKeyFingerprint();

        doReturn(Optional.of("AAAA_v3"))
                .when(firstRelay)
                .getRelayKeyFingerprint();

        TorNode secondRelay = spy(
                TorNode.builder()
                        .type(TorNode.Type.RELAY)
                        .nickname("B")
                        .dataDir(tempDir.resolve("DA_B"))

                        .orPort(2)
                        .dirPort(3)

                        .build()
        );

        doReturn(Optional.of("BBBB_fp"))
                .when(secondRelay)
                .getAuthorityIdentityKeyFingerprint();

        doReturn(Optional.of("BBBB_v3"))
                .when(secondRelay)
                .getRelayKeyFingerprint();

        TorrcConfigGenerator relayTorrcGenerator = TestNetworkTorrcGeneratorFactory.relayTorrcGenerator(firstRelay);
        Map<String, String> torrcConfigs = relayTorrcGenerator.generate();

        TorNode dirAuth = spy(
                TorNode.builder()
                        .type(TorNode.Type.DIRECTORY_AUTHORITY)
                        .nickname("A")
                        .dataDir(tempDir.resolve("dir_auth"))

                        .orPort(2)
                        .dirPort(3)

                        .build()
        );

        doReturn(Optional.of("AAAA_fp"))
                .when(dirAuth)
                .getAuthorityIdentityKeyFingerprint();

        doReturn(Optional.of("AAAA_v3"))
                .when(dirAuth)
                .getRelayKeyFingerprint();

        Path torrcPath = firstRelay.getTorrcPath();
        Set<DirectoryAuthority> allDAs = Set.of(dirAuth.toDirectoryAuthority());
        var torrcFileGenerator = new TorrcFileGenerator(torrcPath, torrcConfigs, allDAs);
        torrcFileGenerator.generate();

        assertThat(firstRelay.getTorrcPath())
                .isNotEmptyFile();
    }
}
