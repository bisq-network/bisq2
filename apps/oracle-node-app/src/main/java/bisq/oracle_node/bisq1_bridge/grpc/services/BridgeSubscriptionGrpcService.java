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

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.common.application.DevMode;
import bisq.common.application.Service;
import bisq.common.timer.Delay;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public abstract class BridgeSubscriptionGrpcService<T> implements Service {
    private static final int LAUNCH_BLOCK_HEIGHT = 832353; // block height on Feb 28 2024
    private static final long MAX_RETRY_REQUEST_ATTEMPTS = 30;

    protected final boolean staticPublicKeysProvided;
    protected final GrpcClient grpcClient;
    protected final AtomicLong subscribeRetryInterval = new AtomicLong(1);
    protected final AtomicLong retryRequestInterval = new AtomicLong(1);
    protected final AtomicLong retryRequestAttempts = new AtomicLong(0);
    private final AtomicReference<RequestState> requestState = new AtomicReference<>(RequestState.initial());
    private final AtomicBoolean streamRecoveryScheduled = new AtomicBoolean();
    protected volatile boolean shutdownCalled;

    public BridgeSubscriptionGrpcService(boolean staticPublicKeysProvided, GrpcClient grpcClient) {
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.grpcClient = grpcClient;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        subscribe();
        request();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        shutdownCalled = true;
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    protected void request() {
        if (shutdownCalled) {
            return;
        }
        requestState.updateAndGet(RequestState::queueRequest);
        executePendingRequest();
    }

    private void executePendingRequest() {
        if (shutdownCalled) {
            return;
        }
        RequestState currentState;
        do {
            currentState = requestState.get();
            if (currentState.requestInProgress() || !currentState.hasPendingRequest()) {
                return;
            }
        } while (!requestState.compareAndSet(currentState, currentState.startRequest()));

        boolean historicalRequestPrepared = false;
        boolean historicalRequestSuccessful = false;
        try {
            onRequestExecutionAcquired();
            int startBlockHeight = prepareHistoricalRequest();
            historicalRequestPrepared = true;
            doRequest(startBlockHeight).forEach(this::handleResponse);
            onHistoricalRequestComplete();
            historicalRequestSuccessful = true;

            retryRequestAttempts.set(0);
            retryRequestInterval.set(1);
        } catch (Exception e) {
            handleRequestException(e);
        } finally {
            try {
                if (historicalRequestPrepared) {
                    onHistoricalRequestFinished(historicalRequestSuccessful);
                }
            } finally {
                RequestState completedState = requestState.updateAndGet(RequestState::completeRequest);
                if (completedState.hasPendingRequest() && !shutdownCalled) {
                    CompletableFuture.runAsync(this::executePendingRequest, commonForkJoinPool());
                }
            }
        }
    }

    protected void requestAsync() {
        CompletableFuture.runAsync(this::request, commonForkJoinPool());
    }

    protected int getStartBlockHeight() {
        // For regtest we use devMode
        return DevMode.isDevMode() ? 0 : LAUNCH_BLOCK_HEIGHT;
    }

    protected int prepareHistoricalRequest() {
        return getStartBlockHeight();
    }

    protected abstract List<T> doRequest(int startBlockHeight);

    protected abstract void handleResponse(T data);

    protected void onHistoricalRequestComplete() {
    }

    protected void onHistoricalRequestFinished(boolean successful) {
    }

    @VisibleForTesting
    void onRequestExecutionAcquired() {
    }

    protected abstract void subscribe();

    protected void handleRequestException(Exception exception) {
        if (shutdownCalled) {
            return;
        }
        if (exception instanceof StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();
            if (status.getCode() == Status.Code.FAILED_PRECONDITION) {
                log.warn(statusRuntimeException.getMessage());
                // We do not check for retryRequestAttempts as we prefer to keep retrying until blockchain
                // parsing is completed.
                // It can take considerable time until that happens.
                Delay.run(this::request)
                        .withExecutor(commonForkJoinPool())
                        .after(10, TimeUnit.SECONDS);
            } else if (status.getCode() == Status.Code.UNIMPLEMENTED) {
                log.warn("Request rejected because the grpc server does not implement the method. " +
                                "Check that the Bisq1 grpc service is running on the configured port and is up to date. " +
                                "Status: {}{}",
                        status.getCode(),
                        status.getDescription() == null ? "" : " (" + status.getDescription() + ")");
            } else if (status.getCode() == Status.Code.INTERNAL) {
                log.warn("Request rejected because of grpc server error.", exception);
                retryRequest();
            } else {
                log.warn("Request rejected because of unknown server error (code: {}).", status.getCode(), exception);
                retryRequest();
            }
        } else {
            log.warn("Request rejected because of error", exception);
            retryRequest();
        }
    }

    private void retryRequest() {
        if (retryRequestAttempts.getAndIncrement() < MAX_RETRY_REQUEST_ATTEMPTS) {
            log.warn("Retrying request (attempt #{}/{}), delay: {}s",
                    retryRequestAttempts.get(), MAX_RETRY_REQUEST_ATTEMPTS, retryRequestInterval.get());
            long delay = retryRequestInterval.updateAndGet(prev -> Math.min(20, prev * 2));
            Delay.run(this::request)
                    .withExecutor(commonForkJoinPool())
                    .after(delay, TimeUnit.SECONDS);
        } else {
            log.error("We stop trying to request after {} unsuccessful attempts", retryRequestAttempts.get());
        }
    }

    protected void handleStreamObserverError(Throwable throwable) {
        recoverStream(throwable);
    }

    protected void handleStreamObserverCompleted() {
        recoverStream(new IllegalStateException("Bridge block subscription completed unexpectedly"));
    }

    private void recoverStream(Throwable throwable) {
        if (shutdownCalled) {
            return;
        }
        Status status = Status.fromThrowable(throwable);
        if (status.getCode() == Status.Code.UNIMPLEMENTED) {
            log.warn("Bridge version mismatch: the configured Bisq 1 bridge does not implement the " +
                            "continuity-aware block subscription. Upgrade the bridge before starting the oracle. " +
                            "Status: {}{}",
                    status.getCode(),
                    status.getDescription() == null ? "" : " (" + status.getDescription() + ")");
            return;
        }
        if (!streamRecoveryScheduled.compareAndSet(false, true)) {
            return;
        }

        log.error("Bridge stream ended. We resubscribe and catch up after {} sec. Error message: {}",
                subscribeRetryInterval.get(), throwable.getMessage());
        Delay.run(() -> {
                    streamRecoveryScheduled.set(false);
                    if (!shutdownCalled) {
                        subscribe();
                        request();
                    }
                })
                .withExecutor(commonForkJoinPool())
                .after(subscribeRetryInterval.get(), TimeUnit.SECONDS);
        subscribeRetryInterval.set(Math.min(10, subscribeRetryInterval.incrementAndGet()));
    }

    private record RequestState(boolean requestInProgress,
                                long requestedGeneration,
                                long startedGeneration) {
        private static RequestState initial() {
            return new RequestState(false, 0, 0);
        }

        private RequestState queueRequest() {
            return new RequestState(requestInProgress, requestedGeneration + 1, startedGeneration);
        }

        private boolean hasPendingRequest() {
            return requestedGeneration != startedGeneration;
        }

        private RequestState startRequest() {
            return new RequestState(true, requestedGeneration, requestedGeneration);
        }

        private RequestState completeRequest() {
            return new RequestState(false, requestedGeneration, startedGeneration);
        }
    }
}
