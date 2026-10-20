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

package bisq.trade.protocol.messages;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static bisq.trade.protocol.messages.PendingTradeMessages.MAX_PENDING_MESSAGES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pending trade messages are bounded. Anybody can send trade messages with made-up trade IDs, and such a message
 * never gets a protocol. When the bound is reached, the oldest message is dropped.
 */
class PendingTradeMessagesTest {
    private final NetworkId sender = networkId("sender", 1001);
    private final NetworkId receiver = networkId("receiver", 1002);
    private final PendingTradeMessages<BisqEasyTradeMessage> pendingMessages = new PendingTradeMessages<>();

    @Test
    void messagesAreKeptInArrivalOrderUpToTheBound() {
        List<BisqEasyTradeMessage> messages = messages(MAX_PENDING_MESSAGES);

        messages.forEach(pendingMessages::add);

        assertThat(pendingMessages.getMessagesInArrivalOrder()).containsExactlyElementsOf(messages);
    }

    @Test
    void oldestMessageIsDroppedWhenTheBoundIsExceeded() {
        List<BisqEasyTradeMessage> messages = messages(MAX_PENDING_MESSAGES + 2);

        messages.forEach(pendingMessages::add);

        assertThat(pendingMessages.getMessagesInArrivalOrder())
                .hasSize(MAX_PENDING_MESSAGES)
                .containsExactlyElementsOf(messages.subList(2, messages.size()));
    }

    @Test
    void addingAKeptMessageAgainChangesNeitherTheOrderNorTheSize() {
        List<BisqEasyTradeMessage> messages = messages(MAX_PENDING_MESSAGES);
        messages.forEach(pendingMessages::add);

        pendingMessages.add(messages.get(0));

        assertThat(pendingMessages.getMessagesInArrivalOrder()).containsExactlyElementsOf(messages);
    }

    @Test
    void removingTheMessagesOfATradeReturnsThemOnceInArrivalOrder() {
        BisqEasyTradeMessage first = message(1, "trade");
        BisqEasyTradeMessage ofOtherTrade = message(2, "other-trade");
        BisqEasyTradeMessage second = message(3, "trade");
        List.of(first, ofOtherTrade, second).forEach(pendingMessages::add);

        assertThat(pendingMessages.removeMessagesOfTrade("trade")).containsExactly(first, second);
        assertThat(pendingMessages.removeMessagesOfTrade("trade")).isEmpty();
        assertThat(pendingMessages.getMessagesInArrivalOrder()).containsExactly(ofOtherTrade);
    }

    // Rule 1 of docs/specifications/trade/pending-trade-messages.md: two threads which process messages of one trade
    // can take its pending messages at the same time, and each pending message must reach the protocol once. The test
    // cannot fail for correct code. How often it detects a missing synchronization depends on the number of CPUs.
    @Test
    void twoThreadsWhichRemoveTheMessagesOfATradeNeverGetTheSameMessage() throws Exception {
        List<BisqEasyTradeMessage> messages = IntStream.range(0, 40)
                .mapToObj(index -> message(index, "trade"))
                .toList();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 20_000; round++) {
                messages.forEach(pendingMessages::add);
                Future<List<BisqEasyTradeMessage>> first = executor.submit(() -> removeMessagesOfTradeAfter(start));
                Future<List<BisqEasyTradeMessage>> second = executor.submit(() -> removeMessagesOfTradeAfter(start));
                List<BisqEasyTradeMessage> taken = new ArrayList<>(first.get(10, SECONDS));
                taken.addAll(second.get(10, SECONDS));

                assertThat(taken).as("messages taken in round %s", round).containsExactlyInAnyOrderElementsOf(messages);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(pendingMessages.isEmpty()).isTrue();
    }

    @Test
    void snapshotIsNotChangedByLaterModifications() {
        BisqEasyTradeMessage message = message(1);
        pendingMessages.add(message);
        List<BisqEasyTradeMessage> snapshot = pendingMessages.getMessagesInArrivalOrder();

        pendingMessages.clear();

        assertThat(snapshot).containsExactly(message);
        assertThat(pendingMessages.isEmpty()).isTrue();
    }

    private List<BisqEasyTradeMessage> removeMessagesOfTradeAfter(CyclicBarrier start) throws Exception {
        start.await(10, SECONDS);
        return pendingMessages.removeMessagesOfTrade("trade");
    }

    private List<BisqEasyTradeMessage> messages(int count) {
        return new ArrayList<>(IntStream.range(0, count).mapToObj(this::message).toList());
    }

    private BisqEasyTradeMessage message(int index) {
        return message(index, "trade-" + index);
    }

    private BisqEasyTradeMessage message(int index, String tradeId) {
        return new BisqEasyReportErrorMessage("message-" + index,
                tradeId,
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
}
