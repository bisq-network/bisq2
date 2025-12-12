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

package bisq.user.contact_list;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;

public enum ContactReason implements ProtoEnum {
    PRIVATE_CHAT,
    BISQ_EASY_TRADE,
    MUSIG_TRADE,
    MANUALLY_ADDED;

    @Override
    public bisq.user.protobuf.ContactReason toProtoEnum() {
        return bisq.user.protobuf.ContactReason.valueOf(getProtobufEnumPrefix() + name());
    }

    public static ContactReason fromProto(bisq.user.protobuf.ContactReason proto) {
        return ProtobufUtils.enumFromProto(ContactReason.class, proto.name(), MANUALLY_ADDED);
    }

    public String getDisplayString() {
        return Res.get("contactsList.contactReason." + name());
    }
}