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

@Getter
@ToString
@EqualsAndHashCode
public final class MinMaxAmountSpec implements AmountSpec {
    private final long baseSideMinAmount;
    private final long baseSideMaxAmount;
    private final long quoteSideMinAmount;
    private final long quoteSideMaxAmount;

    public MinMaxAmountSpec(long baseSideMinAmount, long baseSideMaxAmount, long quoteSideMinAmount, long quoteSideMaxAmount) {
        this.baseSideMinAmount = baseSideMinAmount;
        this.baseSideMaxAmount = baseSideMaxAmount;
        this.quoteSideMinAmount = quoteSideMinAmount;
        this.quoteSideMaxAmount = quoteSideMaxAmount;
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto() {
        return getAmountSpecBuilder().setMinMaxAmountSpec(bisq.offer.protobuf.MinMaxAmountSpec.newBuilder()
                        .setBaseSideMinAmount(baseSideMinAmount)
                        .setBaseSideMaxAmount(baseSideMaxAmount)
                        .setQuoteSideMinAmount(quoteSideMinAmount)
                        .setQuoteSideMaxAmount(quoteSideMaxAmount))
                .build();
    }

    public static MinMaxAmountSpec fromProto(bisq.offer.protobuf.AmountSpec proto) {
        bisq.offer.protobuf.MinMaxAmountSpec minMaxAmountSpec = proto.getMinMaxAmountSpec();
        return new MinMaxAmountSpec(minMaxAmountSpec.getBaseSideMinAmount(),
                minMaxAmountSpec.getBaseSideMaxAmount(),
                minMaxAmountSpec.getQuoteSideMinAmount(),
                minMaxAmountSpec.getQuoteSideMaxAmount()
        );
    }
}