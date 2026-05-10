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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LdPreloadTest {

    @Test
    @DisplayName("compute ld preload variable returns colon separated paths")
    void compute_ld_preload_variable_returns_colon_separated_paths(@TempDir Path tempDirPath) throws IOException {
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
    @DisplayName("compute ld preload variable returns empty string if no so files")
    void compute_ld_preload_variable_returns_empty_string_if_no_so_files(@TempDir Path tempDirPath) throws IOException {
        FileMutatorUtils.createFile(tempDirPath.resolve("file.txt"));
        String result = LdPreload.computeLdPreloadVariable(tempDirPath);
        assertEquals("", result);
    }

    @Test
    @DisplayName("compute ld preload variable throws exception if dir is null")
    void compute_ld_preload_variable_throws_exception_if_dir_is_null() {
        assertThrows(NullPointerException.class, () -> LdPreload.computeLdPreloadVariable(null));
    }

    @Test
    @DisplayName("compute ld preload variable throws illegal state exception on io exception")
    void compute_ld_preload_variable_throws_illegal_state_exception_on_io_exception(@TempDir Path tempDirPath) throws IOException {
        Path notADirPath = FileMutatorUtils.createFile(tempDirPath.resolve("notADir"));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> LdPreload.computeLdPreloadVariable(notADirPath));
        assertTrue(exception.getMessage().contains("Failed to list directory"));
    }

    @Test
    @DisplayName("compute ld preload variable excludes unversioned so files")
    void compute_ld_preload_variable_excludes_unversioned_so_files(@TempDir Path tempDirPath) throws IOException {
        FileMutatorUtils.createFile(tempDirPath.resolve("libfoo.so"));  // unversioned
        FileMutatorUtils.createFile(tempDirPath.resolve("libbar.so.1")); // versioned

        String result = LdPreload.computeLdPreloadVariable(tempDirPath);

        assertFalse(result.contains("libfoo.so"));
        assertTrue(result.contains("libbar.so.1"));
    }
}
