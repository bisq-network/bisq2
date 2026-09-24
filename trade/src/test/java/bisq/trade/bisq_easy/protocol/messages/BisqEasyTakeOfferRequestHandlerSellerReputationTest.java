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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The maker as buyer accepts only a taker whose reputation covers the trade amount.
 */
class BisqEasyTakeOfferRequestHandlerSellerReputationTest {
    private static final Market USD_MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final Market EUR_MARKET = new Market("BTC", "EUR", "Bitcoin", "Euro");
    // MarketPrice treats a price older than 12 hours as outdated
    private static final long OUTDATED_AGE = TimeUnit.HOURS.toMillis(12) + TimeUnit.MINUTES.toMillis(1);

    private MarketPriceService marketPriceService;
    private ReputationService reputationService;
    private BisqEasyOffer offer;
    private BisqEasyContract contract;
    private BisqEasyTakeOfferRequestHandler handler;
    private long now;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        marketPriceService = mock(MarketPriceService.class);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        now = System.currentTimeMillis();
        setUpMarketPrice(USD_MARKET, 60_000, now);
        reputationService = mock(ReputationService.class);
        when(serviceProvider.getUserService().getReputationService()).thenReturn(reputationService);

        offer = mock(BisqEasyOffer.class);
        when(offer.getMarket()).thenReturn(USD_MARKET);
        when(offer.getTakersDirection()).thenReturn(Direction.SELL);
        contract = mock(BisqEasyContract.class, RETURNS_DEEP_STUBS);
        // 100 USD at 200 score per USD requires a score of 20_000
        when(contract.getQuoteSideMonetary()).thenReturn(Fiat.fromFaceValue(100, "USD"));
        when(contract.getTaker().getNetworkId().getId()).thenReturn("taker-profile");

        handler = new BisqEasyTakeOfferRequestHandler(serviceProvider, mock(BisqEasyTrade.class));
    }

    private void setUpMarketPrice(Market market, double price, long timestamp) {
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(price, market.getQuoteCurrencyCode());
        MarketPrice marketPrice = new MarketPrice(priceQuote, timestamp, mock(MarketPriceProviderInfo.class));
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.of(marketPrice));
    }

    private void setUpEurOffer(long eurPriceTimestamp) {
        when(offer.getMarket()).thenReturn(EUR_MARKET);
        setUpMarketPrice(EUR_MARKET, 50_000, eurPriceTimestamp);
        // 500 EUR are 0.01 BTC, which are 600 USD and require a score of 120_000
        when(contract.getQuoteSideMonetary()).thenReturn(Fiat.fromFaceValue(500, "EUR"));
    }

    private void setUpTakersReputationScore(long totalScore) {
        when(reputationService.getReputationScore("taker-profile")).thenReturn(new ReputationScore(totalScore, 0, 0));
    }

    @Test
    void doesNotCheckReputationOfTakerAsBuyer() {
        when(offer.getTakersDirection()).thenReturn(Direction.BUY);

        assertDoesNotThrow(() -> handler.validateTakersReputation(offer, contract));

        verifyNoInteractions(reputationService);
    }

    @Test
    void rejectsTakerWithoutReputation() {
        setUpTakersReputationScore(0);

        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> handler.validateTakersReputation(offer, contract));

        assertEquals(TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW, exception.getTradeProtocolFailure());
    }

    @Test
    void rejectsTakerWithReputationBelowTradeAmount() {
        setUpTakersReputationScore(19_000);

        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> handler.validateTakersReputation(offer, contract));

        assertEquals(TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW, exception.getTradeProtocolFailure());
    }

    @Test
    void acceptsTakerWithReputationCoveringTradeAmount() {
        setUpTakersReputationScore(20_000);

        assertDoesNotThrow(() -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void acceptsTakerWithReputationWithinTolerance() {
        setUpTakersReputationScore(19_048);

        assertDoesNotThrow(() -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void rejectsTakerWithoutMarketPrice() {
        setUpTakersReputationScore(20_000);
        when(marketPriceService.findMarketPrice(USD_MARKET)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void rejectsTakerWithOutdatedMarketPrice() {
        setUpTakersReputationScore(20_000);
        setUpMarketPrice(USD_MARKET, 60_000, now - OUTDATED_AGE);

        assertThrows(IllegalArgumentException.class, () -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void acceptsTakerWithReputationCoveringUsdValueOfEurAmount() {
        setUpEurOffer(now);
        setUpTakersReputationScore(120_000);

        assertDoesNotThrow(() -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void rejectsTakerWithReputationCoveringOnlyFaceValueOfEurAmount() {
        // With one of the two prices used for both conversions, 500 EUR would count as 500 USD or less.
        setUpEurOffer(now);
        setUpTakersReputationScore(100_000);

        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> handler.validateTakersReputation(offer, contract));

        assertEquals(TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW, exception.getTradeProtocolFailure());
    }

    @Test
    void acceptsEurAndUsdMarketPricesWithinTimeLimitOfEachOther() {
        setUpEurOffer(now - MarketPrice.STALE_AGE);
        setUpTakersReputationScore(120_000);

        assertDoesNotThrow(() -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void rejectsEurAndUsdMarketPricesTooFarApart() {
        setUpEurOffer(now - MarketPrice.STALE_AGE - 1);
        setUpTakersReputationScore(120_000);

        assertThrows(IllegalArgumentException.class, () -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void rejectsTakerWhenOnlyEurMarketPriceIsOutdated() {
        long eurPriceTimestamp = now - OUTDATED_AGE;
        setUpEurOffer(eurPriceTimestamp);
        // The USD price is not outdated and not too far from the EUR price.
        setUpMarketPrice(USD_MARKET, 60_000, eurPriceTimestamp + MarketPrice.STALE_AGE);
        setUpTakersReputationScore(120_000);

        assertThrows(IllegalArgumentException.class, () -> handler.validateTakersReputation(offer, contract));
    }

    @Test
    void rejectsTakerWithoutUsdMarketPriceInEurMarket() {
        setUpEurOffer(now);
        when(marketPriceService.findMarketPrice(USD_MARKET)).thenReturn(Optional.empty());
        setUpTakersReputationScore(120_000);

        assertThrows(IllegalArgumentException.class, () -> handler.validateTakersReputation(offer, contract));
    }
}
