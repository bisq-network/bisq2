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
import bisq.common.proto.UnresolvableProtobufException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The wire carries the quote's value and both monetaries independently, while honest
 * construction derives the value from the pair and uses the canonical monetary factories.
 * FixPriceSpec.verify() runs in the constructor and therefore also at deserialization.
 */
public class FixPriceSpecTest {
    private static final Monetary ONE_BTC = Coin.asBtcFromFaceValue(1.0);
    // The bound takes milliseconds. Without it, the recomputation with the huge precision below
    // takes about 44 s (measured in the Gradle test worker on JDK 21).
    private static final Duration PRECISION_BOUND_TIME_LIMIT = Duration.ofSeconds(5);
    private static final String HUGE_PRECISION_MESSAGE =
            "The precision 100000000 of BTC is not the canonical precision 8";

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
        // Canonical id, so that the precision comparison and not the id comparison rejects it.
        Monetary forgedBase = new Coin("BTC [crypto]", 10_000L, "BTC", 4, 4);
        PriceQuote malformed = PriceQuote.from(forgedBase, Fiat.fromFaceValue(100_000, "USD"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new FixPriceSpec(malformed));
        assertEquals("The precision 4 of BTC is not the canonical precision 8", exception.getMessage());
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

    // Invariant: the node bounds a peer-supplied precision before any arithmetic whose cost grows
    // with that precision. If the consistency recomputation ran first, movePointRight(100_000_000)
    // would build a number of about 332 million bits, and the overflow check would then throw with
    // another message. A change of the bound may update the expected message, but must keep the
    // time limit.
    @Test
    public void hugeForgedPrecisionIsRejectedBeforeTheArithmetic() {
        PriceQuote malformed = quoteWithHugeBasePrecision();
        IllegalArgumentException exception = assertTimeoutPreemptively(PRECISION_BOUND_TIME_LIMIT,
                () -> assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed)));
        assertEquals(HUGE_PRECISION_MESSAGE, exception.getMessage());
    }

    @Test
    public void hugeForgedPrecisionIsRejectedBeforeTheArithmeticAtProtoDeserialization() {
        // Covers the whole parse path from the wire, so that precision-dependent work added
        // before verify() also fails the time limit.
        bisq.offer.protobuf.FixPrice proto = bisq.offer.protobuf.FixPrice.newBuilder()
                .setPriceQuote(quoteWithHugeBasePrecision().toProto(false))
                .build();
        UnresolvableProtobufException exception = assertTimeoutPreemptively(PRECISION_BOUND_TIME_LIMIT,
                () -> assertThrows(UnresolvableProtobufException.class, () -> FixPriceSpec.fromProto(proto)));
        assertEquals(HUGE_PRECISION_MESSAGE,
                assertInstanceOf(IllegalArgumentException.class, exception.getCause()).getMessage());
    }

    // Only the precision is forged. The id and the low precision are canonical and both values
    // are positive, so the precision bound is the only check that can reject this quote. Keep
    // 100_000_000: with 10_000_000 the unbounded path takes under 2 s, and from about 600_000_000
    // BigInteger fails at once.
    private static PriceQuote quoteWithHugeBasePrecision() {
        Monetary forgedBase = new Coin("BTC [crypto]", 1L, "BTC", 100_000_000, 4);
        return new PriceQuote(90_000_0000L, forgedBase, Fiat.fromFaceValue(90_000, "USD"));
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
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class, () -> FixPriceSpec.fromProto(proto)).getCause());
    }

    @Test
    public void honestQuoteSurvivesProtoRoundTrip() throws UnresolvableProtobufException {
        FixPriceSpec spec = new FixPriceSpec(PriceQuote.fromFiatPrice(100_000, "USD"));
        bisq.offer.protobuf.FixPrice proto = bisq.offer.protobuf.FixPrice.newBuilder()
                .setPriceQuote(spec.getPriceQuote().toProto(false))
                .build();
        assertEquals(spec, FixPriceSpec.fromProto(proto));
    }
}
