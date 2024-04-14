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

import bisq.common.monetary.PriceQuote;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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

    @Override
    public void verify() {
    }

    @Override
    public bisq.offer.protobuf.PriceSpec.Builder getBuilder(boolean serializeForHash) {
        return getPriceSpecBuilder(serializeForHash)
                .setFixPrice(bisq.offer.protobuf.FixPrice.newBuilder()
                        .setPriceQuote(priceQuote.toProto(serializeForHash)));
    }

    @Override
    public bisq.offer.protobuf.PriceSpec toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static FixPriceSpec fromProto(bisq.offer.protobuf.FixPrice proto) {
        return new FixPriceSpec(PriceQuote.fromProto(proto.getPriceQuote()));
    }
}