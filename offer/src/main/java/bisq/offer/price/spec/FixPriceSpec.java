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

    // Runs in the constructor and therefore also at wire deserialization (fromProto), like
    // FloatPriceSpec's bounds check. The wire carries the quote's value and both monetaries
    // independently, while honest construction derives the value from the pair and uses the
    // canonical monetary factories - so any legitimately created offer passes, and an
    // inconsistent or forged quote dies at the trust boundary before it can diverge display
    // from contract amounts.
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
        // Canonical metadata FIRST: the consistency recomputation below feeds
        // baseSideMonetary.getPrecision() into movePointRight, so a forged precision would blow
        // up the BigDecimal expansion before it could be rejected. The canonical check bounds
        // the precision to the factory value cheaply.
        verifyCanonicalMonetary(baseSideMonetary);
        verifyCanonicalMonetary(quoteSideMonetary);
        // Same derivation as PriceQuote.from, but overflow-safe: from() ends in
        // BigDecimal.longValue(), whose truncation could make an extreme monetary pair collide
        // with the carried value.
        BigDecimal exactDerivedValue = BigDecimal.valueOf(quoteSideMonetary.getValue())
                .movePointRight(baseSideMonetary.getPrecision())
                .divide(BigDecimal.valueOf(baseSideMonetary.getValue()), RoundingMode.HALF_UP);
        checkArgument(exactDerivedValue.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0,
                "The price derived from the fixed price monetaries overflows");
        checkArgument(exactDerivedValue.longValueExact() == priceQuote.getValue(),
                "The fixed price value %s does not match the value %s derived from its monetaries",
                priceQuote.getValue(), exactDerivedValue);
    }

    // A monetary whose metadata deviates from the canonical factory form (e.g. an 8-decimal
    // USD amount) corrupts every downstream comparison against factory-built amounts.
    private static void verifyCanonicalMonetary(Monetary monetary) {
        Monetary canonical = Asset.isFiat(monetary.getCode())
                ? Fiat.fromValue(monetary.getValue(), monetary.getCode())
                : Coin.fromValue(monetary.getValue(), monetary.getCode());
        checkArgument(monetary.getClass() == canonical.getClass(),
                "The monetary type of %s must be %s", monetary.getCode(), canonical.getClass().getSimpleName());
        checkArgument(monetary.getId().equals(canonical.getId()),
                "The monetary id %s of %s is not the canonical id %s",
                monetary.getId(), monetary.getCode(), canonical.getId());
        checkArgument(monetary.getPrecision() == canonical.getPrecision(),
                "The precision %s of %s is not the canonical precision %s",
                monetary.getPrecision(), monetary.getCode(), canonical.getPrecision());
        checkArgument(monetary.getLowPrecision() == canonical.getLowPrecision(),
                "The low precision %s of %s is not the canonical low precision %s",
                monetary.getLowPrecision(), monetary.getCode(), canonical.getLowPrecision());
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

    public static FixPriceSpec fromProto(bisq.offer.protobuf.FixPrice proto) {
        return new FixPriceSpec(PriceQuote.fromProto(proto.getPriceQuote()));
    }
}