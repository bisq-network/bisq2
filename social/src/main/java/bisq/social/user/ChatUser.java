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
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.PublicTradeChatMessage;
import bisq.social.user.role.Role;
import bisq.social.user.reputation.Reputation;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Publicly shared chat user profile.
 */
@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public class ChatUser implements DistributedData {
    // We give a bit longer TTL than the chat messages to ensure the chat user is available as long the messages are 
    private final static long TTL = Math.round(ChatMessage.TTL * 1.2);
    
    private final String nickName;
    private final NetworkId networkId;
    private final Set<Reputation> reputation;
    private final Set<Role> roles;
    private transient final byte[] pubKeyHash;
    private final MetaData metaData;
    private transient final String id;
    private transient final String nym;

    public ChatUser(String nickName, NetworkId networkId) {
        this(nickName, networkId,
                new HashSet<>(),
                new HashSet<>());
    }

    public ChatUser(String nickName, NetworkId networkId, Set<Reputation> reputation, Set<Role> roles) {
        this(nickName, networkId,
                reputation,
                roles,
                new MetaData(TTL, 100000, PublicTradeChatMessage.class.getSimpleName()));
    }

    public ChatUser(String nickName, NetworkId networkId, Set<Reputation> reputation, Set<Role> roles, MetaData metaData) {
        this.nickName = nickName;
        this.networkId = networkId;
        this.reputation = reputation;
        this.roles = roles;

        pubKeyHash = DigestUtil.hash(networkId.getPubKey().publicKey().getEncoded());
        this.metaData = metaData;
        id = Hex.encode(pubKeyHash);
        nym = NymGenerator.fromHash(pubKeyHash);
    }

    public bisq.social.protobuf.ChatUser toProto() {
        return bisq.social.protobuf.ChatUser.newBuilder()
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
                .setMetaData(metaData.toProto())
                .build();
    }

    public static ChatUser fromProto(bisq.social.protobuf.ChatUser proto) {
        Set<Reputation> reputation = proto.getReputationList().stream()
                .map(Reputation::fromProto)
                .collect(Collectors.toSet());
        Set<Role> roles = proto.getRolesList().stream()
                .map(Role::fromProto)
                .collect(Collectors.toSet());
        return new ChatUser(proto.getNickName(),
                NetworkId.fromProto(proto.getNetworkId()),
                reputation,
                roles,
                MetaData.fromProto(proto.getMetaData()));
    }


    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.ChatUser.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public boolean hasEntitlementType(Role.Type type) {
        return roles.stream().anyMatch(e -> e.type() == type);
    }

    public String getTooltipString() {
        return Res.get("social.chatUser.tooltip", nickName, nym);
    }

    public String getUserName() {
        return NymLookup.getUserName(nym, nickName);
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    public String getBio() {
        return "Trusted trader, 4 year contributor to bisq"; //todo implement instead of hardcode
    }
    
    public String getBurnScoreAsString() {
        return "301"; //todo implement instead of hardcode
    }
    public String getAccountAgeAsString() {
        return "274 days"; //todo implement instead of hardcode
    }

    //todo
    public static record BurnInfo(long totalBsqBurned, long firstBurnDate) {
    }
}