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

package bisq.social.user.profile;

import bisq.common.proto.Proto;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Local user profile. Not shared over network.
 */
@EqualsAndHashCode
@Getter
public class ChatUserIdentity implements Proto {
    private final Identity identity;
    private final ChatUser chatUser;

    public ChatUserIdentity(Identity identity, ChatUser chatUser) {
        this.identity = identity;
        this.chatUser = chatUser;
    }

    public byte[] getPubKeyHash() {
        return chatUser.getPubKeyHash();
    }

    public String getTooltipString() {
        return Res.get("social.chatUser.tooltip", chatUser.getNickName(), getProfileId());
    }

    public bisq.social.protobuf.UserProfile toProto() {
        return bisq.social.protobuf.UserProfile.newBuilder()
                .setIdentity(identity.toProto())
                .setChatUser(chatUser.toProto())
                .build();
    }

    public static ChatUserIdentity fromProto(bisq.social.protobuf.UserProfile proto) {
        return new ChatUserIdentity(Identity.fromProto(proto.getIdentity()),
                ChatUser.fromProto(proto.getChatUser()));
    }

    public String getProfileId() {
        return chatUser.getProfileId();
    }

    public String getNickName() {
        return chatUser.getNickName();
    }
}