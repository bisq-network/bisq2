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

package bisq.user.identity;

import bisq.common.proto.PersistableProto;
import bisq.identity.Identity;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * My local user profile. Not shared over network.
 */
@EqualsAndHashCode
@ToString
@Getter
public final class UserIdentity implements PersistableProto {
    private final Identity identity;
    private final UserProfile userProfile;

    public UserIdentity(Identity identity, UserProfile userProfile) {
        this.identity = identity;
        this.userProfile = userProfile;

        checkArgument(identity.getId().equals(getId()));
    }

    @Override
    public bisq.user.protobuf.UserIdentity.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.UserIdentity.newBuilder()
                .setIdentity(identity.toProto(serializeForHash))
                .setUserProfile(userProfile.toProto(serializeForHash));
    }

    @Override
    public bisq.user.protobuf.UserIdentity toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static UserIdentity fromProto(bisq.user.protobuf.UserIdentity proto) {
        return new UserIdentity(Identity.fromProto(proto.getIdentity()),
                UserProfile.fromProto(proto.getUserProfile()));
    }

    // Delegates
    public byte[] getPubKeyHash() {
        return userProfile.getPubKeyHash();
    }

    public String getId() {
        return userProfile.getId();
    }

    public String getNym() {
        return userProfile.getNym();
    }

    public String getNickName() {
        return userProfile.getNickName();
    }

    public NetworkIdWithKeyPair getNetworkIdWithKeyPair() {
        return identity.getNetworkIdWithKeyPair();
    }

    public String getUserName() {
        return userProfile.getUserName();
    }
}