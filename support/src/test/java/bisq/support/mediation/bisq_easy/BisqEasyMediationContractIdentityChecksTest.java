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
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BisqEasyMediationContractIdentityChecksTest {
    @Test
    void acceptsRequesterAndPeerWhenTheyMatchMakerAndTaker() {
        UserProfile maker = TestUserProfiles.create("maker");
        UserProfile taker = TestUserProfiles.create("taker");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                maker.getNetworkId(),
                taker.getNetworkId(),
                maker,
                taker));
    }

    @Test
    void acceptsRequesterAndPeerWhenTheyMatchTakerAndMaker() {
        UserProfile maker = TestUserProfiles.create("maker");
        UserProfile taker = TestUserProfiles.create("taker");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                maker.getNetworkId(),
                taker.getNetworkId(),
                taker,
                maker));
    }

    @Test
    void rejectsRequesterOrPeerOutsideContractParties() {
        UserProfile maker = TestUserProfiles.create("maker");
        UserProfile taker = TestUserProfiles.create("taker");
        UserProfile outsider = TestUserProfiles.create("outsider");

        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                maker.getNetworkId(),
                taker.getNetworkId(),
                maker,
                outsider));
    }

    // A trade has two different parties. The mediator creates a channel with the requester and the peer as its two
    // traders, so a request which names one profile twice must not match, also with another copy of the profile.
    @Test
    void rejectsRequesterWhoIsAlsoThePeer() {
        UserProfile trader = TestUserProfiles.create("trader");
        UserProfile traderWithOtherNickName = new UserProfile(0,
                "other nick name",
                trader.getProofOfWork(),
                0,
                trader.getNetworkId(),
                "",
                "",
                "");

        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                trader.getNetworkId(),
                trader.getNetworkId(),
                trader,
                trader));
        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                trader.getNetworkId(),
                trader.getNetworkId(),
                trader,
                traderWithOtherNickName));
    }

    @Test
    void acceptsMatchingContractMediator() {
        UserProfile mediator = TestUserProfiles.create("mediator");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.of(mediator),
                mediator.getNetworkId()));
    }

    @Test
    void rejectsMissingOrMismatchingContractMediator() {
        UserProfile mediator = TestUserProfiles.create("mediator");
        UserProfile other = TestUserProfiles.create("other");

        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.empty(),
                mediator.getNetworkId()));
        assertFalse(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.of(mediator),
                other.getNetworkId()));
    }

    // The address map is routing data and can change after the offer was published, for example when a user adds a
    // transport. The parties are identified by their keys.
    @Test
    void acceptsPartiesAndMediatorWhoseAddressesChanged() {
        UserProfile maker = TestUserProfiles.create("maker");
        UserProfile taker = TestUserProfiles.create("taker");
        UserProfile mediator = TestUserProfiles.create("mediator");

        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractParties(
                withOtherAddress(maker.getNetworkId()),
                withOtherAddress(taker.getNetworkId()),
                maker,
                taker));
        assertTrue(BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator(
                Optional.of(mediator),
                withOtherAddress(mediator.getNetworkId())));
    }

    private static NetworkId withOtherAddress(NetworkId networkId) {
        Address otherAddress = Address.from("127.0.0.1", 2000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                networkId.getPubKey());
    }
}
