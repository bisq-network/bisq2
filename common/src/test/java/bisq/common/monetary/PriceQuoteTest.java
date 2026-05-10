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

package bisq.common.monetary;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PriceQuoteTest {
    @Test
    @DisplayName("to quote monetary")
    void to_quote_monetary() {
        Coin btc = Coin.asBtcFromFaceValue(1.0);
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary quoteSideMonetary = priceQuote.toQuoteSideMonetary(btc);
        assertInstanceOf(Fiat.class, quoteSideMonetary);
        assertEquals(500000000, quoteSideMonetary.value);

        btc = Coin.asBtcFromFaceValue(2.0);
        priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        quoteSideMonetary = priceQuote.toQuoteSideMonetary(btc);
        assertEquals(1000000000, quoteSideMonetary.value);
    }

    @Nested
    @DisplayName("toBaseSideMonetary rounding (HALF_UP)")
    class ToBaseSideMonetaryRounding {

        @Test
        @DisplayName("HALF_UP rounds 0.5 satoshi boundary upward")
        void half_up_rounds_half_satoshi_boundary_upward() {
            // At $50,000/BTC: $25 = 50,000 sat exactly. Pick a fiat value that
            // produces exactly X.5 satoshis when divided by the price.
            // quote value = 250_001 (i.e. $25.0001 at precision 4)
            // base = quoteValue * 10^8 / priceValue = 250_001 * 10^8 / 500_000_000
            //      = 250_001 * 100_000_000 / 500_000_000 = 50_000.2  → rounds to 50_000
            PriceQuote quote = PriceQuote.fromFiatPrice(50_000, "USD");
            Fiat fiatAmount = Fiat.fromValue(250_001, "USD");
            Monetary baseSide = quote.toBaseSideMonetary(fiatAmount);
            assertThat(baseSide).isInstanceOf(Coin.class);
            assertThat(baseSide.getValue()).isEqualTo(50_000L);

            // Now try a value that hits exactly 0.5:
            // We need quoteValue such that quoteValue * 10^8 / priceValue ends in .5
            // priceValue for $50,000/BTC = 500_000_000 (at precision 4 for 1 BTC)
            // So we need quoteValue * 10^8 / 500_000_000 = X.5
            // quoteValue * 0.2 = X.5  →  quoteValue = (X.5) / 0.2 = 5*X + 2.5
            // quoteValue must be integer, so let's use a different price.
            // At $40,000/BTC (priceValue=400_000_000): 3 sat = $0.0012
            // quoteValue=12 → 12 * 10^8 / 400_000_000 = 3.0 (exact, not helpful)
            // quoteValue=10 → 10 * 10^8 / 400_000_000 = 2.5 → HALF_UP → 3
            PriceQuote quote40k = PriceQuote.fromFiatPrice(40_000, "USD");
            Fiat tenUnits = Fiat.fromValue(10, "USD");
            Monetary result = quote40k.toBaseSideMonetary(tenUnits);
            assertThat(result.getValue())
                    .as("2.5 satoshis should round to 3 with HALF_UP")
                    .isEqualTo(3L);
        }

        @Test
        @DisplayName("values just below 0.5 round down")
        void values_just_below_half_round_down() {
            // At $40,000/BTC: quoteValue=9 → 9 * 10^8 / 400_000_000 = 2.25 → truncated to 2
            PriceQuote quote40k = PriceQuote.fromFiatPrice(40_000, "USD");
            Fiat nineUnits = Fiat.fromValue(9, "USD");
            Monetary result = quote40k.toBaseSideMonetary(nineUnits);
            assertThat(result.getValue()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("toQuoteSideMonetary truncation (no rounding)")
    class ToQuoteSideMonetaryTruncation {

        @Test
        @DisplayName("truncation loses fractional fiat units")
        void truncation_loses_fractional_fiat_units() {
            // toQuoteSideMonetary uses .longValue() which truncates.
            // At $50,000/BTC: 1 sat = $0.0005 = 5 fiat units (precision 4)
            // 3 sat = 15 fiat units (exact)
            // But 1 sat at a price where the result is fractional:
            // At $33,333/BTC (priceValue ≈ 333_330_000):
            // 1 sat → 1 * 333_330_000 / 10^8 = 3.3333 → truncated to 3
            PriceQuote quote = PriceQuote.fromFiatPrice(33_333, "USD");
            Coin oneSat = Coin.fromValue(1, "BTC");
            Monetary quoteSide = quote.toQuoteSideMonetary(oneSat);
            assertThat(quoteSide.getValue()).isEqualTo(3L);
        }

        @Test
        @DisplayName("truncation vs HALF_UP: toQuoteSideMonetary always truncates down")
        void truncation_always_rounds_down() {
            // toQuoteSideMonetary uses .longValue() (truncation) while
            // toBaseSideMonetary uses HALF_UP. They are asymmetric by design.
            // At $33,333/BTC: 5 sat → 5 * 333_330_000 / 10^8 = 16.6665
            // Truncation → 16 (rounds down), whereas HALF_UP would give 17.
            PriceQuote quote = PriceQuote.fromFiatPrice(33_333, "USD");
            Coin fiveSats = Coin.fromValue(5, "BTC");
            Monetary quoteSide = quote.toQuoteSideMonetary(fiveSats);
            assertThat(quoteSide.getValue())
                    .as("truncation always rounds toward zero")
                    .isEqualTo(16L);

            // Manually compute what HALF_UP would have given:
            // 5 * 333_330_000 = 1_666_650_000 → movePointLeft(8) = 16.66650000
            // HALF_UP → 17
            java.math.BigDecimal exact = java.math.BigDecimal.valueOf(5)
                    .multiply(java.math.BigDecimal.valueOf(quote.getValue()))
                    .movePointLeft(8);
            long halfUpValue = exact.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            assertThat(halfUpValue)
                    .as("HALF_UP would round 16.6665 to 17, not 16")
                    .isEqualTo(17L);
            assertThat(quoteSide.getValue())
                    .as("truncation and HALF_UP produce different results for the same input")
                    .isLessThan(halfUpValue);
        }
    }

    @Nested
    @DisplayName("round-trip consistency")
    class RoundTripConsistency {

        @Test
        @DisplayName("base to quote and back preserves value for exact conversions")
        void round_trip_preserves_exact_conversions() {
            PriceQuote quote = PriceQuote.fromFiatPrice(50_000, "USD");
            Coin original = Coin.fromValue(100_000, "BTC"); // 0.001 BTC

            Monetary quoteSide = quote.toQuoteSideMonetary(original);
            Monetary backToBase = quote.toBaseSideMonetary(quoteSide);
            assertThat(backToBase.getValue())
                    .as("exact conversion should round-trip perfectly")
                    .isEqualTo(original.getValue());
        }

        @Test
        @DisplayName("round-trip drift is bounded to 1 satoshi for typical trade amounts")
        void round_trip_drift_bounded_for_typical_amounts() {
            PriceQuote quote = PriceQuote.fromFiatPrice(60_000, "USD");
            // 0.001 BTC = 100_000 sat at $60,000 = $60 = 600_000 fiat units
            Coin baseMoney = Coin.fromValue(100_000, "BTC");

            Monetary quoteSide = quote.toQuoteSideMonetary(baseMoney);
            Monetary backToBase = quote.toBaseSideMonetary(quoteSide);
            long drift = Math.abs(backToBase.getValue() - baseMoney.getValue());
            assertThat(drift)
                    .as("drift for typical 0.001 BTC trade")
                    .isLessThanOrEqualTo(1L);
        }

        @Test
        @DisplayName("round-trip from quote side: quote to base and back")
        void round_trip_from_quote_side() {
            PriceQuote quote = PriceQuote.fromFiatPrice(50_000, "USD");
            Fiat originalQuote = Fiat.fromValue(500_000, "USD"); // $50

            Monetary baseSide = quote.toBaseSideMonetary(originalQuote);
            Monetary backToQuote = quote.toQuoteSideMonetary(baseSide);
            long drift = Math.abs(backToQuote.getValue() - originalQuote.getValue());
            assertThat(drift)
                    .as("quote→base→quote drift should be at most 1 fiat unit")
                    .isLessThanOrEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("extreme but valid values")
    class ExtremeValues {

        @Test
        @DisplayName("minimum trade: ~$6 / ~10K sat at $60,000/BTC")
        void minimum_trade_amount() {
            PriceQuote quote = PriceQuote.fromFiatPrice(60_000, "USD");
            Coin minBtc = Coin.fromValue(10_000, "BTC");

            Monetary quoteSide = quote.toQuoteSideMonetary(minBtc);
            assertThat(quoteSide).isInstanceOf(Fiat.class);
            assertThat(quoteSide.getValue()).isEqualTo(60_000L); // $6.0000

            Monetary backToBase = quote.toBaseSideMonetary(quoteSide);
            assertThat(backToBase.getValue()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("maximum Bisq Easy trade: ~$600 / ~250K sat at ~$240,000/BTC")
        void maximum_trade_amount() {
            PriceQuote quote = PriceQuote.fromFiatPrice(240_000, "USD");
            Coin maxBtc = Coin.fromValue(250_000, "BTC");

            Monetary quoteSide = quote.toQuoteSideMonetary(maxBtc);
            assertThat(quoteSide).isInstanceOf(Fiat.class);
            assertThat(quoteSide.getValue()).isEqualTo(6_000_000L); // $600.0000

            Monetary backToBase = quote.toBaseSideMonetary(quoteSide);
            assertThat(backToBase.getValue()).isEqualTo(250_000L);
        }

        @Test
        @DisplayName("very low BTC price ($100/BTC)")
        void very_low_btc_price() {
            PriceQuote quote = PriceQuote.fromFiatPrice(100, "USD");
            Coin btc = Coin.fromValue(100_000_000, "BTC"); // 1 BTC

            Monetary quoteSide = quote.toQuoteSideMonetary(btc);
            assertThat(quoteSide.getValue()).isEqualTo(1_000_000L); // $100.0000
        }

        @Test
        @DisplayName("very high BTC price ($1,000,000/BTC)")
        void very_high_btc_price() {
            PriceQuote quote = PriceQuote.fromFiatPrice(1_000_000, "USD");
            Coin oneSat = Coin.fromValue(1, "BTC");

            Monetary quoteSide = quote.toQuoteSideMonetary(oneSat);
            // 1 sat at $1M/BTC = $0.01 = 100 fiat units
            assertThat(quoteSide.getValue()).isEqualTo(100L);
        }

        @Test
        @DisplayName("1 satoshi at $60,000 produces non-zero quote")
        void one_satoshi_produces_nonzero_quote() {
            PriceQuote quote = PriceQuote.fromFiatPrice(60_000, "USD");
            Coin oneSat = Coin.fromValue(1, "BTC");

            Monetary quoteSide = quote.toQuoteSideMonetary(oneSat);
            // 1 * 600_000_000 / 10^8 = 6.0 → 6 fiat units ($0.0006)
            assertThat(quoteSide.getValue()).isEqualTo(6L);
        }
    }
}