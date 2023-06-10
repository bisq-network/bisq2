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

package bisq.offer.amount;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MinMaxQuoteAmountSpec extends MinMaxAmountSpec implements QuoteAmountSpec {
    public MinMaxQuoteAmountSpec(long minAmount, long maxAmount) {
        super(minAmount, maxAmount);
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto() {
        return getAmountSpecBuilder().setMinMaxQuoteAmountSpec(bisq.offer.protobuf.MinMaxQuoteAmountSpec.newBuilder()
                        .setMinAmount(minAmount)
                        .setMaxAmount(maxAmount))
                .build();
    }

    public static MinMaxQuoteAmountSpec fromProto(bisq.offer.protobuf.AmountSpec proto) {
        return new MinMaxQuoteAmountSpec(proto.getMinMaxQuoteAmountSpec().getMinAmount(),
                proto.getMinMaxQuoteAmountSpec().getMaxAmount()
        );
    }
}