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

package bisq.support.notifications;
import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufEnumException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SupportNotificationType implements ProtoEnum {
    MODERATOR_NOTIFICATION("MODERATOR_NOTIFICATION"),
    BISQ_EASY_MEDIATOR_NOTIFICATION("BISQ_EASY_MEDIATOR_NOTIFICATION"),
    MU_SIG_MEDIATOR_NOTIFICATION("MU_SIG_MEDIATOR_NOTIFICATION");

    private final String name;

    @Override
    public bisq.support.protobuf.SupportNotificationType toProtoEnum() {
        return bisq.support.protobuf.SupportNotificationType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static SupportNotificationType fromProtoEnum(bisq.support.protobuf.SupportNotificationType proto) {
        try {
            return ProtobufUtils.enumFromProto(SupportNotificationType.class, proto.name());
        } catch (UnresolvableProtobufEnumException e) {
            throw new RuntimeException(e);
        }
    }
}
