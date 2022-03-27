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

package bisq.social.chat;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

public enum ChannelType implements ProtoEnum {
    PUBLIC,
    PRIVATE;

    @Override
    public bisq.social.protobuf.ChannelType toProto() {
        return bisq.social.protobuf.ChannelType.valueOf(name());
    }

    public static ChannelType fromProto(bisq.social.protobuf.ChannelType proto) {
        return ProtobufUtils.enumFromProto(ChannelType.class, proto.name());
    }


}