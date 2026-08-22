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

package bisq.api.web_socket.domain.offers;

import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasyService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.UserService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NumOffersWebSocketServiceTest {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();

    @Test
    void numOffersFollowsOfferValidityChangesWithoutChatMessageEvents() {
        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(MARKET);
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.getId()).thenReturn("offer-message-id");
        when(message.getAuthorUserProfileId()).thenReturn("author-id");
        when(message.hasBisqEasyOffer()).thenReturn(true);
        channel.addChatMessage(message);
        ObservableSet<BisqEasyOfferbookChannel> channels = new ObservableSet<>();
        channels.add(channel);
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        when(chatService.getBisqEasyOfferbookChannelService().getChannels()).thenReturn(channels);

        Observable<Long> offerValidityRevision = new Observable<>(0L);
        boolean[] offerIsValid = {false};
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
        when(messageService.getOfferValidityRevision()).thenReturn(offerValidityRevision);
        when(messageService.getOffers(any(BisqEasyOfferbookChannel.class)))
                .thenAnswer(invocation -> offerIsValid[0] ? Stream.of(offer) : Stream.empty());
        BisqEasyService bisqEasyService = mock(BisqEasyService.class);
        when(bisqEasyService.getBisqEasyOfferbookMessageService()).thenReturn(messageService);

        NumOffersWebSocketService service = new NumOffersWebSocketService(mock(SubscriberRepository.class),
                chatService,
                mock(UserService.class),
                bisqEasyService);
        service.initialize().join();
        assertEquals(Map.of("USD", 0), service.getObservable());
        int[] numNotifications = {0};
        service.getObservable().addObserver(() -> numNotifications[0]++);
        int notificationsAtRegistration = numNotifications[0];

        offerIsValid[0] = true;
        offerValidityRevision.set(1L);
        assertEquals(Map.of("USD", 1), service.getObservable());
        assertEquals(notificationsAtRegistration + 1, numNotifications[0]);

        offerValidityRevision.set(2L);
        assertEquals(Map.of("USD", 1), service.getObservable());
        assertEquals(notificationsAtRegistration + 1, numNotifications[0]);

        offerIsValid[0] = false;
        offerValidityRevision.set(3L);
        assertEquals(Map.of("USD", 0), service.getObservable());
        assertEquals(notificationsAtRegistration + 2, numNotifications[0]);

        service.shutdown().join();
        offerIsValid[0] = true;
        offerValidityRevision.set(4L);
        assertEquals(Map.of("USD", 0), service.getObservable());
    }
}
