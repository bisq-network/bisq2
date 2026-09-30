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

package bisq.offer.price.spec;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The wire carries the quote's value and both monetaries independently, while honest
 * construction derives the value from the pair and uses the canonical monetary factories.
 * FixPriceSpec.verify() runs in the constructor and therefore also at deserialization.
 */
public class FixPriceSpecTest {
    private static final Monetary ONE_BTC = Coin.asBtcFromFaceValue(1.0);

    @Test
    public void honestFiatQuotePasses() {
        FixPriceSpec spec = new FixPriceSpec(PriceQuote.fromFiatPrice(100_000, "USD"));
        assertEquals("USD", spec.getPriceQuote().getMarket().getQuoteCurrencyCode());
    }

    @Test
    public void honestAltcoinQuotePasses() {
        FixPriceSpec spec = new FixPriceSpec(PriceQuote.fromAltCoinPrice(0.0025, "XMR"));
        assertEquals("XMR", spec.getPriceQuote().getMarket().getBaseCurrencyCode());
    }

    @Test
    public void honestRoundedQuotePasses() {
        // The derived value rounds (HALF_UP) for non-exact ratios; the consistency check must
        // accept its own rounding.
        PriceQuote rounded = PriceQuote.from(Coin.fromFaceValue(3.0, "XMR"), Coin.asBtcFromFaceValue(0.01));
        new FixPriceSpec(rounded);
    }

    @Test
    public void zeroValueIsRejected() {
        PriceQuote malformed = new PriceQuote(0, ONE_BTC, Fiat.fromFaceValue(100_000, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void negativeValueIsRejected() {
        PriceQuote malformed = new PriceQuote(-1, ONE_BTC, Fiat.fromFaceValue(100_000, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void valueInconsistentWithTheMonetariesIsRejected() {
        PriceQuote honest = PriceQuote.fromFiatPrice(100_000, "USD");
        PriceQuote malformed = new PriceQuote(honest.getValue() + 1,
                honest.getBaseSideMonetary(),
                honest.getQuoteSideMonetary());
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void forgedQuoteSidePrecisionIsRejected() {
        // An 8-decimal USD monetary flowing into 4-decimal fiat arithmetic corrupts every
        // downstream comparison by a factor of 10^4.
        Monetary forgedQuote = new Fiat("USD", 100_000_0000L, "USD", 8, 2);
        PriceQuote malformed = PriceQuote.from(ONE_BTC, forgedQuote);
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void forgedBaseSidePrecisionIsRejected() {
        Monetary forgedBase = new Coin("BTC", 10_000L, "BTC", 4, 4);
        PriceQuote malformed = PriceQuote.from(forgedBase, Fiat.fromFaceValue(100_000, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void forgedIdIsRejected() {
        Monetary forgedId = new Fiat("EVIL", 100_000_0000L, "USD", 4, 2);
        PriceQuote malformed = PriceQuote.from(ONE_BTC, forgedId);
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void wrongMonetaryTypeForTheCodeIsRejected() {
        Monetary coinTypedUsd = new Coin("USD", 100_000_0000L, "USD", 4, 4);
        PriceQuote malformed = PriceQuote.from(ONE_BTC, coinTypedUsd);
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void hugeForgedPrecisionIsRejectedBeforeTheArithmetic() {
        // A forged precision must be rejected by the canonical check, not fed into the
        // consistency recomputation's movePointRight (which would expand to an enormous
        // BigDecimal). The check completes quickly rather than exhausting memory.
        Monetary forgedBase = new Coin("BTC", 1L, "BTC", 100_000_000, 4);
        PriceQuote malformed = new PriceQuote(90_000_0000L, forgedBase, Fiat.fromFaceValue(90_000, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void overflowingDerivedValueIsRejected() {
        // The exact derived value of this pair overflows long; its truncated low bits collide
        // with the carried value, so a lossy recomputation would accept an inconsistent triple
        // (the pair encodes 18,446,744 USD per sat while the value reads 9,044 USD per BTC).
        PriceQuote malformed = new PriceQuote(90_448_384L,
                Coin.fromValue(1L, "BTC"),
                Fiat.fromValue(184_467_440_738L, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    @Test
    public void malformedQuoteIsRejectedAtProtoDeserialization() {
        PriceQuote malformed = new PriceQuote(0, ONE_BTC, Fiat.fromFaceValue(100_000, "USD"));
        bisq.offer.protobuf.FixPrice proto = bisq.offer.protobuf.FixPrice.newBuilder()
                .setPriceQuote(malformed.toProto(false))
                .build();
        assertThrows(IllegalArgumentException.class, () -> FixPriceSpec.fromProto(proto));
    }

    @Test
    public void honestQuoteSurvivesProtoRoundTrip() {
        FixPriceSpec spec = new FixPriceSpec(PriceQuote.fromFiatPrice(100_000, "USD"));
        bisq.offer.protobuf.FixPrice proto = bisq.offer.protobuf.FixPrice.newBuilder()
                .setPriceQuote(spec.getPriceQuote().toProto(false))
                .build();
        assertEquals(spec, FixPriceSpec.fromProto(proto));
    }
}
