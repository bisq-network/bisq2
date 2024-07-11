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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ElectrumBinaryExtractorTest {
    private final Path destDirPath = FileUtils.createTempDir();
    private final ElectrumBinaryExtractor binaryExtractor = new ElectrumBinaryExtractor(destDirPath);

    public ElectrumBinaryExtractorTest() throws IOException {
    }

    @Test
    void extractBinaries() {
        binaryExtractor.extractArchive();

        File[] filesInDir = destDirPath.toFile().listFiles();
        assertThat(filesInDir).isNotEmpty();

        File extractedFile = filesInDir[0];
        String binarySuffix = ElectrumProcess.getBinarySuffix();
        assertThat(extractedFile.getName())
                .endsWith(binarySuffix);
    }
}
