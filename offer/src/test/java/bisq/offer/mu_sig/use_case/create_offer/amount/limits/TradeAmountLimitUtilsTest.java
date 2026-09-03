package bisq.offer.mu_sig.use_case.create_offer.amount.limits;

import bisq.common.market.Market;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.TradeAmountLimitUtils.Rates;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeAmountLimitUtilsTest {
    private final Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private final Fiat tenThousandUsd = Fiat.fromFaceValue(10_000, "USD");

    @Test
    void exactLimitDerivesBothSidesFromTheCapturedRates() {
        Rates rates = new Rates(PriceQuote.fromFiatPrice(100_000, "USD"),
                Optional.of(PriceQuote.fromFiatPrice(90_000, "EUR")));
        PriceQuote resolvedQuote = PriceQuote.fromFiatPrice(90_000, "EUR");

        TradeAmount limit = TradeAmountLimitUtils.toTradeAmountLimitExact(rates, eurMarket, resolvedQuote, tenThousandUsd);

        assertEquals(Fiat.fromFaceValue(9_000, "EUR"), limit.getQuoteSideAmount());
        assertEquals(10_000_000L, limit.getBaseSideAmount().getValue());
    }

    @Test
    void exactLimitFailsClosedWhenTheFiatLegWraps() {
        // At 0.01 USD/BTC the 10,000 USD cap is 10^6 BTC; at 2 * 10^9 EUR/BTC that is 2 * 10^19
        // fiat units, beyond long range. The plain chain wraps it into a POSITIVE limit that
        // the downstream positivity guard would accept, effectively removing the cap.
        Rates rates = new Rates(PriceQuote.fromFiatPrice(0.01, "USD"),
                Optional.of(PriceQuote.fromFiatPrice(2_000_000_000, "EUR")));
        PriceQuote resolvedQuote = PriceQuote.fromFiatPrice(2_000_000_000, "EUR");

        TradeAmount wrapped = TradeAmountLimitUtils.toTradeAmountLimit(rates, eurMarket, resolvedQuote, tenThousandUsd);
        assertTrue(wrapped.getQuoteSideAmount().getValue() > 0);

        assertThrows(ArithmeticException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimitExact(rates, eurMarket, resolvedQuote, tenThousandUsd));
    }

    @Test
    void limitConversionsRejectAPriceQuoteOfAnotherMarket() {
        // PriceQuote conversions check only the monetary classes: a BTC/GBP quote would price a
        // BTC/EUR limit's Bitcoin side at the GBP rate.
        Rates rates = new Rates(PriceQuote.fromFiatPrice(100_000, "USD"),
                Optional.of(PriceQuote.fromFiatPrice(90_000, "EUR")));
        PriceQuote gbpQuote = PriceQuote.fromFiatPrice(80_000, "GBP");

        assertThrows(IllegalArgumentException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimit(rates, eurMarket, gbpQuote, tenThousandUsd));
        assertThrows(IllegalArgumentException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimitExact(rates, eurMarket, gbpQuote, tenThousandUsd));
    }

    @Test
    void limitConversionsRejectAFiatRateOfAnotherMarket() {
        Rates rates = new Rates(PriceQuote.fromFiatPrice(100_000, "USD"),
                Optional.of(PriceQuote.fromFiatPrice(80_000, "GBP")));
        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");

        assertThrows(IllegalArgumentException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimit(rates, eurMarket, eurQuote, tenThousandUsd));
        assertThrows(IllegalArgumentException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimitExact(rates, eurMarket, eurQuote, tenThousandUsd));
    }

    @Test
    void limitConversionsRejectAMissingFiatRateForAFiatMarket() {
        Rates rates = new Rates(PriceQuote.fromFiatPrice(100_000, "USD"), Optional.empty());
        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");

        assertThrows(IllegalArgumentException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimit(rates, eurMarket, eurQuote, tenThousandUsd));
        assertThrows(IllegalArgumentException.class,
                () -> TradeAmountLimitUtils.toTradeAmountLimitExact(rates, eurMarket, eurQuote, tenThousandUsd));
    }
}
