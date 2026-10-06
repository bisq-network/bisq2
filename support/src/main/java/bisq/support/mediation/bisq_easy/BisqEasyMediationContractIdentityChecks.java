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

import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.identity.NetworkId;
import bisq.user.profile.UserProfile;

import java.util.Optional;

public final class BisqEasyMediationContractIdentityChecks {
    private BisqEasyMediationContractIdentityChecks() {
    }

    public static boolean hasMatchingContractParties(BisqEasyContract contract,
                                                     UserProfile requester,
                                                     UserProfile peer) {
        return hasMatchingContractParties(contract.getMaker().getNetworkId(),
                contract.getTaker().getNetworkId(),
                requester,
                peer);
    }

    // A party is identified by its key: NetworkId.getId() is the hash of the public key, which is also the profile ID.
    // The address map of a NetworkId is routing data, which can change after the offer was published, for example when
    // a user adds a transport. Comparing whole NetworkIds would make the mediator ignore the request of honest traders.
    // A trade has two different parties. A request which names one profile as requester and peer, with a contract
    // which names it as maker and taker, does not match, because a mediation channel needs two traders.
    static boolean hasMatchingContractParties(NetworkId makerNetworkId,
                                              NetworkId takerNetworkId,
                                              UserProfile requester,
                                              UserProfile peer) {
        String requesterId = requester.getNetworkId().getId();
        String peerId = peer.getNetworkId().getId();
        if (requesterId.equals(peerId)) {
            return false;
        }
        return (requesterId.equals(makerNetworkId.getId()) && peerId.equals(takerNetworkId.getId())) ||
                (requesterId.equals(takerNetworkId.getId()) && peerId.equals(makerNetworkId.getId()));
    }

    // A forged message from the mediator's own profile would render as the mediator's own words, without any mark.
    // By key, as for the parties: a copy of the mediator's profile can carry other addresses.
    public static boolean isMediatorAParty(UserProfile requester, UserProfile peer, NetworkId receiver) {
        String mediatorId = receiver.getId();
        return requester.getNetworkId().getId().equals(mediatorId) || peer.getNetworkId().getId().equals(mediatorId);
    }

    public static boolean hasMatchingContractMediator(Optional<UserProfile> mediator,
                                                      NetworkId receiver) {
        return mediator
                .map(UserProfile::getNetworkId)
                .map(mediatorNetworkId -> mediatorNetworkId.getId().equals(receiver.getId()))
                .orElse(false);
    }
}
