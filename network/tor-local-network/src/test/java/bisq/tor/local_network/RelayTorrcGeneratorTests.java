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

package bisq.tor.local_network;

import bisq.tor.local_network.torrc.RelayTorrcGenerator;
import bisq.tor.local_network.torrc.TorrcFileGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RelayTorrcGeneratorTests {
    @Test
    void basicTest(@TempDir Path tempDir) throws IOException {
        Path daAPath = tempDir.resolve("DA_A");
        assertThat(daAPath.toFile().mkdir()).isTrue();

        DirectoryAuthority firstDirAuth = DirectoryAuthority.builder()
                .nickname("A")
                .dataDir(daAPath)

                .controlPort(1)
                .orPort(2)
                .dirPort(3)

                .v3LongTermSigningKeyFingerprint("AAAA_v3")
                .torKeyFingerprint("AAAA_fp")
                .build();

        DirectoryAuthority secondDirAuth = DirectoryAuthority.builder()
                .nickname("B")
                .dataDir(tempDir.resolve("DA_B"))

                .controlPort(1)
                .orPort(2)
                .dirPort(3)

                .v3LongTermSigningKeyFingerprint("BBBB_v3")
                .torKeyFingerprint("BBBB_fp")
                .build();

        var relayTorrcGenerator = new RelayTorrcGenerator(firstDirAuth);
        var allDirAuthorities = Set.of(firstDirAuth, secondDirAuth);

        var torrcFileGenerator = new TorrcFileGenerator(relayTorrcGenerator, allDirAuthorities);
        torrcFileGenerator.generate();

        assertThat(firstDirAuth.getTorrcPath())
                .isNotEmptyFile();
    }
}
