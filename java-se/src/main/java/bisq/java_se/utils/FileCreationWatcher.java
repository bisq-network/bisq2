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

package bisq.java_se.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FileCreationWatcher {
    private final Path directoryToWatch;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FileCreationWatcher(Path directoryToWatch) {
        this.directoryToWatch = directoryToWatch;
    }

    public Future<Path> waitUntilNewFileCreated() {
        return executor.submit(() -> waitForNewFile(Optional.empty()));
    }

    public Future<Path> waitForFile(Path path) {
        return executor.submit(() -> waitForNewFile(Optional.of(path)));
    }

    private Path waitForNewFile(Optional<Path> optionalPath) {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directoryToWatch.register(watchService,
                    new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE});
            while (true) {
                WatchKey watchKey = watchService.poll(1, TimeUnit.MINUTES);
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> castedWatchEvent = (WatchEvent<Path>) event;
                    Path filename = castedWatchEvent.context();
                    Path newFilePath = directoryToWatch.resolve(filename);

                    if (optionalPath.isEmpty()) {
                        return newFilePath;
                    } else if (optionalPath.get().equals(newFilePath)) {
                        return newFilePath;
                    }
                    watchKey.reset();
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error("Couldn't watch directory: {}", directoryToWatch.toAbsolutePath(), e);
        }

        throw new IllegalStateException("FileCreationWatcher terminated prematurely.");
    }
}
