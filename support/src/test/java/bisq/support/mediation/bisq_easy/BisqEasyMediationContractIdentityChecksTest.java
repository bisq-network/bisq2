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
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BisqEasyMediationContractIdentityChecksTest {
    @Test
    void acceptsRequesterAndPeerWhenTheyMatchMakerAndTaker() {
        UserProfile maker = createUserProfile("maker");
        UserProfile taker = createUserProfile("taker");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                maker.getNetworkId(),
                taker.getNetworkId(),
                maker,
                taker));
    }

    @Test
    void acceptsRequesterAndPeerWhenTheyMatchTakerAndMaker() {
        UserProfile maker = createUserProfile("maker");
        UserProfile taker = createUserProfile("taker");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                maker.getNetworkId(),
                taker.getNetworkId(),
                taker,
                maker));
    }

    @Test
    void rejectsRequesterOrPeerOutsideContractParties() {
        UserProfile maker = createUserProfile("maker");
        UserProfile taker = createUserProfile("taker");
        UserProfile outsider = createUserProfile("outsider");

        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                maker.getNetworkId(),
                taker.getNetworkId(),
                maker,
                outsider));
    }

    @Test
    void acceptsMatchingContractMediator() {
        UserProfile mediator = createUserProfile("mediator");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.of(mediator),
                mediator.getNetworkId()));
    }

    @Test
    void rejectsMissingOrMismatchingContractMediator() {
        UserProfile mediator = createUserProfile("mediator");
        UserProfile other = createUserProfile("other");

        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.empty(),
                mediator.getNetworkId()));
        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.of(mediator),
                other.getNetworkId()));
    }

    private UserProfile createUserProfile(String nickName) {
        return new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                createNetworkId(nickName),
                "",
                "",
                "");
    }

    private NetworkId createNetworkId(String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
