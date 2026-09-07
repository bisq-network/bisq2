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

package bisq.api.rest_api.endpoints.offers;

import bisq.api.dto.presentation.offerbook.OfferItemPresentationDto;
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDtoFactory;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.UserService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class OfferbookRestApiTest {
    private final Market market = MarketRepository.getUSDBitcoinMarket();

    @Test
    void oneOverflowingOfferDoesNotAbortTheWholeMarketListing() {
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        BisqEasyOfferbookChannelService channelService = chatService.getBisqEasyOfferbookChannelService();
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(channelService.findChannel(market)).thenReturn(Optional.of(channel));

        BisqEasyOfferbookMessage validMessage = offerMessage();
        BisqEasyOfferbookMessage overflowingMessage = offerMessage();
        ObservableSet<BisqEasyOfferbookMessage> messages = new ObservableSet<>();
        messages.add(validMessage);
        messages.add(overflowingMessage);
        when(channel.getChatMessages()).thenReturn(messages);

        OfferbookRestApi api = new OfferbookRestApi(chatService,
                mock(MarketPriceService.class),
                mock(UserService.class, RETURNS_DEEP_STUBS));

        OfferItemPresentationDto validDto = mock(OfferItemPresentationDto.class);
        try (MockedStatic<OfferItemPresentationDtoFactory> factory = mockStatic(OfferItemPresentationDtoFactory.class)) {
            factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), eq(validMessage)))
                    .thenReturn(Optional.of(validDto));
            // The overflowing offer throws in the factory the way an amount conversion does,
            // instead of aborting the whole endpoint stream with a 500.
            factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), eq(overflowingMessage)))
                    .thenThrow(new ArithmeticException("overflow"));

            Response response = api.getOffers("USD");

            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            @SuppressWarnings("unchecked")
            List<OfferItemPresentationDto> entity = (List<OfferItemPresentationDto>) response.getEntity();
            assertThat(entity).containsExactly(validDto);
        }
    }

    private BisqEasyOfferbookMessage offerMessage() {
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.hasBisqEasyOffer()).thenReturn(true);
        return message;
    }
}
