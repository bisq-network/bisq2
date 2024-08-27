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

import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;

public interface AmountSpec extends NetworkProto {
    @Override
    bisq.offer.protobuf.AmountSpec toProto(boolean serializeForHash);

    default bisq.offer.protobuf.AmountSpec.Builder getAmountSpecBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.AmountSpec.newBuilder();
    }

    static AmountSpec fromProto(bisq.offer.protobuf.AmountSpec proto) {
        return switch (proto.getMessageCase()) {
            case FIXEDAMOUNTSPEC -> FixedAmountSpec.fromProto(proto.getFixedAmountSpec());
            case RANGEAMOUNTSPEC -> RangeAmountSpec.fromProto(proto.getRangeAmountSpec());
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
