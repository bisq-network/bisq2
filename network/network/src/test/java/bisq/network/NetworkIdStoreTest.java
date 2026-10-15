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

package bisq.network;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkIdStoreTest {
    private static final String KEY_ID_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String KEY_ID_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String KEY_ID_OTHER = "cccccccccccccccccccccccccccccccccccccccc";

    @Test
    void findsOurNetworkIdByTheKeyIdOfItsKey() {
        NetworkId networkIdA = networkId(1000, KEY_ID_A);
        NetworkId networkIdB = networkId(1001, KEY_ID_B);
        NetworkIdStore store = new NetworkIdStore(Map.of("default", networkIdA, "profileB", networkIdB));

        assertEquals(Optional.of(networkIdA), store.findNetworkIdByKeyId(KEY_ID_A));
        assertEquals(Optional.of(networkIdB), store.findNetworkIdByKeyId(KEY_ID_B));
        assertEquals(Optional.empty(), store.findNetworkIdByKeyId(KEY_ID_OTHER));
    }

    private static NetworkId networkId(int port, String keyId) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }
}
