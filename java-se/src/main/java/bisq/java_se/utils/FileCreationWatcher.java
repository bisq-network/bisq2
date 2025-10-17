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
    private final Path dirPathToWatch;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FileCreationWatcher(Path dirPathToWatch) {
        this.dirPathToWatch = dirPathToWatch;
    }

    public Future<Path> waitUntilNewFileCreated() {
        return executor.submit(() -> waitForNewFilePath(Optional.empty()));
    }

    private Path waitForNewFilePath(Optional<Path> optionalPath) {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            dirPathToWatch.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey watchKey = watchService.poll(1, TimeUnit.MINUTES);
                if (watchKey == null) {
                    throw new FileCreationWatcherTimeoutException("No changes detected for 1 minute (timeout).");
                }

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> castedWatchEvent = (WatchEvent<Path>) event;
                    Path filenamePath = castedWatchEvent.context();
                    Path newFilePath = dirPathToWatch.resolve(filenamePath);

                    if (optionalPath.isEmpty()) {
                        return newFilePath;
                    } else if (optionalPath.get().equals(newFilePath)) {
                        return newFilePath;
                    }
                }

                if (!watchKey.reset()) {
                    log.warn("File watcher is no longer valid.");
                }
            }
        } catch (InterruptedException e) {
            log.error("Couldn't watch directory: {}. Thread got interrupted at waitForNewFilePath method", dirPathToWatch.toAbsolutePath(), e);
            Thread.currentThread().interrupt(); // Restore interrupted state
        } catch (IOException e) {
            log.error("Couldn't watch directory: {}", dirPathToWatch.toAbsolutePath(), e);
        }

        throw new IllegalStateException("FileCreationWatcher terminated prematurely.");
    }
}
