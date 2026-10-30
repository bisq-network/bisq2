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

import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyBuyerAsTakerProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmFiatSentEvent;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatReceiptMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;

/**
 * The state machine of a trade queues a trade message which arrives before the trade can process it. The node does not
 * receive such a message again, so the trade keeps its queued trade messages when it is persisted. The trade also keeps
 * the types of the events which changed its state, so the protocol does not queue a copy of a processed message after
 * a restart (docs/specifications/trade/queued-trade-messages.md).
 */
class BisqEasyTradeQueuedTradeMessagesTest {
    private NetworkId makerNetworkId;
    private NetworkId takerNetworkId;
    private BisqEasyTrade trade;
    private BisqEasyProtocol protocol;

    @BeforeEach
    void setUp() {
        Identity makerIdentity = identity("maker");
        makerNetworkId = makerIdentity.getNetworkId();
        Identity takerIdentity = identity("taker");
        takerNetworkId = takerIdentity.getNetworkId();
        BisqEasyOffer offer = offer("offer-id", makerNetworkId, QUOTE_AMOUNT);
        // I am the taker and the buyer. The peer is the maker.
        trade = buyerAsTakerTrade(contract(offer, takerNetworkId, Optional.empty()), takerIdentity);
        protocol = new BisqEasyBuyerAsTakerProtocol(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), trade);
    }

    @Test
    void queuedTradeMessageIsRestoredWithTheTrade() throws UnresolvableProtobufException {
        // The buyer has a transition for the confirmation of the fiat receipt only after it sent the fiat
        BisqEasyConfirmFiatReceiptMessage message = confirmFiatReceipt();
        protocol.handle(message);
        assertEquals(BisqEasyTradeState.INIT, trade.getState());
        assertEquals(Set.of(message), trade.getEventQueue());

        BisqEasyTrade restored = BisqEasyTrade.fromProto(trade.toProto(false));

        assertEquals(Set.of(message), restored.getEventQueue());
    }

    @Test
    void queuedLocalEventIsNotPersisted() throws UnresolvableProtobufException {
        protocol.handle(new BisqEasyConfirmFiatSentEvent());
        assertEquals(BisqEasyTradeState.INIT, trade.getState());
        assertEquals(1, trade.getEventQueue().size());

        BisqEasyTrade restored = BisqEasyTrade.fromProto(trade.toProto(false));

        assertTrue(restored.getEventQueue().isEmpty());
    }

    @Test
    void queuedTradeMessageWhichCannotBeParsedIsSkipped() throws UnresolvableProtobufException {
        BisqEasyConfirmFiatReceiptMessage message = confirmFiatReceipt();
        protocol.handle(message);
        // A message ID which breaks the length limit stands for a rule which a later version adds
        bisq.trade.protobuf.TradeMessage unparsableMessage = message.toValueProto(false).toBuilder()
                .setId("a".repeat(NetworkDataValidation.MAX_ID_LENGTH + 1))
                .build();
        bisq.trade.protobuf.Trade proto = trade.toProto(false).toBuilder()
                .addQueuedTradeMessages(unparsableMessage)
                .build();

        BisqEasyTrade restored = BisqEasyTrade.fromProto(proto);

        assertEquals(trade.getId(), restored.getId());
        assertEquals(Set.of(message), restored.getEventQueue());
    }

    // The mailbox can deliver a second copy of a message after a restart. The trade has processed the type, so the
    // protocol does not queue the copy, also after the restart.
    @Test
    void copyOfAProcessedMessageIsNotQueuedAfterARestart() throws UnresolvableProtobufException {
        BisqEasyTrade paidTrade = inState(trade, BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION);
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        new BisqEasyBuyerAsTakerProtocol(serviceProvider, paidTrade).handle(confirmFiatReceipt());
        assertEquals(BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION, paidTrade.getState());

        BisqEasyTrade restored = BisqEasyTrade.fromProto(paidTrade.toProto(false));
        new BisqEasyBuyerAsTakerProtocol(serviceProvider, restored).handle(confirmFiatReceipt());

        assertEquals(BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION, restored.getState());
        assertTrue(restored.getEventQueue().isEmpty());
    }

    // The state machine sets the new state before it records the type of the event as processed. A write which reads
    // the processed event types first therefore stores a type only together with the state which its event produced.
    // In the other order, a write could store a type without its state, and after a restart the protocol could drop a
    // copy of the message which the trade still needs.
    @Test
    void writeReadsTheProcessedEventTypesBeforeTheState() {
        BisqEasyTrade spiedTrade = spy(trade);

        spiedTrade.toProto(false);

        List<String> reads = mockingDetails(spiedTrade).getInvocations().stream()
                .map(invocation -> invocation.getMethod().getName())
                .filter(name -> name.equals("getProcessedEventTypes") || name.equals("getState"))
                .toList();
        assertTrue(reads.contains("getState"));
        assertEquals("getProcessedEventTypes", reads.get(0));
    }

    private BisqEasyConfirmFiatReceiptMessage confirmFiatReceipt() {
        return new BisqEasyConfirmFiatReceiptMessage(StringUtils.createUid(),
                trade.getId(),
                BisqEasyProtocol.VERSION,
                makerNetworkId,
                takerNetworkId);
    }
}
