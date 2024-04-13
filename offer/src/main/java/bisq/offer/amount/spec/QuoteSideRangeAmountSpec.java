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
public final class QuoteSideRangeAmountSpec extends RangeAmountSpec implements QuoteSideAmountSpec {
    public QuoteSideRangeAmountSpec(long minAmount, long maxAmount) {
        super(minAmount, maxAmount);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.offer.protobuf.AmountSpec.Builder getBuilder(boolean ignoreAnnotation) {
        return getAmountSpecBuilder(ignoreAnnotation).setRangeAmountSpec(
                getRangeAmountSpecBuilder(ignoreAnnotation).setQuoteSideRangeAmountSpec(
                        bisq.offer.protobuf.QuoteSideRangeAmountSpec.newBuilder()));
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static QuoteSideRangeAmountSpec fromProto(bisq.offer.protobuf.RangeAmountSpec proto) {
        return new QuoteSideRangeAmountSpec(proto.getMinAmount(), proto.getMaxAmount());
    }
}