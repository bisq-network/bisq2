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

package bisq.offer.mu_sig.use_case.create_offer;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.offer.Direction;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateOfferUseCaseTest {
    private final Market usdMarket = MarketRepository.getUSDBitcoinMarket();
    private final ObservableHashMap<Market, MarketPrice> marketPriceMap = new ObservableHashMap<>();
    private final Map<Market, PriceQuote> quotes = new HashMap<>();

    @Test
    void draftIsNotReadyForReviewUntilTheMarketPriceArrives() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceMap);
        when(marketPriceService.findMarketPriceQuote(any())).thenAnswer(invocation ->
                Optional.ofNullable(quotes.get(invocation.getArgument(0, Market.class))));
        when(marketPriceService.getMarketPriceQuoteOrThrow(any())).thenAnswer(invocation -> {
            PriceQuote quote = quotes.get(invocation.getArgument(0, Market.class));
            if (quote == null) {
                throw new IllegalStateException("No market price available");
            }
            return quote;
        });

        CreateOfferUseCase useCase = new CreateOfferUseCase(marketPriceService,
                mock(CreateOfferDraftCookieStore.class),
                mock(AccountsProvider.class));
        useCase.initialize();
        useCase.getMarketSelection().onSetMarket(usdMarket);
        useCase.getDirectionSelection().onSetDisplayDirection(Direction.BUY);

        assertFalse(useCase.isDraftReadyForReview(),
                "an unpriced draft has no amounts or price quote and must not reach review");

        quotes.put(usdMarket, PriceQuote.fromFiatPrice(100_000, "USD"));
        marketPriceMap.put(usdMarket, mock(MarketPrice.class));

        assertTrue(useCase.isDraftReadyForReview(),
                "the gate must lift on its own once the market price arrives");
        assertNotNull(useCase.captureDraftSnapshot().amountSpec(),
                "a ready draft must materialize into a complete snapshot");
    }
}
