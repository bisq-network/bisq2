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

package bisq.evolution.updater;

import bisq.common.file.FileMutatorUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdaterUtilsTest {

    @Test
    void testReadVersionFromVersionFile(@TempDir Path tempDirPath) throws IOException {
        Path expectedPath = tempDirPath.resolve(UpdaterUtils.VERSION_FILE_NAME);
        FileMutatorUtils.writeToPath("12.3.6", expectedPath);

        Optional<String> result = UpdaterUtils.readVersionFromVersionFile(tempDirPath);

        assertTrue(result.isPresent());
        assertEquals("12.3.6", result.get());
    }
}