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

package bisq.identity.profile;

import bisq.common.proto.Proto;
import bisq.identity.Identity;
import bisq.network.NetworkIdWithKeyPair;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Local user profile. Not shared over network.
 */
@EqualsAndHashCode
@Getter
public final class ChatUserIdentity implements Proto {
    private final Identity identity;
    private final PublicUserProfile publicUserProfile;

    public ChatUserIdentity(Identity identity, PublicUserProfile publicUserProfile) {
        this.identity = identity;
        this.publicUserProfile = publicUserProfile;
    }

    public bisq.identity.protobuf.ChatUserIdentity toProto() {
        return bisq.identity.protobuf.ChatUserIdentity.newBuilder()
                .setIdentity(identity.toProto())
                .setChatUser(publicUserProfile.toProto())
                .build();
    }

    public static ChatUserIdentity fromProto(bisq.identity.protobuf.ChatUserIdentity proto) {
        return new ChatUserIdentity(Identity.fromProto(proto.getIdentity()),
                PublicUserProfile.fromProto(proto.getChatUser()));
    }

    // Delegates
    public byte[] getPubKeyHash() {
        return publicUserProfile.getPubKeyHash();
    }

    public String getId() {
        return publicUserProfile.getId();
    }

    public String getProfileId() {
        return publicUserProfile.getNym();
    }

    public String getNickName() {
        return publicUserProfile.getNickName();
    }

    public NetworkIdWithKeyPair getNodeIdAndKeyPair() {
        return identity.getNodeIdAndKeyPair();
    }
}