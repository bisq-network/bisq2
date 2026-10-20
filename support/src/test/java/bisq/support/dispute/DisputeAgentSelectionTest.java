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

package bisq.support.dispute;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DisputeAgentSelectionTest {

    private static final String MAKERS_PROFILE_ID = padId("maker");
    private static final String TAKERS_PROFILE_ID = padId("taker");
    private static final String OFFER_ID = "offer-123";

    @Test
    @DisplayName("Empty candidates returns empty optional")
    void empty_candidates_returns_empty_optional() {
        Optional<String> result = DisputeAgentSelection.selectDeterministicProfileId(
                Collections.emptySet(), MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Single candidate returns that candidate's profile ID")
    void single_candidate_returns_that_candidates_profile_id() {
        AuthorizedBondedRole role = createRole();
        String profileId = role.getProfileId();
        Optional<String> result = DisputeAgentSelection.selectDeterministicProfileId(
                Set.of(role), MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID);
        assertTrue(result.isPresent());
        assertEquals(profileId, result.get());
    }

    @Test
    @DisplayName("Order independent: different Set iteration order produces same result")
    void order_independent_different_set_iteration_order_produces_same_result() {
        AuthorizedBondedRole role1 = createRole();
        AuthorizedBondedRole role2 = createRole();
        AuthorizedBondedRole role3 = createRole();

        Set<AuthorizedBondedRole> setA = new LinkedHashSet<>(List.of(role1, role2, role3));
        Set<AuthorizedBondedRole> setB = new LinkedHashSet<>(List.of(role3, role1, role2));

        String resultA = DisputeAgentSelection.selectDeterministicProfileId(
                setA, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID).orElseThrow();
        String resultB = DisputeAgentSelection.selectDeterministicProfileId(
                setB, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID).orElseThrow();

        assertEquals(resultA, resultB);
    }

    // The maker's and the taker's node must select the same mediator, also when they run different versions. The
    // expected IDs were computed outside of this code: the RIPEMD-160 hash of the SHA-256 hash of maker ID, taker ID and
    // offer ID, its five 32-bit words XORed, modulo the number of candidates, as index into the candidates sorted by
    // profile ID. Versions 2.1.13 and 2.1.14 select in the same way.
    @Test
    @DisplayName("Selection matches fixed expected values")
    void selection_matches_fixed_expected_values() {
        Set<AuthorizedBondedRole> candidates = Set.of(createRole("1".repeat(40)),
                createRole("2".repeat(40)),
                createRole("3".repeat(40)),
                createRole("4".repeat(40)),
                createRole("5".repeat(40)));

        assertEquals("4".repeat(40), DisputeAgentSelection.selectDeterministicProfileId(
                candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, "offer-1").orElseThrow());
        assertEquals("2".repeat(40), DisputeAgentSelection.selectDeterministicProfileId(
                candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, "offer-3").orElseThrow());
        assertEquals("3".repeat(40), DisputeAgentSelection.selectDeterministicProfileId(
                candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, "offer-4").orElseThrow());
    }

    private static AuthorizedBondedRole createRole() {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + System.nanoTime());
        return createRole(pubKey.getId(), kp, pubKey);
    }

    private static AuthorizedBondedRole createRole(String profileId) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        return createRole(profileId, kp, new PubKey(kp.getPublic(), "key-" + profileId));
    }

    private static AuthorizedBondedRole createRole(String profileId, KeyPair kp, PubKey pubKey) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(9000))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        return new AuthorizedBondedRole(
                profileId,
                Hex.encode(kp.getPublic().getEncoded()),
                BondedRoleType.MEDIATOR,
                profileId.substring(0, 10) + "-bond",
                "dummySig+base64sig00000000000000000000000000000000000000000000000=",
                Optional.of(addresses),
                networkId,
                Optional.empty(),
                false
        );
    }

    private static String padId(String prefix) {
        return (prefix + "0".repeat(40)).substring(0, 40);
    }
}
