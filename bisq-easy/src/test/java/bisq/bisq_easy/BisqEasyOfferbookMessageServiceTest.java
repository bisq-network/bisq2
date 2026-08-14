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

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.user.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
        when(offer.getPriceSpec()).thenReturn(new bisq.offer.price.spec.MarketPriceSpec());

        assertFalse(service.isValid(message));
    }

    @Test
    void marketPricedOfferBecomesValidOnceItsPriceArrives() {
        // The offer-first, price-later case: a market-priced offer is invalid while its price
        // is unavailable, but the same offer becomes valid once the price arrives, so a
        // controller re-scan on price change re-admits it. An overflow, by contrast, is
        // permanent (covered by the overflow test below).
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.empty());
        BisqEasyOfferbookMessageService service = createService(marketPriceService);
        BisqEasyOfferbookMessage message = messageWithOffer(Direction.BUY, 50_000);
        BisqEasyOffer offer = message.getBisqEasyOffer().orElseThrow();
        when(offer.getPriceSpec()).thenReturn(new bisq.offer.price.spec.MarketPriceSpec());

        assertFalse(service.isValid(message));

        bisq.bonded_roles.market_price.MarketPrice marketPrice =
                mock(bisq.bonded_roles.market_price.MarketPrice.class);
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
}
