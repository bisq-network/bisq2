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

package bisq.offer;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;

public enum Direction implements ProtoEnum {
    BUY,
    SELL;

    @Override
    public bisq.offer.protobuf.Direction toProtoEnum() {
        return bisq.offer.protobuf.Direction.valueOf(getProtobufEnumPrefix() + name());
    }

    public static Direction fromProto(bisq.offer.protobuf.Direction proto) {
        return ProtobufUtils.enumFromProto(Direction.class, proto.name(), BUY);
    }

    public boolean isBuy() {
        return this == BUY;
    }

    public boolean isSell() {
        return this == SELL;
    }

    public Direction mirror() {
        return isBuy() ? SELL : BUY;
    }

    public String getDisplayString() {
        return Res.get("offer." + name().toLowerCase());
    }

    public String getDisplayStringForTraderPair() {
        return isBuy() ? Res.get("bisqEasy.openTrades.table.direction.buyer") :
                Res.get("bisqEasy.openTrades.table.direction.seller");
    }
}
