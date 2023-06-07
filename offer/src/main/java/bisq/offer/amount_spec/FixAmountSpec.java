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

package bisq.offer.amount_spec;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * No min. amount supported
 */
@Getter
@ToString
@EqualsAndHashCode
public final class FixAmountSpec implements AmountSpec {
    private final long baseSideAmount;
    private final long quoteSideAmount;

    public FixAmountSpec(long baseSideAmount, long quoteSideAmount) {
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto() {
        return getAmountSpecBuilder().setFixAmountSpec(bisq.offer.protobuf.FixAmountSpec.newBuilder()
                        .setBaseSideAmount(baseSideAmount)
                        .setQuoteSideAmount(quoteSideAmount))
                .build();
    }

    public static FixAmountSpec fromProto(bisq.offer.protobuf.AmountSpec proto) {
        bisq.offer.protobuf.FixAmountSpec minMaxAmountSpec = proto.getFixAmountSpec();
        return new FixAmountSpec(minMaxAmountSpec.getBaseSideAmount(), minMaxAmountSpec.getQuoteSideAmount());
    }
}