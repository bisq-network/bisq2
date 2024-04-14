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

package bisq.offer.amount.spec;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BaseSideRangeAmountSpec extends RangeAmountSpec implements BaseSideAmountSpec {
    public BaseSideRangeAmountSpec(long minAmount, long maxAmount) {
        super(minAmount, maxAmount);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.offer.protobuf.AmountSpec.Builder getBuilder(boolean serializeForHash) {
        return getAmountSpecBuilder(serializeForHash).setRangeAmountSpec(
                getRangeAmountSpecBuilder(serializeForHash).setBaseSideRangeAmountSpec(
                        bisq.offer.protobuf.BaseSideRangeAmountSpec.newBuilder()));
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BaseSideRangeAmountSpec fromProto(bisq.offer.protobuf.RangeAmountSpec proto) {
        return new BaseSideRangeAmountSpec(proto.getMinAmount(), proto.getMaxAmount());
    }
}