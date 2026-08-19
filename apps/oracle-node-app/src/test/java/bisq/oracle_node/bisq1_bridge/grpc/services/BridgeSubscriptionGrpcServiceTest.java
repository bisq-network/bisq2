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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

    private static final class TestService extends BridgeSubscriptionGrpcService<Integer> {
        private final List<String> calls = new ArrayList<>();
        private final AtomicInteger subscribeCount = new AtomicInteger();
        private final AtomicInteger requestCount = new AtomicInteger();
        private final CountDownLatch secondRequest = new CountDownLatch(1);

        private TestService() {
            super(false, mock(GrpcClient.class));
        }

        @Override
        protected List<Integer> doRequest(int startBlockHeight) {
            calls.add("request");
            if (requestCount.incrementAndGet() == 2) {
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
        protected void subscribe() {
            calls.add("subscribe");
            subscribeCount.incrementAndGet();
        }
    }
}
