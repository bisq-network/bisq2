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
import bisq.network.NetworkId;
import bisq.security.DigestUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Publicly shared chat user data
 * We cache pubKey hash, id and generated userName.
 */
public class ChatUser implements Serializable {
    private static final Map<ByteArray, DerivedData> CACHE = new HashMap<>();

    @Getter
    private final NetworkId networkId;
    @Getter
    private final Set<Entitlement> entitlements;
    private final DerivedData derivedData;

    public ChatUser(NetworkId networkId, Set<Entitlement> entitlements) {
        this.networkId = networkId;
        this.entitlements = entitlements;
        byte[] pubKey = networkId.getPubKey().publicKey().getEncoded();
        derivedData = getDerivedData(pubKey);
    }

    public ChatUser(NetworkId networkId) {
        this(networkId, new HashSet<>());
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
        return derivedData.pubKeyHash();
    }

    private static DerivedData getDerivedData(byte[] pubKey) {
        ByteArray key = new ByteArray(pubKey);
        if (!CACHE.containsKey(key)) {
            byte[] pubKeyHash = DigestUtil.hash(pubKey);
            String id = Hex.encode(pubKeyHash);
            String userName = UserNameGenerator.fromHash(pubKeyHash);
            DerivedData derivedData = new DerivedData(pubKeyHash, id, userName);
            CACHE.put(key, derivedData);
        }
        return CACHE.get(key);
    }

    private static record DerivedData(byte[] pubKeyHash, String id, String userName) implements Serializable {
    }

    public static record BurnInfo(long totalBsqBurned, long firstBurnDate) implements Serializable {
    }
}