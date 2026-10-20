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

import bisq.bonded_roles.release.AppType;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmBtcSentMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatReceiptMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static bisq.trade.protocol.messages.PendingTradeMessages.MAX_PENDING_MESSAGES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A trade message which arrives before its trade exists is kept and processed when the next message of the trade is
 * processed. The kept messages are bounded, because anybody can send trade messages with made-up trade IDs. When the
 * bound is exceeded, the oldest kept message is dropped.
 */
class BisqEasyTradeServicePendingMessagesTest {
    private final NetworkId me = networkId("me", 1001);
    private final NetworkId peer = networkId("peer", 1002);
    private final NetworkId thirdParty = networkId("third-party", 1003);
    private final BisqEasyProtocolRegistry protocolRegistry = new BisqEasyProtocolRegistry();

    private BisqEasyTradeService service;
    private BisqEasyTrade trade;

    @BeforeEach
    void setUp() {
        service = new BisqEasyTradeService(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), AppType.DESKTOP,
                protocolRegistry);
        // I am the taker and the buyer. The peer is the maker.
        BisqEasyContract contract = mock(BisqEasyContract.class);
        when(contract.getTakeOfferDate()).thenReturn(0L);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offerId");
        trade = new BisqEasyTrade(contract, true, true, mock(Identity.class), offer, me, peer);
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @Test
    void messageOfTheTradePeerWhichArrivedBeforeTheTradeExistedIsProcessedLater() {
        // The error report of the peer arrives before the trade exists. It is the newest of the kept messages.
        sendMessagesWithUnknownTradeIds(MAX_PENDING_MESSAGES);
        service.onMessage(errorReport(peer));
        startServiceWithTrade();

        // The next message of the peer makes the service process the kept messages again
        service.onMessage(confirmFiatReceipt());

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.FAILED_AT_PEER);
    }

    @Test
    void oldestPendingMessageIsDroppedWhenTheBoundIsExceeded() {
        // The error report of the peer arrives before the trade exists. It is the oldest of the kept messages.
        service.onMessage(errorReport(peer));
        sendMessagesWithUnknownTradeIds(MAX_PENDING_MESSAGES);
        startServiceWithTrade();

        service.onMessage(confirmFiatReceipt());

        // The error report was dropped, so it did not reach the protocol. The trade has no transition for the
        // confirmation message in its first state, so that message does not change the state either.
        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
        assertThat(trade.getEventQueue()).doesNotContain(errorReport(peer));
    }

    // Each pending message reaches the protocol once. Two messages which the peer sent before the trade existed are
    // pending. A later message of the trade makes the node pass them to the protocol.
    @Test
    void eachPendingMessageOfTheTradeReachesTheProtocolOnce() {
        BisqEasyAccountDataMessage accountData = new BisqEasyAccountDataMessage("account-data-of-peer",
                trade.getId(), "1.0.0", peer, me, "account data", trade.getOffer());
        BisqEasyConfirmBtcSentMessage btcSent = new BisqEasyConfirmBtcSentMessage("btc-sent-of-peer",
                trade.getId(), "1.0.0", peer, me, Optional.empty());
        service.onMessage(accountData);
        service.onMessage(btcSent);
        startServiceWithTrade();
        BisqEasyProtocol protocol = spy(service.findProtocol(trade.getId()).orElseThrow());
        protocolRegistry.add(trade.getId(), protocol);

        service.onMessage(confirmFiatReceipt());

        verify(protocol).handle(accountData);
        verify(protocol).handle(btcSent);
        assertThat(protocolRegistry.hasPendingMessages()).isFalse();
    }

    private void sendMessagesWithUnknownTradeIds(int count) {
        IntStream.range(0, count).forEach(index ->
                service.onMessage(new BisqEasyReportErrorMessage("message-" + index,
                        "unknown-trade-" + index,
                        "1.0.0",
                        thirdParty,
                        me,
                        "error",
                        "",
                        TradeProtocolFailure.UNKNOWN)));
    }

    private void startServiceWithTrade() {
        service.getPersistableStore().addTrade(trade);
        service.initialize().join();
        assertThat(service.findProtocol(trade.getId())).isPresent();
        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
    }

    private BisqEasyConfirmFiatReceiptMessage confirmFiatReceipt() {
        return new BisqEasyConfirmFiatReceiptMessage("message-of-peer", trade.getId(), "1.0.0", peer, me);
    }

    private BisqEasyReportErrorMessage errorReport(NetworkId sender) {
        return new BisqEasyReportErrorMessage("message-of-" + sender.getPubKey().getKeyId(),
                trade.getId(),
                "1.0.0",
                sender,
                me,
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
