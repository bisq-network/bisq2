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

package bisq.common.fsm;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import lombok.Getter;

@Getter
public enum ErrorCode implements ProtoEnum {
    UNSPECIFIED,
    TRADE_AMOUNT_VALIDATION_FAILED;

    @Override
    public bisq.common.protobuf.ErrorCode toProtoEnum() {
        return bisq.common.protobuf.ErrorCode.valueOf(getProtobufEnumPrefix() + name());
    }


    public static ErrorCode fromProto(bisq.common.protobuf.ErrorCode proto) {
        return ProtobufUtils.enumFromProto(ErrorCode.class, proto.name(), UNSPECIFIED);
    }


}
