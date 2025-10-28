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

package bisq.trade.exceptions;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import lombok.Getter;

public enum TradeProtocolFailure implements ProtoEnum {
    UNKNOWN(true),
    PRICE_DEVIATION(false),
    NO_MATCHING_OFFER_FOUND(false),
    MEDIATORS_NOT_MATCHING(false);

    @Getter
    private final boolean isUnexpected;

    TradeProtocolFailure(boolean isUnexpected) {
        this.isUnexpected = isUnexpected;
    }

    @Override
    public bisq.trade.protobuf.TradeProtocolFailure toProtoEnum() {
        return bisq.trade.protobuf.TradeProtocolFailure.valueOf(getProtobufEnumPrefix() + name());
    }

    public static TradeProtocolFailure fromProto(bisq.trade.protobuf.TradeProtocolFailure proto) {
        return ProtobufUtils.enumFromProto(TradeProtocolFailure.class, proto.name(), UNKNOWN);
    }
}
