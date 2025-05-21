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

import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.ThreadName;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ControlPortFilePoller {
    private final AtomicBoolean isRunning = new AtomicBoolean();
    private final CompletableFuture<Integer> portCompletableFuture = new CompletableFuture<>();
    private final Path controlPortFilePath;

    private static final int MAX_CONSECUTIVE_READ_FAILURES = 10;
    private static final int MAX_ATTEMPTS = 300;
    private static final int SETTLE_DELAY_MS = 50;
    private static final int POLLING_INTERVAL_MS = 100;

    private ScheduledExecutorService executorService;
    private int attempts = 0;
    private Optional<Integer> lastParsedPort = Optional.empty();
    private int consecutiveReadFailures = 0;

    public ControlPortFilePoller(Path controlPortFilePath) {
        this.controlPortFilePath = controlPortFilePath;
    }

    public CompletableFuture<Integer> parsePort() {
        if (isRunning.compareAndSet(false, true)) {
            submitInitialPollingTask();
        }
        return portCompletableFuture;
    }

    private void submitInitialPollingTask() {
        this.executorService = ExecutorFactory.newSingleThreadScheduledExecutor("ControlPortFilePollerThread");
        this.attempts = 0;
        this.lastParsedPort = Optional.empty();
        this.consecutiveReadFailures = 0;

        log.debug("Starting control port file poller for: {}. Max attempts: {}, Settle delay: {}ms, Poll interval: {}ms",
                controlPortFilePath, MAX_ATTEMPTS, SETTLE_DELAY_MS, POLLING_INTERVAL_MS);

        // Schedule the first poll cycle to run immediately on the executor thread
        if (canContinuePolling()) {
            executorService.submit(this::executePollCycle);
        }
    }

    private void executePollCycle() {
        if (!canContinuePolling()) return;

        this.attempts++;
        try {
            ThreadName.setName("ControlPortFilePoller.pollCycle-" + this.attempts);
            log.trace("Executing poll cycle attempt {} for {}", this.attempts, controlPortFilePath);

            if (this.attempts > MAX_ATTEMPTS) { // Check if current attempt exceeds max (e.g. if MAX_ATTEMPTS = 0, or after last attempt)
                handleTimeout();
                return;
            }

            Optional<Integer> optionalPort = parsePortFromFile();

            if (optionalPort.isPresent()) {
                int currentPort = optionalPort.get();
                if (this.lastParsedPort.isEmpty() || this.lastParsedPort.get() != currentPort) {
                    log.debug("Poller parsed new/changed port {} from {} (attempt {}). Scheduling stability check in {}ms.",
                            currentPort, controlPortFilePath, this.attempts, SETTLE_DELAY_MS);
                    if (canContinuePolling()) {
                        executorService.schedule(() -> performStabilityCheck(currentPort, this.attempts),
                                SETTLE_DELAY_MS, TimeUnit.MILLISECONDS);
                    }
                    // Stability check will handle next steps, so return from this cycle execution.
                    return;
                } else {
                    // Port is present and same as last: confirm stable.
                    log.info("ControlPortFilePoller confirmed stable port (same as last): {} from file {} (attempt {})",
                            currentPort, controlPortFilePath, this.attempts);
                    completeSuccessfully(currentPort);
                    // Polling stops.
                    return;
                }
            } else { // Initial parsePortFromFile returned empty
                handleReadFailure(this.attempts, "initial parse");
                if (portCompletableFuture.isDone()) return; // Stopped by max consecutive failures
                scheduleNextRegularPollCycle(); // Continue polling
            }
        } catch (Exception e) {
            handleUnexpectedException("pollCycle attempt " + this.attempts, e);
        }
    }

    private void performStabilityCheck(int initialPort, int originatingAttemptNum) {
        if (!canContinuePolling()) return;

        try {
            ThreadName.setName("ControlPortFilePoller.stabilityCheck-" + originatingAttemptNum);
            log.trace("Performing stability check for port {} (from attempt {}) for file {}",
                    initialPort, originatingAttemptNum, controlPortFilePath);

            Optional<Integer> finalCheckOptionalPort = parsePortFromFile();

            if (finalCheckOptionalPort.isPresent() && finalCheckOptionalPort.get() == initialPort) {
                log.info("ControlPortFilePoller confirmed stable port {} post-check (from attempt {}) for file {}",
                        initialPort, originatingAttemptNum, controlPortFilePath);
                completeSuccessfully(initialPort);
                // Polling stops.
            } else {
                // Not stable (changed or became unparseable)
                String failureContext = "stability check (originally attempt " + originatingAttemptNum + ")";
                if (finalCheckOptionalPort.isPresent()) {
                    log.warn("Control port file {} changed (was {}, now {}) during {}. Re-polling.",
                            controlPortFilePath, initialPort, finalCheckOptionalPort.get(), failureContext);
                    this.lastParsedPort = finalCheckOptionalPort;
                } else {
                    log.warn("Control port file {} became unparseable (was {}) during {}. Re-polling.",
                            controlPortFilePath, initialPort, failureContext);
                    this.lastParsedPort = Optional.empty();
                }
                handleReadFailure(originatingAttemptNum, failureContext);
                if (portCompletableFuture.isDone()) return; // Stopped by max consecutive failures
                scheduleNextRegularPollCycle(); // Continue polling
            }
        } catch (Exception e) {
            handleUnexpectedException("stabilityCheck (from attempt " + originatingAttemptNum + ")", e);
        }
    }

    private void scheduleNextRegularPollCycle() {
        if (!canContinuePolling()) return; // Double check before scheduling

        if (this.attempts < MAX_ATTEMPTS) {
            log.trace("Scheduling next poll cycle for {} (current attempts: {}).", controlPortFilePath, this.attempts);
            executorService.schedule(this::executePollCycle, POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } else {
            // Max attempts reached, ensure timeout is handled if not already.
            log.debug("Max attempts ({}) reached for {}, not scheduling further poll cycles.", MAX_ATTEMPTS, controlPortFilePath);
            handleTimeout();
        }
    }

    private void completeSuccessfully(int port) {
        if (!portCompletableFuture.isDone()) {
            portCompletableFuture.complete(port);
        }
        this.consecutiveReadFailures = 0; // Reset counter
    }

    private void handleReadFailure(int attemptContext, String phase) {
        this.consecutiveReadFailures++;
        checkConsecutiveFailuresAndCompleteExceptionally(attemptContext, this.consecutiveReadFailures, phase);
    }

    private void checkConsecutiveFailuresAndCompleteExceptionally(int currentAttemptNum,
                                                                  int failures,
                                                                  String phase) {
        if (failures >= MAX_CONSECUTIVE_READ_FAILURES) {
            log.error("ControlPortFilePoller for {} failed after {} consecutive read failures (during {}, attempt {}). Giving up.",
                    controlPortFilePath, failures, phase, currentAttemptNum);
            if (!portCompletableFuture.isDone()) {
                portCompletableFuture.completeExceptionally(
                        new ControlPortFileParseFailureException("Failed to parse/stabilize port file '" + controlPortFilePath
                                + "' after " + failures + " consecutive read failures (phase: " + phase + ", attempt: " + currentAttemptNum + ")."));
            }
            // No return value needed, side effect is completing the future.
        } else {
            // Log trace only if we are not completing exceptionally due to max failures.
            log.trace("Read failure for file {} (phase: {}, attempt: {}). Consecutive failures: {}/{}.",
                    controlPortFilePath, phase, currentAttemptNum, failures, MAX_CONSECUTIVE_READ_FAILURES);
        }
    }

    private void handleTimeout() {
        if (!portCompletableFuture.isDone()) {
            log.error("ControlPortFilePoller timed out for file {} after {} attempts.",
                    controlPortFilePath, this.attempts);
            portCompletableFuture.completeExceptionally(
                    new TimeoutException("ControlPortFilePoller timed out for file '" + controlPortFilePath + "' after "
                            + this.attempts + " attempts."));
        }
    }

    private void handleUnexpectedException(String context, Exception e) {
        log.error("ControlPortFilePoller for {} encountered an unexpected error during {}.", controlPortFilePath, context, e);
        if (!portCompletableFuture.isDone()) {
            portCompletableFuture.completeExceptionally(e);
        }
    }

    private boolean canContinuePolling() {
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Polling for {} interrupted.", controlPortFilePath);
            if (!portCompletableFuture.isDone())
                portCompletableFuture.completeExceptionally(new InterruptedException("Polling was interrupted."));
            return false;
        }
        if (executorService == null || executorService.isShutdown()) {
            log.warn("Polling for {} cannot continue: executor service not available or shutdown.", controlPortFilePath);
            if (!portCompletableFuture.isDone())
                portCompletableFuture.completeExceptionally(new CancellationException("Executor service unavailable."));
            return false;
        }
        if (portCompletableFuture.isDone()) {
            log.trace("Polling for {} stopped: future already completed.", controlPortFilePath);
            return false;
        }
        return true;
    }

    public void shutdown() {
        log.info("Attempting to shutdown ControlPortFilePoller for {}", controlPortFilePath);
        isRunning.set(false); // Prevent new polling sessions from starting if parsePort() is called again.

        ScheduledExecutorService serviceToShutdown = this.executorService;
        if (serviceToShutdown != null) {
            ExecutorFactory.shutdownAndAwaitTermination(serviceToShutdown, 200, TimeUnit.MILLISECONDS);
            this.executorService = null;
        }

        if (!portCompletableFuture.isDone()) {
            log.warn("ControlPortFilePoller for {} was shut down before external completion.", controlPortFilePath);
            portCompletableFuture.completeExceptionally(new CancellationException("ControlPortFilePoller for " + controlPortFilePath + " was shut down by external request."));
        }
    }

    private Optional<Integer> parsePortFromFile() {
        try {
            if (!controlPortFilePath.toFile().exists()) {
                log.trace("Control port file does not exist yet: {}", controlPortFilePath);
                return Optional.empty();
            }
            int controlPort = ControlPortFileParser.parse(controlPortFilePath);
            return Optional.of(controlPort);
        } catch (Exception e) {
            log.warn("Failed to parse control port from file {} due to: {}. Treating as transient error.",
                    controlPortFilePath, e.getMessage());
            return Optional.empty();
        }
    }
}
