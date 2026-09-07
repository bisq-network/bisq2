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
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasySellersReputationBasedTradeAmountServiceTest {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();

    @Test
    void cachedInsufficientReputationIsReevaluatedWhenMarketPricesChange() {
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)));
        when(marketPriceService.findMarketPriceQuote(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)).map(MarketPrice::getPriceQuote));

        UserProfile seller = mock(UserProfile.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.findUserProfile("seller-id")).thenReturn(Optional.of(seller));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());
        ReputationScore reputationScore = mock(ReputationScore.class);
        when(reputationScore.getTotalScore()).thenReturn(30_000L);
        when(reputationService.getReputationScore(seller)).thenReturn(reputationScore);

        // 0.002 BTC needs a score of 200 per USD: 40,000 at 100,000 USD/BTC and 20,000 at 50,000 USD/BTC.
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMakersUserProfileId()).thenReturn("seller-id");
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getAmountSpec()).thenReturn(new BaseSideFixedAmountSpec(200_000));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());

        BisqEasySellersReputationBasedTradeAmountService service =
                new BisqEasySellersReputationBasedTradeAmountService(userProfileService, reputationService, marketPriceService);
        service.initialize().join();

        putMarketPrice(marketPriceByCurrencyMap, 100_000);
        assertFalse(service.hasSellerSufficientReputation(offer));

        putMarketPrice(marketPriceByCurrencyMap, 50_000);
        assertTrue(service.hasSellerSufficientReputation(offer));
    }

    @Test
    void checkStartedBeforeAPriceChangeDoesNotCacheItsStaleResult() {
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        // While the first check runs, every price lookup keeps answering with the price the check
        // started with, although the price change lands (and invalidates the cache) mid-check.
        MarketPrice[] priceSeenByFirstCheck = {null};
        boolean[] firstCheckRunning = {true};
        when(marketPriceService.findMarketPrice(MARKET)).thenAnswer(invocation -> {
            if (firstCheckRunning[0]) {
                if (priceSeenByFirstCheck[0] == null) {
                    priceSeenByFirstCheck[0] = marketPriceByCurrencyMap.get(MARKET);
                    putMarketPrice(marketPriceByCurrencyMap, 50_000);
                }
                return Optional.of(priceSeenByFirstCheck[0]);
            }
            return Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET));
        });
        when(marketPriceService.findMarketPriceQuote(MARKET))
                .thenAnswer(invocation -> marketPriceService.findMarketPrice(MARKET).map(MarketPrice::getPriceQuote));

        UserProfile seller = mock(UserProfile.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.findUserProfile("seller-id")).thenReturn(Optional.of(seller));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());
        ReputationScore reputationScore = mock(ReputationScore.class);
        when(reputationScore.getTotalScore()).thenReturn(30_000L);
        when(reputationService.getReputationScore(seller)).thenReturn(reputationScore);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMakersUserProfileId()).thenReturn("seller-id");
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getAmountSpec()).thenReturn(new BaseSideFixedAmountSpec(200_000));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        BisqEasySellersReputationBasedTradeAmountService service =
                new BisqEasySellersReputationBasedTradeAmountService(userProfileService, reputationService, marketPriceService);
        service.initialize().join();
        putMarketPrice(marketPriceByCurrencyMap, 100_000);

        assertFalse(service.hasSellerSufficientReputation(offer));
        firstCheckRunning[0] = false;

        assertTrue(service.hasSellerSufficientReputation(offer));
    }

    @Test
    void offerWhoseUsdAmountOverflowsIsTreatedAsInsufficientReputationInsteadOfThrowing() {
        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(new ObservableHashMap<>());
        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");
        PriceQuote usdQuote = PriceQuote.fromFiatPrice(100_000, "USD");
        MarketPrice eurPrice = mock(MarketPrice.class);
        when(eurPrice.getPriceQuote()).thenReturn(eurQuote);
        MarketPrice usdPrice = mock(MarketPrice.class);
        when(usdPrice.getPriceQuote()).thenReturn(usdQuote);
        when(marketPriceService.findMarketPrice(eurMarket)).thenReturn(Optional.of(eurPrice));
        when(marketPriceService.findMarketPriceQuote(eurMarket)).thenReturn(Optional.of(eurQuote));
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(usdPrice));
        when(marketPriceService.findMarketPriceQuote(MARKET)).thenReturn(Optional.of(usdQuote));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());
        // 1e18 sat converts to 9e18 EUR units at 90,000 EUR/BTC, which fits a long, but to 1e19 USD
        // units at 100,000 USD/BTC, which does not.
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMakersUserProfileId()).thenReturn("seller-id");
        when(offer.getMarket()).thenReturn(eurMarket);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getAmountSpec()).thenReturn(new BaseSideFixedAmountSpec(1_000_000_000_000_000_000L));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        BisqEasySellersReputationBasedTradeAmountService service =
                new BisqEasySellersReputationBasedTradeAmountService(mock(UserProfileService.class), reputationService, marketPriceService);
        service.initialize().join();

        assertFalse(service.hasSellerSufficientReputation(offer));
    }

    @Test
    void consecutiveScoreChangesOfTheSameMakerInvalidateTheCacheEachTime() {
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)));
        when(marketPriceService.findMarketPriceQuote(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)).map(MarketPrice::getPriceQuote));
        putMarketPrice(marketPriceByCurrencyMap, 100_000);
        UserProfile seller = mock(UserProfile.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.findUserProfile("seller-id")).thenReturn(Optional.of(seller));
        ReputationService reputationService = mock(ReputationService.class);
        ObservableHashMap<String, Long> scoreByUserProfileId = new ObservableHashMap<>();
        when(reputationService.getScoreByUserProfileId()).thenReturn(scoreByUserProfileId);
        ReputationScore reputationScore = mock(ReputationScore.class);
        long[] totalScore = {30_000L};
        when(reputationScore.getTotalScore()).thenAnswer(invocation -> totalScore[0]);
        when(reputationService.getReputationScore(seller)).thenReturn(reputationScore);
        // 0.002 BTC at 100,000 USD/BTC requires a score of 40,000.
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMakersUserProfileId()).thenReturn("seller-id");
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getAmountSpec()).thenReturn(new BaseSideFixedAmountSpec(200_000));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        BisqEasySellersReputationBasedTradeAmountService service =
                new BisqEasySellersReputationBasedTradeAmountService(userProfileService, reputationService, marketPriceService);
        service.initialize().join();

        scoreByUserProfileId.put("seller-id", 30_000L);
        assertFalse(service.hasSellerSufficientReputation(offer));

        totalScore[0] = 50_000L;
        scoreByUserProfileId.put("seller-id", 50_000L);
        assertTrue(service.hasSellerSufficientReputation(offer));

        totalScore[0] = 30_000L;
        scoreByUserProfileId.put("seller-id", 30_000L);
        assertFalse(service.hasSellerSufficientReputation(offer));

        totalScore[0] = 50_000L;
        scoreByUserProfileId.put("seller-id", 50_000L);
        assertTrue(service.hasSellerSufficientReputation(offer));
    }

    private void putMarketPrice(ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap, double price) {
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(price, "USD"));
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
    }
}
