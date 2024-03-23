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

package bisq.common.io_watcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryWatcherTests {
    @Test
    void detectFileCreation(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        try (var watcher = new DirectoryWatcher(tempDir, Set.of(StandardWatchEventKinds.ENTRY_CREATE))) {
            var completableFuture = new CompletableFuture<Path>();
            watcher.initialize(completableFuture::complete);

            Path newFilePath = tempDir.resolve("newFile");
            Files.writeString(newFilePath, "Hello!");

            Path path = completableFuture.get(30, TimeUnit.SECONDS);
            assertThat(path).isEqualTo(newFilePath);
        }
    }

    @Test
    void detectFileWrite(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Path newFilePath = tempDir.resolve("newFile");
        Files.writeString(newFilePath, "Hello!");

        try (var watcher = new DirectoryWatcher(tempDir, Set.of(StandardWatchEventKinds.ENTRY_MODIFY))) {
            var completableFuture = new CompletableFuture<Path>();
            watcher.initialize(completableFuture::complete);

            Files.writeString(newFilePath, "World!");

            Path path = completableFuture.get(30, TimeUnit.SECONDS);
            assertThat(path).isEqualTo(newFilePath);
        }
    }
}
