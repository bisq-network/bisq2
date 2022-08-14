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

package bisq.wallets.electrum;

import bisq.common.util.FileUtils;
import bisq.wallets.electrum.rpc.cli.ElectrumCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ElectrumBundledBinaryTest {
    private final ElectrumBinaryExtractor binaryExtractor;

    public ElectrumBundledBinaryTest() throws IOException {
        Path destDirPath = FileUtils.createTempDir();
        binaryExtractor = new ElectrumBinaryExtractor(destDirPath);
    }

    @Test
    @EnabledOnOs({OS.LINUX})
    void runHelpTest() throws IOException {
        Path electrumBinaryPath = binaryExtractor.extractFileWithSuffix("AppImage");
        boolean isExecutableNow = electrumBinaryPath.toFile().setExecutable(true);
        assertThat(isExecutableNow).isTrue();

        Path dataDir = FileUtils.createTempDir();
        ElectrumCli electrumCli = new ElectrumCli(electrumBinaryPath, dataDir);

        String helpOutput = electrumCli.help();
        assertThat(helpOutput).isNotEmpty();
    }
}
