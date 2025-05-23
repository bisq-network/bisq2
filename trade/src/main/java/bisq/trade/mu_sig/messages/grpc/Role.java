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

package bisq.trade.mu_sig.messages.grpc;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import lombok.Getter;

//TODO same as TradeRole
@Getter
public enum Role implements ProtoEnum {
    BUYER_AS_TAKER(true, true),
    BUYER_AS_MAKER(true, false),
    SELLER_AS_TAKER(false, true),
    SELLER_AS_MAKER(false, false);

    private final boolean isBuyer;
    private final boolean isTaker;

    Role(boolean isBuyer, boolean isTaker) {
        this.isBuyer = isBuyer;
        this.isTaker = isTaker;
    }

    @Override
    public bisq.trade.protobuf.Role toProtoEnum() {
        return bisq.trade.protobuf.Role.valueOf(getProtobufEnumPrefix() + name());
    }

    public static Role fromProto(bisq.trade.protobuf.Role proto) {
        return ProtobufUtils.enumFromProto(Role.class, proto.name(), BUYER_AS_TAKER);
    }

    public boolean isMaker() {
        return !isTaker;
    }

    public boolean isSeller() {
        return !isBuyer;
    }

}
