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

public enum NotificationSetting implements ProtoEnum {
    ALL,
    MENTION,
    NEVER;

    @Override
    public bisq.social.protobuf.NotificationSetting toProto() {
        return bisq.social.protobuf.NotificationSetting.valueOf(name());
    }

    public static NotificationSetting fromProto(bisq.social.protobuf.NotificationSetting proto) {
        return ProtobufUtils.enumFromProto(NotificationSetting.class, proto.name());
    }
}
