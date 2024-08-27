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

import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public abstract class RangeAmountSpec implements AmountSpec {
    protected final long minAmount;
    protected final long maxAmount;

    public RangeAmountSpec(long minAmount, long maxAmount) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    @Override
    public void verify() {
        checkArgument(minAmount > 0);
        checkArgument(maxAmount > 0);
        checkArgument(maxAmount >= minAmount);
    }

    public bisq.offer.protobuf.RangeAmountSpec.Builder getRangeAmountSpecBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.RangeAmountSpec.newBuilder()
                .setMinAmount(minAmount)
                .setMaxAmount(maxAmount);
    }

    static RangeAmountSpec fromProto(bisq.offer.protobuf.RangeAmountSpec proto) {
        return switch (proto.getMessageCase()) {
            case BASESIDERANGEAMOUNTSPEC -> BaseSideRangeAmountSpec.fromProto(proto);
            case QUOTESIDERANGEAMOUNTSPEC -> QuoteSideRangeAmountSpec.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}