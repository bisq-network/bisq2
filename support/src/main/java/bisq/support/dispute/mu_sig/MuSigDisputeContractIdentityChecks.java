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

package bisq.support.dispute.mu_sig;

import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.user.profile.UserProfile;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public final class MuSigDisputeContractIdentityChecks {
    private MuSigDisputeContractIdentityChecks() {
    }

    public static boolean hasMatchingContractParties(MuSigContract contract,
                                                     UserProfile requester,
                                                     UserProfile peer) {
        return requester.getNetworkId().equals(contract.getMaker().getNetworkId()) &&
                peer.getNetworkId().equals(contract.getTaker().getNetworkId()) ||
                requester.getNetworkId().equals(contract.getTaker().getNetworkId()) &&
                        peer.getNetworkId().equals(contract.getMaker().getNetworkId());
    }

    public static boolean hasMatchingContractDisputeAgent(Optional<UserProfile> disputeAgent,
                                                          NetworkId receiver) {
        return disputeAgent
                .map(UserProfile::getNetworkId)
                .map(disputeAgentNetworkId -> disputeAgentNetworkId.equals(receiver))
                .orElse(false);
    }

    public static Role resolveSenderRole(MuSigContract contract, String senderUserProfileId) {
        checkArgument(isContractParty(contract, senderUserProfileId),
                "senderUserProfileId must be one of the contract parties");
        return senderUserProfileId.equals(contract.getOffer().getMakersUserProfileId()) ? Role.MAKER : Role.TAKER;
    }

    private static boolean isContractParty(MuSigContract contract, String senderUserProfileId) {
        return senderUserProfileId.equals(contract.getOffer().getMakersUserProfileId()) ||
                senderUserProfileId.equals(contract.getTaker().getNetworkId().getId());
    }
}
