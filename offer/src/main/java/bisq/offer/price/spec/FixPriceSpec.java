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

import bisq.common.asset.Asset;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fix price defined as a long value.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class FixPriceSpec implements PriceSpec {
    private final PriceQuote priceQuote;

    public FixPriceSpec(PriceQuote priceQuote) {
        this.priceQuote = priceQuote;

        verify();
    }

    /**
     * A fixed price from the quote of a client, for example of the API. The amounts are built with the canonical
     * factories from their codes and values, and the value of the quote is derived from them. The value which the
     * client sent is not used: the quote side amount is the price which the user entered, and released clients round
     * the value (see docs/specifications/offer/fixed-price.md, rule 6).
     */
    public static FixPriceSpec fromClientQuote(PriceQuote clientQuote) {
        Monetary baseSideMonetary = toCanonicalMonetary(clientQuote.getBaseSideMonetary());
        Monetary quoteSideMonetary = toCanonicalMonetary(clientQuote.getQuoteSideMonetary());
        checkArgument(baseSideMonetary.getValue() > 0,
                "The base side amount of the fixed price must be positive but was %s", baseSideMonetary.getValue());
        return new FixPriceSpec(PriceQuote.from(baseSideMonetary, quoteSideMonetary));
    }

    // Runs in the constructor and therefore also at wire deserialization (fromProto), also for the offer of a stored
    // trade, trade channel or mediation case. It checks what every released creator of a fixed price satisfies (rule 3
    // of docs/specifications/network/validation-at-parsing.md): positive values, and amounts of the canonical type and
    // precisions. The precisions bound the cost of the price arithmetic and keep the amount conversions in the units of
    // the currency. Bisq Connect clients send the id BTC and a value rounded to 8 significant digits through API nodes
    // of version 2.1.13, so the id and the exact value are checked only for an offer in an offerbook, by
    // verifyCanonical (see docs/specifications/offer/fixed-price.md, rules 4 and 5).
    @Override
    public void verify() {
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkArgument(priceQuote.getValue() > 0,
                "The fixed price must be positive but was %s", priceQuote.getValue());
        Monetary baseSideMonetary = priceQuote.getBaseSideMonetary();
        Monetary quoteSideMonetary = priceQuote.getQuoteSideMonetary();
        checkArgument(baseSideMonetary.getValue() > 0,
                "The base side amount of the fixed price must be positive but was %s", baseSideMonetary.getValue());
        checkArgument(quoteSideMonetary.getValue() > 0,
                "The quote side amount of the fixed price must be positive but was %s", quoteSideMonetary.getValue());
        verifyCanonicalTypeAndPrecisions(baseSideMonetary);
        verifyCanonicalTypeAndPrecisions(quoteSideMonetary);
    }

    /**
     * The rules for the fixed price of an offer in an offerbook: the amounts have their canonical ids, and the value is
     * derived exactly from the amounts. Nodes convert the amounts of a trade with the value, while API clients such as
     * Bisq Connect show the price from the quote side amount. A value which does not match the amounts would show a
     * taker another price than the trade uses (see docs/specifications/offer/fixed-price.md, rule 5). It runs after
     * verify(), which bounds the precisions before this arithmetic.
     */
    public void verifyCanonical() {
        Monetary baseSideMonetary = priceQuote.getBaseSideMonetary();
        Monetary quoteSideMonetary = priceQuote.getQuoteSideMonetary();
        verifyCanonicalId(baseSideMonetary);
        verifyCanonicalId(quoteSideMonetary);
        // Same derivation as PriceQuote.from, but overflow-safe: from() ends in BigDecimal.longValue(), whose
        // truncation could make an extreme monetary pair collide with the carried value.
        BigDecimal exactDerivedValue = BigDecimal.valueOf(quoteSideMonetary.getValue())
                .movePointRight(baseSideMonetary.getPrecision())
                .divide(BigDecimal.valueOf(baseSideMonetary.getValue()), RoundingMode.HALF_UP);
        checkArgument(exactDerivedValue.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0,
                "The price derived from the fixed price monetaries overflows");
        checkArgument(exactDerivedValue.longValueExact() == priceQuote.getValue(),
                "The fixed price value %s does not match the value %s derived from its monetaries",
                priceQuote.getValue(), exactDerivedValue);
    }

    private static void verifyCanonicalId(Monetary monetary) {
        String canonicalId = toCanonicalMonetary(monetary).getId();
        checkArgument(monetary.getId().equals(canonicalId),
                "The monetary id %s of %s is not the canonical id %s",
                monetary.getId(), monetary.getCode(), canonicalId);
    }

    // A monetary whose precisions deviate from the canonical factory form (e.g. an 8-decimal USD amount) corrupts
    // every amount conversion and comparison against factory-built amounts.
    private static void verifyCanonicalTypeAndPrecisions(Monetary monetary) {
        Monetary canonical = toCanonicalMonetary(monetary);
        checkArgument(monetary.getClass() == canonical.getClass(),
                "The monetary type of %s must be %s", monetary.getCode(), canonical.getClass().getSimpleName());
        checkArgument(monetary.getPrecision() == canonical.getPrecision(),
                "The precision %s of %s is not the canonical precision %s",
                monetary.getPrecision(), monetary.getCode(), canonical.getPrecision());
        checkArgument(monetary.getLowPrecision() == canonical.getLowPrecision(),
                "The low precision %s of %s is not the canonical low precision %s",
                monetary.getLowPrecision(), monetary.getCode(), canonical.getLowPrecision());
    }

    private static Monetary toCanonicalMonetary(Monetary monetary) {
        return Asset.isFiat(monetary.getCode())
                ? Fiat.fromValue(monetary.getValue(), monetary.getCode())
                : Coin.fromValue(monetary.getValue(), monetary.getCode());
    }

    @Override
    public String getDisplayName() {
        return Res.get("priceSpec.fixPriceSpec");
    }

    @Override
    public bisq.offer.protobuf.PriceSpec.Builder getBuilder(boolean serializeForHash) {
        return getPriceSpecBuilder(serializeForHash)
                .setFixPrice(bisq.offer.protobuf.FixPrice.newBuilder()
                        .setPriceQuote(priceQuote.toProto(serializeForHash)));
    }

    @Override
    public bisq.offer.protobuf.PriceSpec toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static FixPriceSpec fromProto(bisq.offer.protobuf.FixPrice proto) throws UnresolvableProtobufException {
        try {
            return new FixPriceSpec(PriceQuote.fromProto(proto.getPriceQuote()));
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }
}