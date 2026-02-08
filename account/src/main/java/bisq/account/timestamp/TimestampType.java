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

package bisq.account.timestamp;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

public enum TimestampType implements ProtoEnum {
    BISQ1_IMPORTED,
    BISQ2_NEW;

    @Override
    public bisq.account.protobuf.TimestampType toProtoEnum() {
        return bisq.account.protobuf.TimestampType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static TimestampType fromProto(bisq.account.protobuf.TimestampType proto) {
        return ProtobufUtils.enumFromProto(TimestampType.class, proto.name(), BISQ2_NEW);
    }
}
