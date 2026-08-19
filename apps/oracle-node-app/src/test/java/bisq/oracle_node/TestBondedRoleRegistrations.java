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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;

import java.util.Map;
import java.util.Optional;

public final class TestBondedRoleRegistrations {
    public static final String PROPOSAL_TX_ID = "a".repeat(64);
    public static final String LOCKUP_TX_ID = "b".repeat(64);

    private TestBondedRoleRegistrations() {
    }

    public static BondedRoleRegistrationRequest createCurrentRequest() {
        return createCurrentRequest(1234, LOCKUP_TX_ID);
    }

    public static BondedRoleRegistrationRequest createCurrentRequest(int port, String lockupTxId) {
        var keyPair = KeyGeneration.generateDefaultEcKeyPair();
        var addressMap = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        NetworkId networkId = new NetworkId(addressMap, new PubKey(keyPair.getPublic(), "test-key"));
        return new BondedRoleRegistrationRequest("0123456789abcdef0123456789abcdef01234567",
                "public-key",
                BondedRoleType.MEDIATOR,
                "bond-user",
                "signature",
                Optional.empty(),
                networkId,
                false,
                BondedRoleRegistrationProtocol.CURRENT_VERSION,
                PROPOSAL_TX_ID,
                lockupTxId);
    }
}
