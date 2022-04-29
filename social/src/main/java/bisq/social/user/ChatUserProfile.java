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

package bisq.social.user;

import bisq.common.encoding.Hex;
import bisq.common.proto.Proto;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.security.DigestUtil;
import bisq.social.user.entitlement.Role;
import bisq.social.user.profile.NymGenerator;
import bisq.social.user.reputation.Reputation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

//todo publish to network
/**
 * Publicly shared chat user profile.
 */
@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public class ChatUserProfile implements Proto {
    private final String nickName;
    private final NetworkId networkId;
    private final Set<Reputation> reputation;
    private final Set<Role> roles;
    private transient final byte[] pubKeyHash;
    private transient final String id;
    private transient final String nym;

    public ChatUserProfile(String nickName, NetworkId networkId) {
        this(nickName, networkId, new HashSet<>(), new HashSet<>());
    }

    public ChatUserProfile(String nickName, NetworkId networkId, Set<Reputation> reputation, Set<Role> roles) {
        this.nickName = nickName;
        this.networkId = networkId;
        this.reputation = reputation;
        this.roles = roles;

        pubKeyHash = DigestUtil.hash(networkId.getPubKey().publicKey().getEncoded());
        id = Hex.encode(pubKeyHash);
        nym = NymGenerator.fromHash(pubKeyHash);
    }

    public bisq.social.protobuf.ChatUserProfile toProto() {
        return bisq.social.protobuf.ChatUserProfile.newBuilder()
                .setNickName(nickName)
                .setNetworkId(networkId.toProto())
                .addAllReputation(reputation.stream()
                        .sorted()
                        .map(Reputation::toProto)
                        .collect(Collectors.toList()))
                .addAllRoles(roles.stream()
                        .sorted()
                        .map(Role::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static ChatUserProfile fromProto(bisq.social.protobuf.ChatUserProfile proto) {
        Set<Reputation> reputation = proto.getReputationList().stream()
                .map(Reputation::fromProto)
                .collect(Collectors.toSet());
        Set<Role> roles = proto.getRolesList().stream()
                .map(Role::fromProto)
                .collect(Collectors.toSet());
        return new ChatUserProfile(proto.getNickName(), NetworkId.fromProto(proto.getNetworkId()), reputation, roles);
    }

    public boolean hasEntitlementType(Role.Type type) {
        return roles.stream().anyMatch(e -> e.type() == type);
    }

    public String getTooltipString() {
        return Res.get("social.chatUser.tooltip", nickName, nym);
    }

    public String getUserName() {
        return NickNameLookup.getUserName(nym, nickName);
    }

    //todo
    public static record BurnInfo(long totalBsqBurned, long firstBurnDate) {
    }
}