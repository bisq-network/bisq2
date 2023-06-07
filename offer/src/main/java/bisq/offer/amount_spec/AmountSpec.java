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

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;

import static com.google.common.base.Preconditions.checkArgument;

public interface AmountSpec extends Proto {
    static AmountSpec fromMinMaxAmounts(boolean isMinAmountEnabled,
                                        long baseSideMinAmount,
                                        long baseSideMaxAmount,
                                        long quoteSideMinAmount,
                                        long quoteSideMaxAmount) {
        if (isMinAmountEnabled && baseSideMinAmount != baseSideMaxAmount) {
            checkArgument(quoteSideMinAmount != quoteSideMaxAmount,
                    "There is a mismatch of quoteSide min/max amounts and baseSide min/max amounts.");
            return new MinMaxAmountSpec(baseSideMinAmount, baseSideMaxAmount, quoteSideMinAmount, quoteSideMaxAmount);
        } else {
            return new FixAmountSpec(baseSideMaxAmount, quoteSideMaxAmount);
        }
    }


    bisq.offer.protobuf.AmountSpec toProto();

    default bisq.offer.protobuf.AmountSpec.Builder getAmountSpecBuilder() {
        return bisq.offer.protobuf.AmountSpec.newBuilder();
    }

    static AmountSpec fromProto(bisq.offer.protobuf.AmountSpec proto) {
        switch (proto.getMessageCase()) {
            case FIXAMOUNTSPEC: {
                return FixAmountSpec.fromProto(proto);
            }
            case MINMAXAMOUNTSPEC: {
                return MinMaxAmountSpec.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
