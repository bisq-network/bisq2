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

package bisq.chat.priv;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static bisq.chat.priv.PendingTradeChatMessages.MAX_PENDING_MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Rules 2 and 4 of docs/specifications/trade/pending-trade-messages.md.
 */
class PendingTradeChatMessagesTest {
    private final PendingTradeChatMessages<MuSigOpenTradeMessage> pendingMessages =
            new PendingTradeChatMessages<>(MuSigOpenTradeMessage::getTradeId);

    @Test
    void oldestMessageIsDroppedWhenTheBoundIsReached() {
        List<MuSigOpenTradeMessage> messages = IntStream.rangeClosed(0, MAX_PENDING_MESSAGES)
                .mapToObj(i -> message("tradeId" + i))
                .toList();

        messages.forEach(pendingMessages::add);

        assertEquals(messages.subList(1, messages.size()), pendingMessages.getMessagesInArrivalOrder());
    }

    @Test
    void messageWhichIsAlreadyPendingKeepsItsPlace() {
        MuSigOpenTradeMessage first = message("tradeId1");
        MuSigOpenTradeMessage second = message("tradeId2");

        pendingMessages.add(first);
        pendingMessages.add(second);
        pendingMessages.add(first);

        assertEquals(List.of(first, second), pendingMessages.getMessagesInArrivalOrder());
    }

    @Test
    void messagesOfATradeAreSelectedByTradeId() {
        MuSigOpenTradeMessage first = message("tradeId1");
        MuSigOpenTradeMessage other = message("tradeId2");
        MuSigOpenTradeMessage second = message("tradeId1");

        pendingMessages.add(first);
        pendingMessages.add(other);
        pendingMessages.add(second);

        assertEquals(List.of(first, second), pendingMessages.getMessagesOfTrade("tradeId1"));
    }

    private static MuSigOpenTradeMessage message(String tradeId) {
        MuSigOpenTradeMessage message = mock(MuSigOpenTradeMessage.class);
        when(message.getTradeId()).thenReturn(tradeId);
        return message;
    }
}
