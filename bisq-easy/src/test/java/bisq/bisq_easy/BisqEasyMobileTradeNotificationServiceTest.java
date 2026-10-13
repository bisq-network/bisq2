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

package bisq.bisq_easy;

import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.notifications.Notification;
import bisq.notifications.NotificationService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyMobileTradeNotificationServiceTest {
    private final ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();
    private final Observable<BisqEasyTradeState> state = new Observable<>(BisqEasyTradeState.INIT);
    private final BisqEasyTrade trade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);
    private final List<Notification> notifications = new ArrayList<>();
    private BisqEasyMobileTradeNotificationService service;

    @BeforeEach
    void setUp() {
        Res.setAndApplyLanguageTag("en");
        BisqEasyTradeService tradeService = mock(BisqEasyTradeService.class);
        when(tradeService.getTrades()).thenReturn(trades);
        when(trade.getId()).thenReturn("trade123456");
        when(trade.tradeStateObservable()).thenReturn(state);
        when(trade.isMaker()).thenReturn(true);
        when(trade.getPeer().getNetworkId().getId()).thenReturn("peer");
        NotificationService notificationService = mock(NotificationService.class);
        doAnswer(invocation -> {
            notifications.add(invocation.getArgument(0));
            return null;
        }).when(notificationService).dispatchMobileOnlyNotification(any());
        service = new BisqEasyMobileTradeNotificationService(tradeService, mock(UserProfileService.class),
                notificationService);
        service.initialize().join();
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_"})
    void makerReceivesOfferTakenNotificationAfterAcceptingPeerRequest(BisqEasyTradeState acceptedRequestState) {
        trades.add(trade);

        state.set(acceptedRequestState);

        assertEquals(1, notifications.size());
        assertEquals(1, offerTakenNotifications());
        assertTrue(notifications.stream().allMatch(notification ->
                notification.getTradeId().orElseThrow().equals(trade.getId())));

        state.set(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA);
        assertEquals(1, offerTakenNotifications());
    }

    @Test
    void buyerMakerWithEarlyAccountDataReceivesOnlyTheOfferTakenNotification() {
        state.set(BisqEasyTradeState.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA);
        trades.add(trade);

        state.set(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_);

        assertEquals(1, notifications.size());
        assertEquals(1, offerTakenNotifications());

        state.set(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA);

        assertEquals(1, notifications.size());
    }

    @Test
    void takersOwnRequestDoesNotNotifyTheTaker() {
        when(trade.isMaker()).thenReturn(false);
        trades.add(trade);

        state.set(BisqEasyTradeState.TAKER_SENT_TAKE_OFFER_REQUEST);

        assertTrue(notifications.isEmpty());
    }

    @Test
    void loadingAnAcceptedTradeDoesNotRepeatTheNotification() {
        state.set(BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA);

        trades.add(trade);

        assertTrue(notifications.isEmpty());
    }

    private long offerTakenNotifications() {
        String title = Res.get("bisqEasy.mobileNotifications.offerTaken.title", "trade123");
        return notifications.stream().filter(notification -> notification.getTitle().equals(title)).count();
    }
}
