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

package bisq.network.tor.process;

import bisq.common.file.FileMutatorUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LdPreloadTest {

    @Test
    void computeLdPreloadVariable_returnsColonSeparatedPaths(@TempDir Path tempDirPath) throws IOException {
        Path libPath1 = FileMutatorUtils.createFile(tempDirPath.resolve("libfoo.so.1"));
        Path libPath2 = FileMutatorUtils.createFile(tempDirPath.resolve("libbar.so.2"));
        FileMutatorUtils.createFile(tempDirPath.resolve("notalib.txt"));

        String result = LdPreload.computeLdPreloadVariable(tempDirPath);

        assertTrue(result.contains(libPath1.toAbsolutePath().toString()));
        assertTrue(result.contains(libPath2.toAbsolutePath().toString()));
        assertFalse(result.contains("notalib.txt"));
        assertTrue(result.contains(":"));
    }

    @Test
    void computeLdPreloadVariable_returnsEmptyStringIfNoSoFiles(@TempDir Path tempDirPath) throws IOException {
        FileMutatorUtils.createFile(tempDirPath.resolve("file.txt"));
        String result = LdPreload.computeLdPreloadVariable(tempDirPath);
        assertEquals("", result);
    }

    @Test
    void computeLdPreloadVariable_throwsExceptionIfDirIsNull() {
        assertThrows(NullPointerException.class, () -> LdPreload.computeLdPreloadVariable(null));
    }

    @Test
    void computeLdPreloadVariable_throwsIllegalStateExceptionOnIOException(@TempDir Path tempDirPath) throws IOException {
        Path notADirPath = FileMutatorUtils.createFile(tempDirPath.resolve("notADir"));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LdPreload.computeLdPreloadVariable(notADirPath));
        assertTrue(exception.getMessage().contains("Failed to list directory"));
    }

    @Test
    void computeLdPreloadVariable_excludesUnversionedSoFiles(@TempDir Path tempDirPath) throws IOException {
        FileMutatorUtils.createFile(tempDirPath.resolve("libfoo.so"));  // unversioned
        FileMutatorUtils.createFile(tempDirPath.resolve("libbar.so.1")); // versioned

        String result = LdPreload.computeLdPreloadVariable(tempDirPath);

        assertFalse(result.contains("libfoo.so"));
        assertTrue(result.contains("libbar.so.1"));
    }
}
