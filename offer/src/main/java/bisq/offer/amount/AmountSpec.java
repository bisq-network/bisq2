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

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;

public interface AmountSpec extends Proto {

    bisq.offer.protobuf.AmountSpec toProto();

    default bisq.offer.protobuf.AmountSpec.Builder getAmountSpecBuilder() {
        return bisq.offer.protobuf.AmountSpec.newBuilder();
    }

    static AmountSpec fromProto(bisq.offer.protobuf.AmountSpec proto) {
        switch (proto.getMessageCase()) {
            case FIXBASEAMOUNTSPEC: {
                return FixBaseAmountSpec.fromProto(proto);
            }
            case FIXQUOTEAMOUNTSPEC: {
                return FixQuoteAmountSpec.fromProto(proto);
            }
            case MINMAXBASEAMOUNTSPEC: {
                return MinMaxBaseAmountSpec.fromProto(proto);
            }
            case MINMAXQUOTEAMOUNTSPEC: {
                return MinMaxQuoteAmountSpec.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
