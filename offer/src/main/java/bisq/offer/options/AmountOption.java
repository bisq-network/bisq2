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

package bisq.offer.options;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class AmountOption implements OfferOption {
    private final double minAmountAsPercentage;

    public AmountOption(double minAmountAsPercentage) {
        this.minAmountAsPercentage = minAmountAsPercentage;
    }

    public bisq.offer.protobuf.OfferOption toProto() {
        return getOfferOptionBuilder().setAmountOption(bisq.offer.protobuf.AmountOption.newBuilder()
                        .setMinAmountAsPercentage(minAmountAsPercentage))
                .build();
    }

    public static AmountOption fromProto(bisq.offer.protobuf.AmountOption proto) {
        return new AmountOption(proto.getMinAmountAsPercentage());
    }
}