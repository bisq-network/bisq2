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

    static boolean hasMatchingContractParties(NetworkId makerNetworkId,
                                              NetworkId takerNetworkId,
                                              UserProfile requester,
                                              UserProfile peer) {
        return (requester.getNetworkId().equals(makerNetworkId) &&
                peer.getNetworkId().equals(takerNetworkId)) ||
                (requester.getNetworkId().equals(takerNetworkId) &&
                        peer.getNetworkId().equals(makerNetworkId));
    }

    public static boolean hasMatchingContractMediator(Optional<UserProfile> mediator,
                                                      NetworkId receiver) {
        return mediator
                .map(UserProfile::getNetworkId)
                .map(mediatorNetworkId -> mediatorNetworkId.equals(receiver))
                .orElse(false);
    }
}
