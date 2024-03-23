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

package bisq.tor.process;

import bisq.common.io_watcher.CouldNotInitializeDirectoryWatcherException;
import bisq.common.io_watcher.DirectoryWatcher;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ControlPortReadyWaiter {
    @Getter
    private final CompletableFuture<Integer> portCompletableFuture = new CompletableFuture<>();
    private final DirectoryWatcher directoryWatcher;
    private final Path controlDirPath;
    private final Path controlPortFilePath;

    public ControlPortReadyWaiter(Path controlDirPath) {
        this.controlDirPath = controlDirPath;
        Set<WatchEvent.Kind<?>> watchEventKinds = Set.of(
                StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        directoryWatcher = new DirectoryWatcher(controlDirPath, watchEventKinds);
        controlPortFilePath = controlDirPath.resolve("control");
    }

    public void initialize() {
        createTorControlDirectory();
        deleteControlPortFileFromPreviousRun();

        directoryWatcher.initialize(path -> {
            if (path.equals(controlPortFilePath)) {
                try {
                    int controlPort = ControlPortFileParser.parse(controlPortFilePath);
                    portCompletableFuture.complete(controlPort);
                    close();
                } catch (IOException | ControlPortFileParseFailureException e) {
                    portCompletableFuture.completeExceptionally(e);
                }
            }
        });
    }

    public void close() throws IOException {
        directoryWatcher.close();
    }

    private void createTorControlDirectory() {
        File controlDirFile = controlDirPath.toFile();
        if (controlDirFile.exists()) {
            return;
        }

        boolean isSuccess = controlDirFile.mkdirs();
        if (!isSuccess) {
            throw new CouldNotInitializeDirectoryWatcherException("Couldn't create Tor control directory: " + controlDirPath);
        }
    }

    private void deleteControlPortFileFromPreviousRun() {
        File controlPortFile = controlPortFilePath.toFile();
        if (!controlPortFile.exists()) {
            return;
        }

        boolean isSuccess = controlPortFile.delete();
        if (!isSuccess) {
            throw new CouldNotInitializeDirectoryWatcherException("Couldn't delete Tor control port file from previous run: " + controlPortFilePath);
        }
    }
}
