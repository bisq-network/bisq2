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

import bisq.common.util.NetworkUtils;
import bisq.network.tor.common.torrc.DirectoryAuthority;
import bisq.network.tor.common.torrc.TorrcConfigGenerator;
import bisq.network.tor.common.torrc.TorrcFileGenerator;
import bisq.network.tor.local_network.TorNode;
import bisq.network.tor.local_network.da.DirectoryAuthorityFactory;
import bisq.network.tor.local_network.torrc.TestNetworkTorrcGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryAuthorityTests {
    private static final String PASSPHRASE = "my_passphrase";

    @Test
    public void createOneDA(@TempDir Path tempDir) throws IOException, InterruptedException {
        var firstDirectoryAuthority = TorNode.builder()
                .type(TorNode.Type.DIRECTORY_AUTHORITY)
                .nickname("DA_1")
                .dataDir(tempDir)
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();
        new DirectoryAuthorityFactory().createDirectoryAuthority(firstDirectoryAuthority, PASSPHRASE);

        assertThat(tempDir).isNotEmptyDirectory();
        assertThat(tempDir.resolve("keys")).isNotEmptyDirectory();
    }

    @Test
    public void createThreeDA(@TempDir Path tempDir) throws IOException, InterruptedException {
        var dirAuthFactory = new DirectoryAuthorityFactory();

        Path firstDaDataDir = tempDir.resolve("da_1");
        var firstDirectoryAuthority = TorNode.builder()
                .type(TorNode.Type.DIRECTORY_AUTHORITY)
                .nickname("DA_1")
                .dataDir(firstDaDataDir)
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();
        dirAuthFactory.createDirectoryAuthority(firstDirectoryAuthority, PASSPHRASE);

        Path secondDaDataDir = tempDir.resolve("da_2");
        var secondDirectoryAuthority = TorNode.builder()
                .type(TorNode.Type.DIRECTORY_AUTHORITY)
                .nickname("DA_2")
                .dataDir(secondDaDataDir)
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();
        dirAuthFactory.createDirectoryAuthority(secondDirectoryAuthority, PASSPHRASE);

        Path thirdDaDataDir = tempDir.resolve("da_3");
        var thirdDirectoryAuthority = TorNode.builder()
                .type(TorNode.Type.DIRECTORY_AUTHORITY)
                .nickname("DA_3")
                .dataDir(thirdDaDataDir)
                .orPort(NetworkUtils.findFreeSystemPort())
                .dirPort(NetworkUtils.findFreeSystemPort())
                .build();
        dirAuthFactory.createDirectoryAuthority(thirdDirectoryAuthority, PASSPHRASE);

        Set<TorNode> allDAs = dirAuthFactory.getAllDirectoryAuthorities();
        for (TorNode da : allDAs) {
            TorrcConfigGenerator torDaTorrcGenerator = TestNetworkTorrcGeneratorFactory.directoryTorrcGenerator(da);
            Map<String, String> torrcConfigs = torDaTorrcGenerator.generate();

            Path torrcPath = da.getTorrcPath();
            Set<DirectoryAuthority> directoryAuthorities = allDAs.stream()
                    .map(TorNode::toDirectoryAuthority)
                    .collect(Collectors.toSet());
            var torrcFileGenerator = new TorrcFileGenerator(torrcPath, torrcConfigs, directoryAuthorities);
            torrcFileGenerator.generate();
        }
    }
}
