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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The wire carries the quote's value and both monetaries independently. FixPriceSpec.verify() runs in the constructor
 * and therefore also at deserialization. It accepts every fixed price which a released client creates, including the
 * shapes of Bisq Connect (see docs/specifications/offer/fixed-price.md).
 */
public class FixPriceSpecTest {
    private static final Monetary ONE_BTC = Coin.asBtcFromFaceValue(1.0);
    // The bound takes milliseconds. Without it, the recomputation with the huge precision below
    // takes about 44 s (measured in the Gradle test worker on JDK 21).
    private static final Duration PRECISION_BOUND_TIME_LIMIT = Duration.ofSeconds(5);
    private static final String HUGE_PRECISION_MESSAGE =
            "The precision 100000000 of BTC is not the canonical precision 8";

    // The fixed prices which Bisq Connect sends through the API of a node, from PriceQuoteVOFactory.fromPrice of
    // bisq-mobile 7f9cddce7b (also connect_0.9.0): the base side id is BTC, not BTC [crypto], and the value is the quote
    // side amount divided with 8 significant digits. M1: 64,321.12 USD. M2: 1,234,567.89 ARS, value rounded.
    // M3: 64,321.1234 USD, value rounded.
    private static final PriceQuote MOBILE_M1 = mobileQuote(643_211_200L, "USD", 643_211_200L);
    private static final PriceQuote MOBILE_M2 = mobileQuote(12_345_679_000L, "ARS", 12_345_678_900L);
    private static final PriceQuote MOBILE_M3 = mobileQuote(643_211_230L, "USD", 643_211_234L);

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
        // The derived value rounds (HALF_UP) for non-exact ratios.
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

    // Nodes of version 2.1.13 store these quotes in trades, trade channels and mediation cases, which must load (rule
    // 4 of docs/specifications/offer/fixed-price.md)
    @Test
    public void quotesOfBisqConnectAreAcceptedAtProtoDeserialization() throws Exception {
        for (PriceQuote mobileQuote : List.of(MOBILE_M1, MOBILE_M2, MOBILE_M3)) {
            byte[] bytes = fixPriceProto(mobileQuote).toByteArray();

            FixPriceSpec spec = FixPriceSpec.fromProto(bisq.offer.protobuf.FixPrice.parseFrom(bytes));

            // The node keeps the received fields, so every hash of the object, for example of a contract which both
            // traders signed with version 2.1.13, stays the same
            assertArrayEquals(bytes, fixPriceProto(spec.getPriceQuote()).toByteArray());
            assertEquals(mobileQuote.getValue(), spec.getPriceQuote().getValue());
        }
    }

    // The API publishes a canonical fixed price: the amounts of the canonical factories, and the value derived from
    // them. The quote side amount is the price which the user entered; the client's value is rounded.
    @Test
    public void quoteOfAClientIsNormalized() {
        assertNormalized(MOBILE_M1, 643_211_200L);
        assertNormalized(MOBILE_M2, 12_345_678_900L);
        assertNormalized(MOBILE_M3, 643_211_234L);
    }

    private static void assertNormalized(PriceQuote clientQuote, long expectedValue) {
        PriceQuote normalized = FixPriceSpec.fromClientQuote(clientQuote).getPriceQuote();

        assertEquals(expectedValue, normalized.getValue());
        assertEquals(Coin.asBtcFromFaceValue(1.0), normalized.getBaseSideMonetary());
        String quoteCode = clientQuote.getQuoteSideMonetary().getCode();
        assertEquals(Fiat.fromValue(clientQuote.getQuoteSideMonetary().getValue(), quoteCode),
                normalized.getQuoteSideMonetary());
        assertEquals(PriceQuote.from(normalized.getBaseSideMonetary(), normalized.getQuoteSideMonetary()), normalized);
    }

    // An offer in the offerbook needs the canonical form (rule 5 of docs/specifications/offer/fixed-price.md): Bisq
    // Connect shows the price from the quote side amount, while nodes convert the amounts with the value
    @Test
    public void canonicalRulesRejectTheQuotesOfBisqConnect() {
        assertEquals("The monetary id BTC of BTC is not the canonical id BTC [crypto]",
                assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(MOBILE_M1).verifyCanonical())
                        .getMessage());
        for (PriceQuote roundedValue : List.of(MOBILE_M2, MOBILE_M3)) {
            PriceQuote canonicalIds = new PriceQuote(roundedValue.getValue(),
                    ONE_BTC,
                    roundedValue.getQuoteSideMonetary());
            assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(canonicalIds).verifyCanonical());
        }
    }

    @Test
    public void canonicalRulesRejectAForgedIdAndAValueWhichDoesNotMatchTheAmounts() {
        PriceQuote forgedId = PriceQuote.from(ONE_BTC, new Fiat("EVIL", 100_000_0000L, "USD", 4, 2));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(forgedId).verifyCanonical());
        PriceQuote honest = PriceQuote.fromFiatPrice(100_000, "USD");
        PriceQuote otherValue = new PriceQuote(honest.getValue() + 1,
                honest.getBaseSideMonetary(),
                honest.getQuoteSideMonetary());
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(otherValue).verifyCanonical());
    }

    @Test
    public void canonicalRulesRejectAnOverflowingDerivedValue() {
        // The exact derived value of this pair overflows long; its truncated low bits collide with the carried value,
        // so a lossy recomputation would accept an inconsistent triple (the pair encodes 18,446,744 USD per sat while
        // the value reads 9,044 USD per BTC).
        PriceQuote malformed = new PriceQuote(90_448_384L,
                Coin.fromValue(1L, "BTC"),
                Fiat.fromValue(184_467_440_738L, "USD"));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed).verifyCanonical());
    }

    @Test
    public void canonicalRulesAcceptHonestAndNormalizedQuotes() {
        new FixPriceSpec(PriceQuote.fromFiatPrice(100_000, "USD")).verifyCanonical();
        new FixPriceSpec(PriceQuote.fromAltCoinPrice(0.0025, "XMR")).verifyCanonical();
        new FixPriceSpec(PriceQuote.from(Coin.fromFaceValue(3.0, "XMR"), Coin.asBtcFromFaceValue(0.01)))
                .verifyCanonical();
        for (PriceQuote mobileQuote : List.of(MOBILE_M1, MOBILE_M2, MOBILE_M3)) {
            FixPriceSpec.fromClientQuote(mobileQuote).verifyCanonical();
        }
    }

    @Test
    public void quoteOfAClientWithAForgedPrecisionIsRejected() {
        PriceQuote forged = new PriceQuote(643_211_200L,
                new Coin("BTC", 100_000_000L, "BTC", 8, 4),
                new Fiat("USD", 643_211_200_0000L, "USD", 8, 2));
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(forged));
    }

    @Test
    public void wrongMonetaryTypeForTheCodeIsRejected() {
        Monetary coinTypedUsd = new Coin("USD", 100_000_0000L, "USD", 4, 4);
        PriceQuote malformed = PriceQuote.from(ONE_BTC, coinTypedUsd);
        assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed));
    }

    // Invariant: the node bounds a peer-supplied precision before any arithmetic whose cost grows
    // with that precision. A recomputation of the value with movePointRight(100_000_000) would build a
    // number of about 332 million bits. A change of the bound may update the expected message, but
    // must keep the time limit. The base side id of Bisq Connect, which parsing accepts, must not
    // bypass the bound.
    @Test
    public void hugeForgedPrecisionIsRejectedBeforeTheArithmetic() {
        for (String baseSideId : List.of("BTC [crypto]", "BTC")) {
            PriceQuote malformed = quoteWithHugeBasePrecision(baseSideId);
            IllegalArgumentException exception = assertTimeoutPreemptively(PRECISION_BOUND_TIME_LIMIT,
                    () -> assertThrows(IllegalArgumentException.class, () -> new FixPriceSpec(malformed)));
            assertEquals(HUGE_PRECISION_MESSAGE, exception.getMessage());
        }
    }

    @Test
    public void hugeForgedPrecisionIsRejectedBeforeTheArithmeticAtProtoDeserialization() {
        // Covers the whole parse path from the wire, so that precision-dependent work added
        // before verify() also fails the time limit.
        bisq.offer.protobuf.FixPrice proto = bisq.offer.protobuf.FixPrice.newBuilder()
                .setPriceQuote(quoteWithHugeBasePrecision("BTC [crypto]").toProto(false))
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
    private static PriceQuote quoteWithHugeBasePrecision(String baseSideId) {
        Monetary forgedBase = new Coin(baseSideId, 1L, "BTC", 100_000_000, 4);
        return new PriceQuote(90_000_0000L, forgedBase, Fiat.fromFaceValue(90_000, "USD"));
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

    private static PriceQuote mobileQuote(long value, String quoteCode, long quoteSideAmount) {
        return new PriceQuote(value,
                new Coin("BTC", 100_000_000L, "BTC", 8, 4),
                new Fiat(quoteCode, quoteSideAmount, quoteCode, 4, 2));
    }

    private static bisq.offer.protobuf.FixPrice fixPriceProto(PriceQuote priceQuote) {
        return bisq.offer.protobuf.FixPrice.newBuilder()
                .setPriceQuote(priceQuote.toProto(false))
                .build();
    }
}
