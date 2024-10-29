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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Disabled
public class DirectoryAuthorityTorrcGeneratorTests {
    @Test
    void basicTest(@TempDir Path tempDir) {
        Path daAPath = tempDir.resolve("DA_A");
        assertThat(daAPath.toFile().mkdir()).isTrue();

        TorNode firstDirAuth = spy(
                TorNode.builder()
                        .type(TorNode.Type.DIRECTORY_AUTHORITY)
                        .nickname("A")
                        .dataDir(daAPath)

                        .orPort(2)
                        .dirPort(3)

                        .build()
        );

        doReturn(Optional.of("AAAA_fp"))
                .when(firstDirAuth)
                .getAuthorityIdentityKeyFingerprint();

        doReturn(Optional.of("AAAA_v3"))
                .when(firstDirAuth)
                .getRelayKeyFingerprint();

        TorNode secondDirAuth = spy(
                TorNode.builder()
                        .type(TorNode.Type.DIRECTORY_AUTHORITY)
                        .nickname("B")
                        .dataDir(tempDir.resolve("DA_B"))

                        .orPort(2)
                        .dirPort(3)

                        .build()
        );

        doReturn(Optional.of("BBBB_fp"))
                .when(secondDirAuth)
                .getAuthorityIdentityKeyFingerprint();

        doReturn(Optional.of("BBBB_v3"))
                .when(secondDirAuth)
                .getRelayKeyFingerprint();

        TorrcConfigGenerator torDaTorrcGenerator = TestNetworkTorrcGeneratorFactory.directoryTorrcGenerator(firstDirAuth);
        var allDirAuthorities = Set.of(firstDirAuth, secondDirAuth);

        Map<String, String> torrcConfigs = torDaTorrcGenerator.generate();

        Path torrcPath = firstDirAuth.getTorrcPath();
        Set<DirectoryAuthority> allDAs = allDirAuthorities.stream()
                .map(TorNode::toDirectoryAuthority)
                .collect(Collectors.toSet());
        var torrcFileGenerator = new TorrcFileGenerator(torrcPath, torrcConfigs, allDAs);

        torrcFileGenerator.generate();

        assertThat(firstDirAuth.getTorrcPath())
                .isNotEmptyFile();
    }
}
