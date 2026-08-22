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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BridgeSubscriptionGrpcServiceTest {
    @Test
    void initializationSubscribesBeforeRequestingTheSnapshot() {
        TestService service = new TestService();

        service.initialize();

        assertThat(service.calls).containsExactly("subscribe", "request", "response", "snapshot-complete");
    }

    @Test
    void normalStreamCompletionResubscribesAndRequestsCatchUpOnce() throws InterruptedException {
        TestService service = new TestService();
        service.initialize();

        service.handleStreamObserverCompleted();
        service.handleStreamObserverCompleted();

        assertThat(service.secondRequest.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(service.subscribeCount).hasValue(2);
        assertThat(service.requestCount).hasValue(2);
        service.shutdown();
    }

    @Test
    void failedHistoricalRequestStillFinishesItsLifecycle() {
        TestService service = new TestService(new StatusRuntimeException(Status.UNIMPLEMENTED));

        service.initialize();

        assertThat(service.finishedRequestCount).hasValue(1);
    }

    @Test
    void concurrentRequestsAreCoalescedWithoutLosingThePendingRequest() throws InterruptedException {
        TestService service = new TestService(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.execute(service::request);
            assertThat(service.firstRequestStarted.await(1, TimeUnit.SECONDS)).isTrue();

            service.request();
            service.releaseFirstRequest.countDown();

            assertThat(service.secondRequest.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(service.requestCount).hasValue(2);
        } finally {
            service.releaseFirstRequest.countDown();
            service.shutdown();
            executor.shutdownNow();
        }
    }

    @Test
    void requestQueuedImmediatelyAfterExecutionAcquisitionIsDispatchedAfterCompletion() throws Exception {
        TestService service = TestService.withBlockedRequestExecutionAcquisition();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.execute(service::request);
            assertThat(service.firstRequestExecutionAcquired.await(1, TimeUnit.SECONDS)).isTrue();

            Future<?> queuedRequest = executor.submit(service::request);
            queuedRequest.get(1, TimeUnit.SECONDS);
            service.releaseFirstRequestExecutionAcquisition.countDown();

            assertThat(service.secondRequest.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(service.requestCount).hasValue(2);
        } finally {
            service.releaseFirstRequestExecutionAcquisition.countDown();
            service.shutdown();
            executor.shutdownNow();
        }
    }

    @Test
    void unimplementedSubscriptionReportsVersionMismatchWithoutRetrying() throws InterruptedException {
        TestService service = new TestService();
        service.subscribeRetryInterval.set(0);

        service.handleStreamObserverError(new StatusRuntimeException(Status.UNIMPLEMENTED));

        assertThat(service.subscriptionAttempted.await(1, TimeUnit.SECONDS)).isFalse();
        assertThat(service.subscribeCount).hasValue(0);
    }

    private static final class TestService extends BridgeSubscriptionGrpcService<Integer> {
        private final List<String> calls = new ArrayList<>();
        private final AtomicInteger subscribeCount = new AtomicInteger();
        private final AtomicInteger requestCount = new AtomicInteger();
        private final AtomicInteger finishedRequestCount = new AtomicInteger();
        private final CountDownLatch firstRequestStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirstRequest = new CountDownLatch(1);
        private final CountDownLatch firstRequestExecutionAcquired = new CountDownLatch(1);
        private final CountDownLatch releaseFirstRequestExecutionAcquisition = new CountDownLatch(1);
        private final CountDownLatch secondRequest = new CountDownLatch(1);
        private final CountDownLatch subscriptionAttempted = new CountDownLatch(1);
        private final AtomicBoolean firstRequestExecution = new AtomicBoolean(true);
        private final RuntimeException requestFailure;
        private final boolean blockFirstRequest;
        private final boolean blockFirstRequestExecutionAcquisition;

        private TestService() {
            this(null, false, false);
        }

        private TestService(RuntimeException requestFailure) {
            this(requestFailure, false, false);
        }

        private TestService(boolean blockFirstRequest) {
            this(null, blockFirstRequest, false);
        }

        private TestService(RuntimeException requestFailure,
                            boolean blockFirstRequest,
                            boolean blockFirstRequestExecutionAcquisition) {
            super(false, mock(GrpcClient.class));
            this.requestFailure = requestFailure;
            this.blockFirstRequest = blockFirstRequest;
            this.blockFirstRequestExecutionAcquisition = blockFirstRequestExecutionAcquisition;
        }

        private static TestService withBlockedRequestExecutionAcquisition() {
            return new TestService(null, false, true);
        }

        @Override
        void onRequestExecutionAcquired() {
            if (blockFirstRequestExecutionAcquisition && firstRequestExecution.compareAndSet(true, false)) {
                firstRequestExecutionAcquired.countDown();
                try {
                    releaseFirstRequestExecutionAcquisition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        protected List<Integer> doRequest(int startBlockHeight) {
            calls.add("request");
            if (requestFailure != null) {
                throw requestFailure;
            }
            int currentRequestCount = requestCount.incrementAndGet();
            if (blockFirstRequest && currentRequestCount == 1) {
                firstRequestStarted.countDown();
                try {
                    releaseFirstRequest.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            if (currentRequestCount == 2) {
                secondRequest.countDown();
            }
            return List.of(1);
        }

        @Override
        protected void handleResponse(Integer ignored) {
            calls.add("response");
        }

        @Override
        protected void onHistoricalRequestComplete() {
            calls.add("snapshot-complete");
        }

        @Override
        protected void onHistoricalRequestFinished(boolean successful) {
            finishedRequestCount.incrementAndGet();
        }

        @Override
        protected void subscribe() {
            calls.add("subscribe");
            subscribeCount.incrementAndGet();
            subscriptionAttempted.countDown();
        }
    }
}
