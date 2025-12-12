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

import bisq.common.application.Service;
import bisq.common.timer.Delay;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public abstract class BridgeSubscriptionGrpcService<T> implements Service {
    private static final int LAUNCH_BLOCK_HEIGHT = 832353; // block height on Feb 28 2024
    private static final long MAX_RETRY_REQUEST_ATTEMPTS = 30;

    protected final boolean staticPublicKeysProvided;
    protected final GrpcClient grpcClient;
    protected final BlockingQueue<AuthorizedDistributedData> queue;
    protected final AtomicLong subscribeRetryInterval = new AtomicLong(1);
    protected final AtomicLong retryRequestInterval = new AtomicLong(1);
    protected final AtomicLong retryRequestAttempts = new AtomicLong(0);
    protected volatile boolean shutdownCalled;

    public BridgeSubscriptionGrpcService(boolean staticPublicKeysProvided,
                                         GrpcClient grpcClient,
                                         BlockingQueue<AuthorizedDistributedData> queue) {
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.grpcClient = grpcClient;
        this.queue = queue;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        request();
        subscribe();
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
        try {
            doRequest(getStartBlockHeight()).forEach(this::handleResponse);

            retryRequestAttempts.set(0);
            retryRequestInterval.set(1);
        } catch (Exception e) {
            handleRequestException(e);
        }
    }

    protected int getStartBlockHeight() {
        return LAUNCH_BLOCK_HEIGHT;
    }

    protected abstract List<T> doRequest(int startBlockHeight);

    protected abstract void handleResponse(T data);

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
        if (shutdownCalled) {
            return;
        }

        log.error("Error at StreamObserver. We call subscribe again after {} sec. Error message: {}", subscribeRetryInterval.get(), throwable.getMessage());
        Delay.run(() -> {
                    if (!shutdownCalled) {
                        subscribe();
                    }
                })
                .withExecutor(commonForkJoinPool())
                .after(subscribeRetryInterval.get(), TimeUnit.SECONDS);
        subscribeRetryInterval.set(Math.min(10, subscribeRetryInterval.incrementAndGet()));
    }
}