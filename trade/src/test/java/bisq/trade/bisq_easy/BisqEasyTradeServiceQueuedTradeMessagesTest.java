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
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A trade message which the protocol of a trade cannot process yet waits in the event queue of the trade. The node
 * removed the message from the mailbox, so the trade must keep it over a restart. At startup the trade service applies
 * the queued messages of the stored trades, with the trade restrictions of the stored alerts
 * (docs/specifications/trade/queued-trade-messages.md).
 * <p>
 * I am the seller and the maker. I have sent my payment account data. The buyer's confirmation of the fiat payment
 * arrives before the buyer's Bitcoin address.
 */
class BisqEasyTradeServiceQueuedTradeMessagesTest {
    private static final String BTC_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";

    private final List<BisqEasyTradeService> services = new ArrayList<>();
    private ObservableSet<AuthorizedAlertData> alerts;
    private NetworkId makerNetworkId;
    private NetworkId takerNetworkId;
    private BisqEasyOffer offer;
    private BisqEasyTrade trade;

    @BeforeEach
    void setUp() {
        alerts = new ObservableSet<>();
        Identity makerIdentity = identity("maker");
        makerNetworkId = makerIdentity.getNetworkId();
        takerNetworkId = identity("taker").getNetworkId();
        offer = offer("offer-id", makerNetworkId, QUOTE_AMOUNT);
        trade = inState(sellerAsMakerTrade(contract(offer, takerNetworkId, Optional.empty()), makerIdentity),
                MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
    }

    @AfterEach
    void tearDown() {
        services.forEach(service -> service.shutdown().join());
    }

    @Test
    void queuedTradeMessageIsProcessedAfterARestart() throws UnresolvableProtobufException {
        BisqEasyTradeService service = startServiceWith(trade);
        BisqEasyConfirmFiatSentMessage confirmFiatSent = confirmFiatSent();
        service.onMessage(confirmFiatSent);
        assertEquals(MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                trade.getState());
        assertEquals(Set.of(confirmFiatSent), trade.getEventQueue());

        // The node restarts. It loads the trade from the store, and the mailbox does not deliver the message again.
        service.shutdown().join();
        BisqEasyTrade storedTrade = BisqEasyTrade.fromProto(trade.toProto(false));
        BisqEasyTradeService restartedService = startServiceWith(storedTrade);
        restartedService.onMessage(btcAddress());

        assertEquals(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, storedTrade.getState());
        assertTrue(storedTrade.getEventQueue().isEmpty());
    }

    @Test
    void queuedTradeMessageWithTransitionFromTheStoredStateIsProcessedAtStartup() throws UnresolvableProtobufException {
        BisqEasyTrade storedTrade = storedTradeWithQueuedConfirmFiatSent();

        startServiceWith(storedTrade);

        assertEquals(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, storedTrade.getState());
        assertTrue(storedTrade.getEventQueue().isEmpty());
    }

    @Test
    void queuedTradeMessageIsNotProcessedAtStartupDuringAStoredHaltOfTrading() throws UnresolvableProtobufException {
        BisqEasyTrade storedTrade = storedTradeWithQueuedConfirmFiatSent();
        alerts.add(haltOfTrading());

        startServiceWith(storedTrade);

        assertEquals(MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                storedTrade.getState());
        assertEquals(1, storedTrade.getEventQueue().size());
    }

    @Test
    void queuedTradeMessageIsNotProcessedAtStartupBelowAStoredMinimumVersion() throws UnresolvableProtobufException {
        BisqEasyTrade storedTrade = storedTradeWithQueuedConfirmFiatSent();
        alerts.add(minimumVersionForTrading());

        startServiceWith(storedTrade);

        assertEquals(MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                storedTrade.getState());
        assertEquals(1, storedTrade.getEventQueue().size());
    }

    // When the restriction ends, the trade service applies the queued events, as at a start without the restriction
    @Test
    void queuedTradeMessageIsProcessedWhenAStoredHaltOfTradingEnds() throws UnresolvableProtobufException {
        BisqEasyTrade storedTrade = storedTradeWithQueuedConfirmFiatSent();
        AuthorizedAlertData haltOfTrading = haltOfTrading();
        alerts.add(haltOfTrading);
        startServiceWith(storedTrade);

        alerts.remove(haltOfTrading);

        assertEquals(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, storedTrade.getState());
        assertTrue(storedTrade.getEventQueue().isEmpty());
    }

    @Test
    void queuedTradeMessageIsProcessedWhenAStoredMinimumVersionEnds() throws UnresolvableProtobufException {
        BisqEasyTrade storedTrade = storedTradeWithQueuedConfirmFiatSent();
        AuthorizedAlertData minimumVersionForTrading = minimumVersionForTrading();
        alerts.add(minimumVersionForTrading);
        startServiceWith(storedTrade);

        alerts.remove(minimumVersionForTrading);

        assertEquals(SELLER_RECEIVED_FIAT_SENT_CONFIRMATION, storedTrade.getState());
        assertTrue(storedTrade.getEventQueue().isEmpty());
    }

    @Test
    void queuedTradeMessageIsNotProcessedWhileAnotherRestrictionRemains() throws UnresolvableProtobufException {
        BisqEasyTrade storedTrade = storedTradeWithQueuedConfirmFiatSent();
        AuthorizedAlertData haltOfTrading = haltOfTrading();
        alerts.add(haltOfTrading);
        alerts.add(minimumVersionForTrading());
        startServiceWith(storedTrade);

        alerts.remove(haltOfTrading);

        assertEquals(MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                storedTrade.getState());
        assertEquals(1, storedTrade.getEventQueue().size());
    }

    // A store whose write contains the state after the Bitcoin address, but still the queued confirmation which the
    // protocol processes in the next step
    private BisqEasyTrade storedTradeWithQueuedConfirmFiatSent() throws UnresolvableProtobufException {
        bisq.trade.protobuf.Trade proto = trade.toProto(false);
        return BisqEasyTrade.fromProto(proto.toBuilder()
                .setState(MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS.name())
                .setBisqEasyTrade(proto.getBisqEasyTrade().toBuilder().setBitcoinPaymentData(BTC_ADDRESS))
                .addQueuedTradeMessages(confirmFiatSent().toValueProto(false))
                .build());
    }

    private BisqEasyTradeService startServiceWith(BisqEasyTrade storedTrade) {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getBondedRolesService().getAlertService().getAuthorizedAlertDataSet()).thenReturn(alerts);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService()).thenReturn(channelService);
        when(channelService.getChannels()).thenReturn(new ObservableSet<>());
        BisqEasyTradeService service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
        services.add(service);
        service.getPersistableStore().addTrade(storedTrade);
        service.initialize().join();
        return service;
    }

    private BisqEasyConfirmFiatSentMessage confirmFiatSent() {
        return new BisqEasyConfirmFiatSentMessage("confirm-fiat-sent",
                trade.getId(),
                BisqEasyProtocol.VERSION,
                takerNetworkId,
                makerNetworkId);
    }

    private BisqEasyBtcAddressMessage btcAddress() {
        return new BisqEasyBtcAddressMessage(StringUtils.createUid(),
                trade.getId(),
                BisqEasyProtocol.VERSION,
                takerNetworkId,
                makerNetworkId,
                BTC_ADDRESS,
                offer);
    }

    private static AuthorizedAlertData haltOfTrading() {
        return new AuthorizedAlertData("alertId",
                System.currentTimeMillis(),
                AlertType.EMERGENCY,
                Optional.empty(),
                Optional.of("Trading is halted"),
                true,
                false,
                Optional.empty(),
                Optional.empty(),
                "1".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }

    // The minimum version is above the version of every node, so the node of the test is restricted
    private static AuthorizedAlertData minimumVersionForTrading() {
        return new AuthorizedAlertData("alertId",
                System.currentTimeMillis(),
                AlertType.EMERGENCY,
                Optional.empty(),
                Optional.of("Update to trade"),
                false,
                true,
                Optional.of("99.0.0"),
                Optional.empty(),
                "1".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }
}
