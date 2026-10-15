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
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatReceiptMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.protocol.messages.PendingTradeMessages;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static bisq.trade.protocol.messages.PendingTradeMessages.MAX_PENDING_MESSAGES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Trade messages carry private data of the traders, for example the payment account data of the seller. Users share
 * their log files with support, so the log lines about pending trade messages name only the type and the trade ID of
 * a message.
 */
@Isolated("Captures the log of BisqEasyTradeService and PendingTradeMessages")
class BisqEasyTradeServiceLoggingTest {
    private static final String PAYMENT_ACCOUNT_DATA = "private-sentinel-iban";

    private final Logger serviceLogger = (Logger) LoggerFactory.getLogger(BisqEasyTradeService.class);
    private final Logger pendingMessagesLogger = (Logger) LoggerFactory.getLogger(PendingTradeMessages.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private final NetworkId me = networkId("me", 1001);
    private final NetworkId peer = networkId("peer", 1002);
    private final NetworkId thirdParty = networkId("third-party", 1003);

    private BisqEasyTradeService service;
    private BisqEasyTrade trade;

    @BeforeEach
    void setUp() {
        appender.start();
        serviceLogger.addAppender(appender);
        pendingMessagesLogger.addAppender(appender);

        service = new BisqEasyTradeService(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), AppType.DESKTOP);
        // I am the taker and the buyer. The peer is the maker and the seller.
        BisqEasyContract contract = mock(BisqEasyContract.class);
        when(contract.getTakeOfferDate()).thenReturn(0L);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offerId");
        trade = new BisqEasyTrade(contract, true, true, mock(Identity.class), offer, me, peer);
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
        serviceLogger.detachAppender(appender);
        pendingMessagesLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logOfAKeptAndLaterProcessedMessageNamesOnlyTypeAndTradeId() {
        // The account data message arrives before the trade exists, and it is processed when the next message of
        // the peer arrives
        service.onMessage(accountDataMessage());
        startServiceWithTrade();
        service.onMessage(new BisqEasyConfirmFiatReceiptMessage("message-of-peer", trade.getId(), "1.0.0", peer, me));
        assertThat(trade.getEventQueue()).contains(accountDataMessage());

        assertThat(loggedLines()).anyMatch(line -> line.contains("add the message of type BisqEasyAccountDataMessage"));
        assertThat(loggedLines()).anyMatch(line -> line.contains("remove the message of type BisqEasyAccountDataMessage"));
        assertThat(loggedLines()).noneMatch(line -> line.contains(PAYMENT_ACCOUNT_DATA));
    }

    @Test
    void logOfADroppedMessageNamesOnlyTypeAndTradeId() {
        service.onMessage(accountDataMessage());
        IntStream.range(0, MAX_PENDING_MESSAGES).forEach(index ->
                service.onMessage(new BisqEasyReportErrorMessage("message-" + index,
                        "unknown-trade-" + index,
                        "1.0.0",
                        thirdParty,
                        me,
                        "error",
                        "",
                        TradeProtocolFailure.UNKNOWN)));

        List<ILoggingEvent> warnings = appender.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getFormattedMessage())
                .contains("type=BisqEasyAccountDataMessage")
                .contains("tradeId=" + trade.getId());
        assertThat(loggedLines()).noneMatch(line -> line.contains(PAYMENT_ACCOUNT_DATA));
    }

    private List<String> loggedLines() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private BisqEasyAccountDataMessage accountDataMessage() {
        return new BisqEasyAccountDataMessage("message-of-peer-with-account-data",
                trade.getId(),
                "1.0.0",
                peer,
                me,
                PAYMENT_ACCOUNT_DATA,
                trade.getOffer());
    }

    private void startServiceWithTrade() {
        service.getPersistableStore().addTrade(trade);
        service.initialize().join();
        assertThat(service.findProtocol(trade.getId())).isPresent();
        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
    }

    private static NetworkId networkId(String keyId, int port) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }
}
