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

/**
 * No min. amount supported
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class QuoteSideFixedAmountSpec extends FixedAmountSpec implements QuoteSideAmountSpec {
    public QuoteSideFixedAmountSpec(long amount) {
        super(amount);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.offer.protobuf.AmountSpec.Builder getBuilder(boolean serializeForHash) {
        return getAmountSpecBuilder(serializeForHash).setFixedAmountSpec(
                getFixedAmountSpecBuilder(serializeForHash).setQuoteSideFixedAmountSpec(
                        bisq.offer.protobuf.QuoteSideFixedAmountSpec.newBuilder()));
    }

    @Override
    public bisq.offer.protobuf.AmountSpec toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static QuoteSideFixedAmountSpec fromProto(bisq.offer.protobuf.FixedAmountSpec proto) {
        return new QuoteSideFixedAmountSpec(proto.getAmount());
    }
}