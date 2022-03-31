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

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.proto.Proto;
import bisq.network.NetworkId;
import bisq.security.DigestUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Publicly shared chat user data
 * We cache pubKey hash, id and generated userName.
 * ChatUser is part of the ChatMessage so we have many instances from the same chat user and want to avoid
 * costs from hashing and the userame generation. We could also try to restructure the domain model to avoid that
 * the chat user is part of the message (e.g. use an id and reference to p2p network data for chat user).
 */
@ToString
@EqualsAndHashCode
@Slf4j
public class ChatUser implements Proto {
    private static final transient Map<ByteArray, DerivedData> CACHE = new HashMap<>();
    @Getter
    private final NetworkId networkId;
    @Getter
    private final Set<Entitlement> entitlements;
    private transient final DerivedData derivedData;

    public ChatUser(NetworkId networkId) {
        this(networkId, new HashSet<>());
    }

    public ChatUser(NetworkId networkId, Set<Entitlement> entitlements) {
        this.networkId = networkId;
        this.entitlements = entitlements;
        derivedData = getDerivedData(networkId.getPubKey().publicKey().getEncoded());
    }

    public bisq.social.protobuf.ChatUser toProto() {
        return bisq.social.protobuf.ChatUser.newBuilder()
                .setNetworkId(networkId.toProto())
                .addAllEntitlements(entitlements.stream()
                        .sorted()
                        .map(Entitlement::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static ChatUser fromProto(bisq.social.protobuf.ChatUser proto) {
        Set<Entitlement> set = proto.getEntitlementsList().stream()
                .map(Entitlement::fromProto)
                .collect(Collectors.toSet());
        return new ChatUser(NetworkId.fromProto(proto.getNetworkId()), set);
    }

    public boolean hasEntitlementType(Entitlement.Type type) {
        return entitlements.stream().anyMatch(e -> e.entitlementType() == type);
    }

    // Delegates
    public String getId() {
        return derivedData.id();
    }

    public String getUserName() {
        return derivedData.userName;
    }

    public byte[] getPubKeyHash() {
        return derivedData.pubKeyHash().getBytes();
    }

    public ByteArray getPubKeyHashAsByteArray() {
        return derivedData.pubKeyHash();
    }

    private static DerivedData getDerivedData(byte[] pubKeyBytes) {
        ByteArray mapKey = new ByteArray(pubKeyBytes);
        if (!CACHE.containsKey(mapKey)) {
            byte[] pubKeyHash = DigestUtil.hash(pubKeyBytes);
            String id = Hex.encode(pubKeyHash);
            String userName = UserNameGenerator.fromHash(pubKeyHash);
            DerivedData derivedData = new DerivedData(new ByteArray(pubKeyHash), id, userName);
            CACHE.put(mapKey, derivedData);
        }
        return CACHE.get(mapKey);
    }

    private static record DerivedData(ByteArray pubKeyHash, String id, String userName) {
    }

    public static record BurnInfo(long totalBsqBurned, long firstBurnDate) {
    }
}