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

package bisq.network.tor.process.control_port;

import bisq.common.threading.ThreadName;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeoutException;

@Slf4j
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
                int attemptLimit = 300; // e.g., try for up to 30 seconds (300 * 100ms interval)
                int attempts = 0;
                Optional<Integer> lastParsedPort = Optional.empty();
                int consecutiveReadFailures = 0;
                final int MAX_CONSECUTIVE_READ_FAILURES = 10; // Increased limit for robustness
                final int SETTLE_DELAY_MS = 50;
                final int POLLING_INTERVAL_MS = 100;

                log.debug("Starting control port file poller for: {}. Timeout: {}s",
                        controlPortFilePath, attemptLimit * POLLING_INTERVAL_MS / 1000);

                while (attempts < attemptLimit) {
                    attempts++;
                    Optional<Integer> optionalPort = parsePortFromFile();

                    if (optionalPort.isPresent()) {
                        consecutiveReadFailures = 0; // Reset counter on successful parse
                        int currentPort = optionalPort.get();

                        // Optimization: if port hasn't changed and we just failed (external check
                        // needed for this),
                        // or just to ensure stability, wait briefly after initial parse.
                        if (lastParsedPort.isEmpty() || lastParsedPort.get() != currentPort) {
                            log.debug("Poller parsed port {} (attempt {}). Waiting {}ms to check for stability.",
                                    currentPort, attempts, SETTLE_DELAY_MS);
                            Thread.sleep(SETTLE_DELAY_MS);
                            Optional<Integer> finalCheckOptionalPort = parsePortFromFile();

                            if (finalCheckOptionalPort.isPresent() && finalCheckOptionalPort.get() == currentPort) {
                                log.info("ControlPortFilePoller confirmed stable port: {}", currentPort);
                                portCompletableFuture.complete(currentPort);
                                break;
                            } else if (finalCheckOptionalPort.isPresent()) {
                                log.warn(
                                        "Control port file changed rapidly after initial parse ({} -> {}). Re-polling.",
                                        currentPort, finalCheckOptionalPort.get());
                                lastParsedPort = finalCheckOptionalPort; // Update to the newest read
                            } else {
                                log.warn("Control port file became unparseable after initial parse. Re-polling.");
                                lastParsedPort = Optional.empty(); // Reset last known good port
                            }
                        } else {
                            // Port parsed is the same as the last attempt, assume stable
                            log.info("ControlPortFilePoller confirmed stable port (same as last): {}", currentPort);
                            portCompletableFuture.complete(currentPort);
                            break;
                        }

                    } else { // parsePortFromFile returned empty (e.g. file not found or not ready)
                        consecutiveReadFailures++;
                        log.trace("Control port file not found or not ready, attempt {}. Consecutive failures: {}.",
                                attempts, consecutiveReadFailures);
                        if (consecutiveReadFailures >= MAX_CONSECUTIVE_READ_FAILURES) {
                            log.error(
                                    "ControlPortFilePoller failed to parse port file after {} consecutive attempts. Giving up.",
                                    MAX_CONSECUTIVE_READ_FAILURES);
                            portCompletableFuture.completeExceptionally(
                                    new ControlPortFileParseFailureException("Failed to parse port file after "
                                            + MAX_CONSECUTIVE_READ_FAILURES + " attempts."));
                            break;
                        }
                        lastParsedPort = Optional.empty(); // Reset if file unparseable
                    }

                    // Main polling interval wait
                    Thread.sleep(POLLING_INTERVAL_MS);

                }

                if (!portCompletableFuture.isDone()) {
                    log.error("ControlPortFilePoller timed out after {} attempts ({} seconds) for file: {}",
                            attempts, attemptLimit * POLLING_INTERVAL_MS / 1000, controlPortFilePath);
                    portCompletableFuture.completeExceptionally(
                            new TimeoutException("ControlPortFilePoller timed out after "
                                    + (attemptLimit * POLLING_INTERVAL_MS / 1000) + " seconds."));
                }

            } catch (InterruptedException e) {
                log.warn("ControlPortFilePoller interrupted.", e);
                portCompletableFuture.completeExceptionally(e);
                Thread.currentThread().interrupt();
            } catch (Exception e) { // Catch unexpected errors during polling
                log.error("ControlPortFilePoller encountered an unexpected error.", e);
                portCompletableFuture.completeExceptionally(e);
            }
        }, "ControlPortFilePollerThread");

        thread.setDaemon(true); // Ensure poller thread doesn't prevent JVM shutdown
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
