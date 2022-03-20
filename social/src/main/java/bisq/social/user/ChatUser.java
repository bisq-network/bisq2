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
import bisq.network.NetworkId;
import bisq.security.DigestUtil;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Publicly shared chat user data
 */
//todo use cache for pubKeyHash and userName
public record ChatUser(NetworkId networkId, Set<Entitlement> entitlements) implements Serializable {
    public ChatUser(NetworkId networkId) {
        this(networkId, new HashSet<>());
    }

    public byte[] pubKeyHash() {
        return DigestUtil.hash(networkId.getPubKey().publicKey().getEncoded());
    }

    public String id() {
        return Hex.encode(pubKeyHash());
    }

    public String userName() {
        return UserNameGenerator.fromHash(pubKeyHash());
    }
}