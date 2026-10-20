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

package bisq.trade.bisq_easy;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.protocol.messages.PendingTradeMessages;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Rule 1 of docs/specifications/trade/pending-trade-messages.md: the decision "no protocol, so the message is pending"
 * and the registration of a protocol are one step.
 * <p>
 * BisqEasyTradeServicePendingMessageHandoffTest shows that the maker loses no message. A registry which keeps the
 * message as pending only after it released its lock passes that test as well. So this test stops the message thread
 * inside the add of the pending message, and checks that the registration of the protocol waits for it.
 */
class BisqEasyProtocolRegistryTest {
    private static final String TRADE_ID = "trade-1";

    private final NetworkId sender = networkId("sender", 1001);
    private final NetworkId receiver = networkId("receiver", 1002);
    private final BisqEasyProtocol protocol = mock(BisqEasyProtocol.class);

    @Test
    void messageWhoseTradeHasNoProtocolIsPending() {
        BisqEasyProtocolRegistry registry = new BisqEasyProtocolRegistry();
        BisqEasyTradeMessage message = message();

        assertThat(registry.findOrKeepAsPending(message)).isEmpty();

        assertThat(registry.getPendingMessagesInArrivalOrder()).containsExactly(message);
    }

    @Test
    void messageWhoseTradeHasAProtocolIsNotPending() {
        BisqEasyProtocolRegistry registry = new BisqEasyProtocolRegistry();
        registry.add(TRADE_ID, protocol);

        assertThat(registry.findOrKeepAsPending(message())).contains(protocol);

        assertThat(registry.hasPendingMessages()).isFalse();
    }

    @Test
    void protocolIsAddedOnlyAfterAMessageWhichFoundNoProtocolIsPending() throws Exception {
        PendingTradeMessages<BisqEasyTradeMessage> pendingMessages = spy(new PendingTradeMessages<>());
        BisqEasyProtocolRegistry registry = new BisqEasyProtocolRegistry(pendingMessages);
        BisqEasyTradeMessage message = message();
        CountDownLatch addingAsPending = new CountDownLatch(1);
        CountDownLatch continueMessageThread = new CountDownLatch(1);
        doAnswer(invocation -> {
            addingAsPending.countDown();
            assertThat(continueMessageThread.await(10, SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(pendingMessages).add(message);

        TestThread messageThread = new TestThread("message", () -> registry.findOrKeepAsPending(message));
        assertThat(addingAsPending.await(10, SECONDS)).isTrue();
        TestThread registrationThread = new TestThread("registration", () -> registry.add(TRADE_ID, protocol));
        try {
            awaitWaitingFor(registrationThread.thread, messageThread.thread);
            assertThat(registry.find(TRADE_ID)).isEmpty();
        } finally {
            continueMessageThread.countDown();
        }

        messageThread.join();
        registrationThread.join();
        assertThat(registry.getPendingMessagesInArrivalOrder()).containsExactly(message);
        assertThat(registry.find(TRADE_ID)).contains(protocol);
    }

    // Returns when the thread waits for a monitor or lock which the owner thread holds. Fails when the thread finishes
    // first, because then it did not wait for the owner.
    private static void awaitWaitingFor(Thread thread, Thread owner) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(thread.threadId());
            if (info != null && info.getLockOwnerId() == owner.threadId()) {
                return;
            }
            assertThat(thread.isAlive())
                    .as(thread.getName() + " finished without waiting for " + owner.getName())
                    .isTrue();
            Thread.sleep(5);
        }
        throw new AssertionError(thread.getName() + " did not wait for " + owner.getName());
    }

    private BisqEasyTradeMessage message() {
        return new BisqEasyReportErrorMessage("message-1",
                TRADE_ID,
                "1.0.0",
                sender,
                receiver,
                "error",
                "",
                TradeProtocolFailure.UNKNOWN);
    }

    private static NetworkId networkId(String keyId, int port) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }

    // A started thread whose failure the test reports
    private static final class TestThread {
        private final Thread thread;
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private TestThread(String name, Runnable task) {
            thread = new Thread(() -> {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }
            }, name);
            thread.start();
        }

        private void join() throws InterruptedException {
            thread.join(SECONDS.toMillis(10));
            assertThat(thread.isAlive()).as(thread.getName() + " has finished").isFalse();
            assertThat(failure.get()).as(thread.getName() + " has not failed").isNull();
        }
    }
}
