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

import bisq.api.dto.common.currency.MarketDto;
import bisq.api.dto.offer.DirectionDto;
import bisq.api.dto.offer.amount.spec.QuoteSideFixedAmountSpecDto;
import bisq.api.dto.offer.price.spec.PriceSpecDto;
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDto;
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDtoFactory;
import bisq.api.dto.presentation.offerbook.SellerReputationVerdict;
import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.json.JsonMapperProvider;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
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
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(BisqEasySellersReputationBasedTradeAmountService.class));

        OfferItemPresentationDto validDto = mock(OfferItemPresentationDto.class);
        try (MockedStatic<OfferItemPresentationDtoFactory> factory = mockStatic(OfferItemPresentationDtoFactory.class)) {
            factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), anyBoolean(), eq(validMessage)))
                    .thenReturn(Optional.of(validDto));
            // The overflowing offer throws in the factory the way an amount conversion does,
            // instead of aborting the whole endpoint stream with a 500.
            factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), anyBoolean(), eq(overflowingMessage)))
                    .thenThrow(new ArithmeticException("overflow"));

            Response response = api.getOffers("USD");

            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            @SuppressWarnings("unchecked")
            List<OfferItemPresentationDto> entity = (List<OfferItemPresentationDto>) response.getEntity();
            assertThat(entity).containsExactly(validDto);
        }
    }

    // A failing check repeats on every request, so its warning carries no stack trace
    @Test
    void offerWhoseReputationCheckFailsIsSkippedWithAShortWarning() {
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(chatService.getBisqEasyOfferbookChannelService().findChannel(market)).thenReturn(Optional.of(channel));
        BisqEasyOfferbookMessage validMessage = offerMessage();
        BisqEasyOfferbookMessage failingMessage = offerMessage();
        ObservableSet<BisqEasyOfferbookMessage> messages = new ObservableSet<>();
        messages.add(validMessage);
        messages.add(failingMessage);
        when(channel.getChatMessages()).thenReturn(messages);
        BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
                mock(BisqEasySellersReputationBasedTradeAmountService.class);
        when(sellersReputationService.hasSellerSufficientReputation(validMessage.getBisqEasyOffer().orElseThrow()))
                .thenReturn(true);
        when(sellersReputationService.hasSellerSufficientReputation(failingMessage.getBisqEasyOffer().orElseThrow()))
                .thenThrow(new IllegalStateException("check failed"));
        OfferbookRestApi api = new OfferbookRestApi(chatService,
                mock(MarketPriceService.class),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                sellersReputationService);

        Logger logger = (Logger) LoggerFactory.getLogger(SellerReputationVerdict.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        OfferItemPresentationDto validDto = mock(OfferItemPresentationDto.class);
        try (MockedStatic<OfferItemPresentationDtoFactory> factory = mockStatic(OfferItemPresentationDtoFactory.class)) {
            factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), anyBoolean(), eq(validMessage)))
                    .thenReturn(Optional.of(validDto));

            Response response = api.getOffers("USD");

            @SuppressWarnings("unchecked")
            List<OfferItemPresentationDto> entity = (List<OfferItemPresentationDto>) response.getEntity();
            assertThat(entity).containsExactly(validDto);
        } finally {
            logger.detachAppender(appender);
        }
        List<ILoggingEvent> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getThrowableProxy()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void itemCarriesTheSellersReputationVerdictOfTheService(boolean hasSellerSufficientReputation) {
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(chatService.getBisqEasyOfferbookChannelService().findChannel(market)).thenReturn(Optional.of(channel));
        BisqEasyOfferbookMessage message = offerMessage();
        ObservableSet<BisqEasyOfferbookMessage> messages = new ObservableSet<>();
        messages.add(message);
        when(channel.getChatMessages()).thenReturn(messages);
        BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
                mock(BisqEasySellersReputationBasedTradeAmountService.class);
        when(sellersReputationService.hasSellerSufficientReputation(message.getBisqEasyOffer().orElseThrow()))
                .thenReturn(hasSellerSufficientReputation);
        OfferbookRestApi api = new OfferbookRestApi(chatService,
                mock(MarketPriceService.class),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                sellersReputationService);

        try (MockedStatic<OfferItemPresentationDtoFactory> factory = mockStatic(OfferItemPresentationDtoFactory.class)) {
            factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());

            api.getOffers("USD");

            factory.verify(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), eq(hasSellerSufficientReputation), eq(message)));
        }
    }

    // The price spec which Bisq Connect (bisq-mobile 7f9cddce7b, PriceQuoteVOFactory.fromPrice) posts for a fixed
    // price of 64,321.1234 USD: the base side id is BTC, and the value is rounded to 8 significant digits.
    private static final String MOBILE_FIX_PRICE_SPEC_JSON = """
            {
              "type": "FixPriceSpec",
              "priceQuote": {
                "value": 643211230,
                "precision": 4,
                "lowPrecision": 2,
                "market": {"baseCurrencyCode": "BTC", "quoteCurrencyCode": "USD", "baseCurrencyName": "BTC", "quoteCurrencyName": "USD"},
                "baseSideMonetary": {"type": "Coin", "id": "BTC", "value": 100000000, "code": "BTC", "precision": 8, "lowPrecision": 4},
                "quoteSideMonetary": {"type": "Fiat", "id": "USD", "value": 643211234, "code": "USD", "precision": 4, "lowPrecision": 2}
              }
            }""";

    @Test
    void fixedPriceOfBisqConnectIsPublishedInTheCanonicalForm() throws Exception {
        // The endpoint writes the text of the offer message
        Res.setAndApplyLanguageTag("en");
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        BisqEasyOfferbookChannelService channelService = chatService.getBisqEasyOfferbookChannelService();
        when(channelService.findChannel(market)).thenReturn(Optional.of(new BisqEasyOfferbookChannel(market)));
        when(channelService.publishChatMessage(any(BisqEasyOfferbookMessage.class), any(UserIdentity.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        UserIdentity userIdentity = userService.getUserIdentityService().getSelectedUserIdentity();
        UserProfile userProfile = mock(UserProfile.class);
        when(userIdentity.getNickName()).thenReturn("maker");
        when(userIdentity.getUserProfile()).thenReturn(userProfile);
        // A mock, because the test classpath of this module cannot load the BouncyCastle classes of a key generation
        NetworkId networkId = mock(NetworkId.class, RETURNS_DEEP_STUBS);
        when(networkId.getPubKey().getId()).thenReturn("a".repeat(40));
        when(userProfile.getNetworkId()).thenReturn(networkId);
        when(userProfile.getId()).thenReturn("a".repeat(40));
        when(userProfile.getTerms()).thenReturn("");
        OfferbookRestApi api = new OfferbookRestApi(chatService, mock(MarketPriceService.class), userService,
                mock(BisqEasySellersReputationBasedTradeAmountService.class));
        CreateOfferRequest request = new CreateOfferRequest(DirectionDto.SELL,
                new MarketDto("BTC", "USD", "Bitcoin", "US Dollar"),
                Set.of("MAIN_CHAIN"),
                Set.of("SEPA"),
                new QuoteSideFixedAmountSpecDto(1000_0000),
                JsonMapperProvider.get().readValue(MOBILE_FIX_PRICE_SPEC_JSON, PriceSpecDto.class),
                Set.of("en"));
        AsyncResponse asyncResponse = mock(AsyncResponse.class);

        api.createOffer(request, asyncResponse);

        ArgumentCaptor<Object> response = ArgumentCaptor.forClass(Object.class);
        verify(asyncResponse).resume(response.capture());
        Response actual = (Response) response.getValue();
        assertThat(actual.getStatus()).as(() -> String.valueOf(actual.getEntity()))
                .isEqualTo(Response.Status.CREATED.getStatusCode());
        ArgumentCaptor<BisqEasyOfferbookMessage> published = ArgumentCaptor.forClass(BisqEasyOfferbookMessage.class);
        verify(channelService).publishChatMessage(published.capture(), any(UserIdentity.class));
        FixPriceSpec priceSpec = (FixPriceSpec) published.getValue().getBisqEasyOffer().orElseThrow().getPriceSpec();
        // The amounts of the canonical factories, and the value derived from the price which the user entered
        assertThat(priceSpec.getPriceQuote()).isEqualTo(PriceQuote.from(Coin.asBtcFromFaceValue(1.0),
                Fiat.fromValue(643_211_234L, "USD")));
        assertThat(priceSpec.getPriceQuote().getValue()).isEqualTo(643_211_234L);
    }

    private BisqEasyOfferbookMessage offerMessage() {
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.hasBisqEasyOffer()).thenReturn(true);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(mock(BisqEasyOffer.class)));
        return message;
    }
}
