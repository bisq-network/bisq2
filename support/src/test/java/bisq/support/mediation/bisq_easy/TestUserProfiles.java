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

package bisq.support.mediation.bisq_easy;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;

import java.security.KeyPair;
import java.util.Map;

final class TestUserProfiles {
    private TestUserProfiles() {
    }

    static UserProfile create(String nickName) {
        return create(nickName, createNetworkId(nickName, KeyGeneration.generateDefaultEcKeyPair()));
    }

    static UserIdentity createUserIdentity(String nickName) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId networkId = createNetworkId(nickName, keyPair);
        KeyBundle keyBundle = new KeyBundle(nickName, keyPair, TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair());
        return new UserIdentity(new Identity(nickName, networkId, keyBundle), create(nickName, networkId));
    }

    // A copy of the profile with the same key and other addresses
    static UserProfile withOtherAddress(UserProfile userProfile) {
        Address otherAddress = Address.from("127.0.0.1", 2000);
        NetworkId networkId = new NetworkId(
                new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                userProfile.getNetworkId().getPubKey());
        return create(userProfile.getNickName(), networkId);
    }

    private static UserProfile create(String nickName, NetworkId networkId) {
        return new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                "",
                "");
    }

    private static NetworkId createNetworkId(String keyId, KeyPair keyPair) {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
