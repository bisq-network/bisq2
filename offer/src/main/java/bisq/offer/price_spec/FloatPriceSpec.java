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

package bisq.offer.price_spec;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A floating price based on the current market price. The value is the percentage above
 * or below the market price with value between 0 and 1 (=100%).
 */
@Getter
@ToString
@EqualsAndHashCode
public final class FloatPriceSpec implements PriceSpec {
    private final double value;

    public FloatPriceSpec(double value) {
        this.value = value;
    }

    public static FloatPriceSpec fromProto(bisq.offer.protobuf.FloatPrice proto) {
        return new FloatPriceSpec(proto.getValue());
    }

    @Override
    public bisq.offer.protobuf.PriceSpec toProto() {
        return getPriceSpecBuilder().setFloatPrice(bisq.offer.protobuf.FloatPrice.newBuilder()
                        .setValue(value))
                .build();
    }
}