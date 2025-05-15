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
    private static final int MAX_CONSECUTIVE_READ_FAILURES_CONST = 10;

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

    private boolean checkConsecutiveFailuresAndCompleteExceptionally(int currentAttemptCount, int failures) {
        if (failures >= ControlPortFilePoller.MAX_CONSECUTIVE_READ_FAILURES_CONST) {
            log.error("ControlPortFilePoller failed to parse/stabilize port after {} consecutive read failures (total attempts: {}). Giving up for file: {}",
                    failures, currentAttemptCount, controlPortFilePath);
            portCompletableFuture.completeExceptionally(
                    new ControlPortFileParseFailureException("Failed to parse/stabilize port file '" + controlPortFilePath + "' after "
                            + failures + " consecutive read failures."));
            return true;
        }
        log.trace("Read failure for file {} on attempt {}. Consecutive failures: {}/{}.",
                controlPortFilePath, currentAttemptCount, failures, ControlPortFilePoller.MAX_CONSECUTIVE_READ_FAILURES_CONST);
        return false;
    }

    private void startPoller() {
        Thread thread = new Thread(() -> {
            ThreadName.setName("ControlPortFilePoller.startPoller");
            try {
                int attemptLimit = 300; // e.g., try for up to 30 seconds (300 * 100ms interval)
                int attempts = 0;
                Optional<Integer> lastParsedPort = Optional.empty();
                int consecutiveReadFailures = 0;
                final int SETTLE_DELAY_MS = 50;
                final int POLLING_INTERVAL_MS = 100;

                log.debug("Starting control port file poller for: {}. Timeout: {}s. Max consecutive read failures: {}",
                        controlPortFilePath, attemptLimit * POLLING_INTERVAL_MS / 1000, MAX_CONSECUTIVE_READ_FAILURES_CONST);

                while (attempts < attemptLimit) {
                    attempts++;
                    Optional<Integer> optionalPort = parsePortFromFile();
                    boolean stablePortConfirmedThisIteration = false;

                    if (optionalPort.isPresent()) {
                        int currentPort = optionalPort.get();
                        
                        if (lastParsedPort.isEmpty() || lastParsedPort.get() != currentPort) {
                            log.debug("Poller parsed port {} from {} (attempt {}). Waiting {}ms to check for stability.",
                                    currentPort, controlPortFilePath, attempts, SETTLE_DELAY_MS);
                            Thread.sleep(SETTLE_DELAY_MS);
                            Optional<Integer> finalCheckOptionalPort = parsePortFromFile();

                            if (finalCheckOptionalPort.isPresent() && finalCheckOptionalPort.get() == currentPort) {
                                log.info("ControlPortFilePoller confirmed stable port: {} from file {}", currentPort, controlPortFilePath);
                                portCompletableFuture.complete(currentPort);
                                stablePortConfirmedThisIteration = true;
                            } else if (finalCheckOptionalPort.isPresent()) {
                                log.warn(
                                        "Control port file {} changed rapidly after initial parse ({} -> {}). Re-polling.",
                                        controlPortFilePath, currentPort, finalCheckOptionalPort.get());
                                lastParsedPort = finalCheckOptionalPort;
                            } else {
                                log.warn("Control port file {} became unparseable after initial parse (was {}). Re-polling.",
                                         controlPortFilePath, currentPort);
                                lastParsedPort = Optional.empty();
                            }
                        } else {
                            log.info("ControlPortFilePoller confirmed stable port (same as last): {} from file {}", currentPort, controlPortFilePath);
                            portCompletableFuture.complete(currentPort);
                            stablePortConfirmedThisIteration = true;
                        }

                        if (stablePortConfirmedThisIteration) {
                            break;
                        } else {
                            // Initial parse was OK, but stability check failed (unparseable or changed and not completing)
                            // This path is taken if !stablePortConfirmedThisIteration after optionalPort.isPresent() was true.
                            // This ensures that if an initial parse resets the counter, but the stability check fails,
                            // it's still counted as a failure for this polling cycle's attempt to get a stable port.
                            consecutiveReadFailures++;
                            if (checkConsecutiveFailuresAndCompleteExceptionally(attempts, consecutiveReadFailures)) {
                                break;
                            }
                        }
                    } else { // parsePortFromFile returned empty (initial parse failed)
                        consecutiveReadFailures++;
                        log.trace("Control port file {} not found or not ready, attempt {}. Consecutive failures: {}/{}.",
                                controlPortFilePath, attempts, consecutiveReadFailures, MAX_CONSECUTIVE_READ_FAILURES_CONST);
                        if (checkConsecutiveFailuresAndCompleteExceptionally(attempts, consecutiveReadFailures)) {
                            break;
                        }
                        lastParsedPort = Optional.empty();
                    }

                    if (portCompletableFuture.isDone()) {
                        break;
                    }
                    
                    Thread.sleep(POLLING_INTERVAL_MS);
                }

                if (!portCompletableFuture.isDone()) {
                    log.error("ControlPortFilePoller timed out for file {} after {} attempts ({} seconds).",
                            controlPortFilePath, attempts, attemptLimit * POLLING_INTERVAL_MS / 1000);
                    portCompletableFuture.completeExceptionally(
                            new TimeoutException("ControlPortFilePoller timed out for file '" + controlPortFilePath + "' after "
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
        try {
            if (!controlPortFilePath.toFile().exists()) {
                log.trace("Control port file does not exist yet: {}", controlPortFilePath);
                return Optional.empty();
            }

            // ControlPortFileParser.parse might throw an exception if the file content is invalid
            int controlPort = ControlPortFileParser.parse(controlPortFilePath);
            return Optional.of(controlPort);
        } catch (Exception e) { // Catching parsing exceptions (e.g., NumberFormatException, IOException from parser)
            log.warn("Failed to parse control port from file {} due to: {}. Treating as transient error.",
                    controlPortFilePath, e.getMessage());
            // For more detailed diagnostics, one might log the full stack trace at DEBUG level:
            // log.debug("Full stack trace of parsing error for file: {}", controlPortFilePath, e);
            return Optional.empty(); // Return empty on parse failure, allowing poller to retry
        }
    }
}
