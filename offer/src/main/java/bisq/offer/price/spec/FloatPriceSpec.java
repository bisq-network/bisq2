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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A floating price based on the current market price.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class FloatPriceSpec implements PriceSpec {
    private final double percentage;

    /**
     * @param percentage The percentage value normalized to 1 (1 = 100%) above or below the market price.
     *                   Positive value means higher than market price.
     *                   E.g. 0.1 means `marketPrice * 1.1`, -0.2 means `marketPrice * 0.8`
     */
    public FloatPriceSpec(double percentage) {
        this.percentage = percentage;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(percentage >= -1 && percentage <= 1,
                "Percentage must be in the range of -100% - 100%");
    }

    @Override
    public bisq.offer.protobuf.PriceSpec.Builder getBuilder(boolean serializeForHash) {
        return getPriceSpecBuilder(serializeForHash)
                .setFloatPrice(bisq.offer.protobuf.FloatPrice.newBuilder()
                        .setPercentage(percentage));
    }

    @Override
    public bisq.offer.protobuf.PriceSpec toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static FloatPriceSpec fromProto(bisq.offer.protobuf.FloatPrice proto) {
        return new FloatPriceSpec(proto.getPercentage());
    }
}