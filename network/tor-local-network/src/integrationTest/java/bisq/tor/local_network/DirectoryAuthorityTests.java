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

import bisq.common.util.NetworkUtils;
import bisq.tor.local_network.torrc.DirectoryAuthorityTorrcGenerator;
import bisq.tor.local_network.torrc.TorrcFileGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryAuthorityTests {

    @Test
    public void createOneDA(@TempDir Path tempDir) throws IOException, InterruptedException {
        var firstDirectoryAuthority = DirectoryAuthority.builder()
                .nickname("DA_1")
                .dataDir(tempDir)
                .controlPort(NetworkUtils.findFreeSystemPort())
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();
        DirectoryAuthorityFactory.createDirectoryAuthority(firstDirectoryAuthority, "my_passphrase");

        assertThat(tempDir).isNotEmptyDirectory();
        assertThat(tempDir.resolve("keys")).isNotEmptyDirectory();
    }

    @Test
    public void createThreeDA(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path firstDaDataDir = tempDir.resolve("da_1");
        var firstDirectoryAuthority = DirectoryAuthority.builder()
                .nickname("DA_1")
                .dataDir(firstDaDataDir)
                .controlPort(NetworkUtils.findFreeSystemPort())
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();

        Path secondDaDataDir = tempDir.resolve("da_2");
        var secondDirectoryAuthority = DirectoryAuthority.builder()
                .nickname("DA_2")
                .dataDir(secondDaDataDir)
                .controlPort(NetworkUtils.findFreeSystemPort())
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();

        Path thirdDaDataDir = tempDir.resolve("da_3");
        var thirdDirectoryAuthority = DirectoryAuthority.builder()
                .nickname("DA_3")
                .dataDir(thirdDaDataDir)
                .controlPort(NetworkUtils.findFreeSystemPort())
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();

        Set<DirectoryAuthority> allDAs = Set.of(
                firstDirectoryAuthority,
                secondDirectoryAuthority,
                thirdDirectoryAuthority);

        // Generate all keys to have fingerprints
        for (DirectoryAuthority da : allDAs) {
            DirectoryAuthorityFactory.createDirectoryAuthority(da, "my_passphrase");
        }

        // Fingerprints are now available
        for (DirectoryAuthority da : allDAs) {
            var torDaTorrcGenerator = new DirectoryAuthorityTorrcGenerator(da);
            var torrcFileGenerator = new TorrcFileGenerator(torDaTorrcGenerator, allDAs);
            torrcFileGenerator.generate();
        }
    }
}
