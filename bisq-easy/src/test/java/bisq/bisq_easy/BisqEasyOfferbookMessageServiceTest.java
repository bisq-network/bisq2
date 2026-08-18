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

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.monetary.PriceQuote;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.user.UserService;
import bisq.user.banned.BannedUserProfileData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyOfferbookMessageServiceTest {
    private final Market market = MarketRepository.getUSDBitcoinMarket();

    private BisqEasyOfferbookMessageService createService() {
        return createService(mock(MarketPriceService.class));
    }

    private BisqEasyOfferbookMessageService createService(MarketPriceService marketPriceService) {
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
                mock(BisqEasySellersReputationBasedTradeAmountService.class);
        when(sellersReputationService.hasSellerSufficientReputation(any(BisqEasyOfferbookMessage.class))).thenReturn(true);
        return new BisqEasyOfferbookMessageService(chatService,
                userService,
                sellersReputationService,
                marketPriceService);
    }

    private BisqEasyOfferbookMessage messageWithOffer(Direction direction, long quoteSideFixedAmount) {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMarket()).thenReturn(market);
        when(offer.getDirection()).thenReturn(direction);
        when(offer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(quoteSideFixedAmount));
        // A fixed price of one quote unit per BTC: converting the quote amount to the base
        // side multiplies by 10^8.
        when(offer.getPriceSpec()).thenReturn(new FixPriceSpec(PriceQuote.fromFiatPrice(0.0001, "USD")));
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.getAuthorUserProfileId()).thenReturn("author-id");
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        return message;
    }

    @Test
    void offersWithConvertibleAmountsAreValid() {
        BisqEasyOfferbookMessageService service = createService();
        assertTrue(service.isValid(messageWithOffer(Direction.BUY, 50_000)));
        assertTrue(service.isValid(messageWithOffer(Direction.SELL, 50_000)));
    }

    @Test
    void offersWhoseAmountsCannotBeResolvedAreInvalid() {
        BisqEasyOfferbookMessageService service = createService();
        BisqEasyOfferbookMessage message = messageWithOffer(Direction.BUY, 50_000);
        BisqEasyOffer offer = message.getBisqEasyOffer().orElseThrow();
        // A market price spec without an available market price: the conversions return empty
        // instead of throwing, and downstream list items call orElseThrow on them.
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());

        assertFalse(service.isValid(message));
    }

    @Test
    void marketPricedOfferCanBeRevalidatedOnceItsPriceArrives() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.empty());
        BisqEasyOfferbookMessageService service = createService(marketPriceService);
        BisqEasyOfferbookMessage message = messageWithOffer(Direction.BUY, 50_000);
        BisqEasyOffer offer = message.getBisqEasyOffer().orElseThrow();
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());

        assertFalse(service.isValid(message));

        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(50_000, "USD"));
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.of(marketPrice));

        assertTrue(service.isValid(message));
    }

    @Test
    void offersWhoseAmountConversionOverflowsAreInvalidForBothDirections() {
        BisqEasyOfferbookMessageService service = createService();
        // Long.MAX_VALUE quote units at a one-unit price: the base side overflows a long.
        // Buy offers short-circuit the reputation check, so the resolvability gate must be
        // independent of the offer direction.
        assertFalse(service.isValid(messageWithOffer(Direction.BUY, Long.MAX_VALUE)));
        assertFalse(service.isValid(messageWithOffer(Direction.SELL, Long.MAX_VALUE)));
    }

    @Test
    void offerValidityRevisionAdvancesWhenIgnoredBannedOrMakerReputationChanges() {
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(market);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getMakersUserProfileId()).thenReturn("maker-id");
        BisqEasyOfferbookMessage makersMessage = mock(BisqEasyOfferbookMessage.class);
        when(makersMessage.getId()).thenReturn("offer-message-id");
        when(makersMessage.getAuthorUserProfileId()).thenReturn("author-id");
        when(makersMessage.hasBisqEasyOffer()).thenReturn(true);
        when(makersMessage.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        channel.addChatMessage(makersMessage);
        ObservableSet<BisqEasyOfferbookChannel> channels = new ObservableSet<>();
        channels.add(channel);
        when(chatService.getBisqEasyOfferbookChannelService().getChannels()).thenReturn(channels);
        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        ObservableSet<String> ignoredUserProfileIds = new ObservableSet<>();
        when(userService.getUserProfileService().getIgnoredUserProfileIds()).thenReturn(ignoredUserProfileIds);
        ObservableSet<BannedUserProfileData> bannedUserProfileDataSet = new ObservableSet<>();
        when(userService.getBannedUserService().getBannedUserProfileDataSet()).thenReturn(bannedUserProfileDataSet);
        ObservableHashMap<String, Long> scoreByUserProfileId = new ObservableHashMap<>();
        when(userService.getReputationService().getScoreByUserProfileId()).thenReturn(scoreByUserProfileId);
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(new ObservableHashMap<>());
        BisqEasyOfferbookMessageService service = new BisqEasyOfferbookMessageService(chatService,
                userService,
                mock(BisqEasySellersReputationBasedTradeAmountService.class),
                marketPriceService);
        service.initialize().join();
        List<Long> observedRevisions = new ArrayList<>();
        service.getOfferValidityRevision().addObserver(observedRevisions::add);
        long revision = observedRevisions.get(0);

        ignoredUserProfileIds.add("some-id");
        assertEquals(List.of(revision, revision + 1), observedRevisions);

        bannedUserProfileDataSet.add(mock(BannedUserProfileData.class));
        assertEquals(List.of(revision, revision + 1, revision + 2), observedRevisions);

        scoreByUserProfileId.put("not-a-maker-id", 1_000L);
        scoreByUserProfileId.put("author-id", 1_000L);
        assertEquals(List.of(revision, revision + 1, revision + 2), observedRevisions);

        scoreByUserProfileId.put("maker-id", 1_000L);
        scoreByUserProfileId.put("maker-id", 2_000L);
        assertEquals(List.of(revision, revision + 1, revision + 2, revision + 3, revision + 4), observedRevisions);

        scoreByUserProfileId.putAll(Map.of("maker-id", 3_000L, "not-a-maker-id", 3_000L, "other-maker-id", 3_000L));
        assertEquals(List.of(revision, revision + 1, revision + 2, revision + 3, revision + 4, revision + 5), observedRevisions);
    }

    @Test
    void offerValidityRevisionAdvancesWhenMarketPricesChangeUntilShutdown() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        BisqEasyOfferbookMessageService service = createService(marketPriceService);
        service.initialize().join();
        List<Long> observedRevisions = new ArrayList<>();
        service.getOfferValidityRevision().addObserver(observedRevisions::add);
        long revisionAtRegistration = observedRevisions.get(0);

        MarketPrice marketPrice = mock(MarketPrice.class);
        marketPriceByCurrencyMap.put(market, marketPrice);
        marketPriceByCurrencyMap.put(market, mock(MarketPrice.class));

        assertEquals(List.of(revisionAtRegistration, revisionAtRegistration + 1, revisionAtRegistration + 2),
                observedRevisions);

        service.shutdown().join();
        marketPriceByCurrencyMap.put(market, mock(MarketPrice.class));

        assertEquals(3, observedRevisions.size());
    }
}
