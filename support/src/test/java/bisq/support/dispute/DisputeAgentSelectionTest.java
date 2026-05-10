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
    @DisplayName("Multiple candidates returns deterministic result")
    void multiple_candidates_returns_deterministic_result() {
        Set<AuthorizedBondedRole> candidates = Set.of(createRole(), createRole(), createRole());

        String first = DisputeAgentSelection.selectDeterministicProfileId(
                candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID).orElseThrow();
        String second = DisputeAgentSelection.selectDeterministicProfileId(
                candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID).orElseThrow();

        assertEquals(first, second);
    }

    @Test
    @DisplayName("Different inputs produce different selection")
    void different_inputs_produce_different_selection() {
        Set<AuthorizedBondedRole> candidates = Set.of(
                createRole(), createRole(), createRole(), createRole(), createRole());

        String result1 = DisputeAgentSelection.selectDeterministicProfileId(
                candidates, padId("maker1"), padId("taker1"), "offer-1").orElseThrow();
        String result2 = DisputeAgentSelection.selectDeterministicProfileId(
                candidates, padId("maker2"), padId("taker2"), "offer-2").orElseThrow();
        String result3 = DisputeAgentSelection.selectDeterministicProfileId(
                candidates, padId("maker3"), padId("taker3"), "offer-3").orElseThrow();

        assertFalse(result1.equals(result2) && result2.equals(result3),
                "All three different inputs selected the same agent — extremely unlikely with 5 candidates");
    }

    @Test
    @DisplayName("Result is stable across many repeated calls")
    void result_is_stable_across_many_repeated_calls() {
        Set<AuthorizedBondedRole> candidates = Set.of(createRole(), createRole(), createRole());

        String expected = DisputeAgentSelection.selectDeterministicProfileId(
                candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID).orElseThrow();

        for (int i = 0; i < 50; i++) {
            assertEquals(expected, DisputeAgentSelection.selectDeterministicProfileId(
                    candidates, MAKERS_PROFILE_ID, TAKERS_PROFILE_ID, OFFER_ID).orElseThrow());
        }
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

    private static AuthorizedBondedRole createRole() {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + System.nanoTime());
        String profileId = pubKey.getId();
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
