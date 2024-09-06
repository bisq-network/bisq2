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

package bisq.tor.process.control_port;

import bisq.common.threading.ThreadName;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControlPortFilePoller {
    private final AtomicBoolean isRunning = new AtomicBoolean();
    private final CompletableFuture<Integer> portCompletableFuture = new CompletableFuture<>();
    private final Path controlPortFilePath;

    public ControlPortFilePoller(Path controlPortFilePath) {
        this.controlPortFilePath = controlPortFilePath;
    }

    public CompletableFuture<Integer> parsePort() {
        boolean isSuccess = isRunning.compareAndSet(false, true);
        if (isSuccess) {
            startPoller();
        }
        return portCompletableFuture;
    }

    private void startPoller() {
        Thread thread = new Thread(() -> {
            ThreadName.setName("ControlPortFilePoller.startPoller");
            try {
                while (true) {
                    Optional<Integer> optionalPort = parsePortFromFile();

                    if (optionalPort.isPresent()) {
                        portCompletableFuture.complete(optionalPort.get());
                        break;
                    } else {
                        // We can't use Java's WatcherService because it misses events between event processing.
                        // Tor writes the port to a swap file first, and renames it afterward.
                        // The WatcherService can miss the second operation, causing a deadlock.
                        //noinspection BusyWait
                        Thread.sleep(100);
                    }
                }

            } catch (ControlPortFileParseFailureException | InterruptedException e) {
                portCompletableFuture.completeExceptionally(e);
            }
        });

        thread.start();
    }

    private Optional<Integer> parsePortFromFile() {
        if (!controlPortFilePath.toFile().exists()) {
            return Optional.empty();
        }

        int controlPort = ControlPortFileParser.parse(controlPortFilePath);
        return Optional.of(controlPort);
    }
}
