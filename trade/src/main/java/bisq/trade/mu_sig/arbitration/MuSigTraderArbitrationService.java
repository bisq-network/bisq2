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

package bisq.trade.mu_sig.arbitration;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.support.dispute.ChatMessagePruning;
import bisq.support.arbitration.mu_sig.MuSigArbitrationRequest;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static bisq.support.dispute.DisputeAgentSelection.selectDeterministicProfileId;

public class MuSigTraderArbitrationService {
    private final NetworkService networkService;
    private final UserProfileService userProfileService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;

    public MuSigTraderArbitrationService(NetworkService networkService,
                                         ChatService chatService,
                                         UserService userService,
                                         BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userProfileService = userService.getUserProfileService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        muSigOpenTradeChannelService = chatService.getMuSigOpenTradeChannelService();
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Optional<UserProfile> selectArbitrator(String makersUserProfileId,
                                                  String takersUserProfileId,
                                                  String offerId,
                                                  Optional<String> mediatorsUserProfileId) {
        Set<AuthorizedBondedRole> arbitrators = authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(role -> role.getBondedRoleType() == BondedRoleType.ARBITRATOR)
                .filter(role -> !role.getProfileId().equals(makersUserProfileId) &&
                        !role.getProfileId().equals(takersUserProfileId))
                .filter(role -> mediatorsUserProfileId
                        .map(profileId -> !role.getProfileId().equals(profileId))
                        .orElse(true))
                .collect(Collectors.toSet());
        return selectArbitrator(arbitrators, makersUserProfileId, takersUserProfileId, offerId);
    }

    // This method can be used for verification when taker provides arbitrators list.
    // If arbitrator list was not matching the expected one present in the network it might have been a manipulation attempt.
    public Optional<UserProfile> selectArbitrator(Set<AuthorizedBondedRole> arbitrators,
                                                  String makersProfileId,
                                                  String takersProfileId,
                                                  String offerId) {
        return selectDeterministicProfileId(arbitrators, makersProfileId, takersProfileId, offerId)
                .flatMap(userProfileService::findUserProfile);
    }

    public void requestArbitration(String tradeId,
                                   Identity myIdentity,
                                   MuSigTradeParty peer,
                                   UserProfile arbitrator,
                                   MuSigContract contract,
                                   MuSigMediationResult mediationResult,
                                   byte[] mediationResultSignature,
                                   MuSigOpenTradeChannel channel) {
        String encoded = Res.encode("muSig.arbitration.requester.tradeLogMessage", channel.getMyUserIdentity().getUserName());
        muSigOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.ARBITRATOR);

        NetworkId arbitratorNetworkId = arbitrator.getNetworkId();

        UserProfile requester = userProfileService.findUserProfile(myIdentity.getId()).orElseThrow();
        UserProfile peerUserProfile = userProfileService
                .findUserProfile(peer.getNetworkId().getId())
                .orElseThrow();
        MuSigArbitrationRequest muSigArbitrationRequest = ChatMessagePruning.createWithMaybePrunedMessages(
                new ArrayList<>(channel.getChatMessages()),
                tradeId,
                chatMessages -> new MuSigArbitrationRequest(tradeId,
                        contract,
                        mediationResult,
                        mediationResultSignature,
                        requester,
                        peerUserProfile,
                        chatMessages,
                        arbitratorNetworkId));
        networkService.confidentialSend(muSigArbitrationRequest,
                arbitratorNetworkId,
                myIdentity.getNetworkIdWithKeyPair());
    }

    public void applyArbitrationStateToChannel(String tradeId,
                                               MuSigDisputeState newDisputeState,
                                               MuSigDisputeState previousDisputeState,
                                               MuSigOpenTradeChannel channel) {
        if (newDisputeState == MuSigDisputeState.ARBITRATION_OPEN) {
            if (previousDisputeState == MuSigDisputeState.ARBITRATION_REQUESTED) {
                muSigOpenTradeChannelService.addArbitrationOpenedMessage(channel, Res.encode("authorizedRole.arbitrator.message.toRequester"));
            } else {
                muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.ARBITRATOR);
                muSigOpenTradeChannelService.addArbitrationOpenedMessage(channel, Res.encode("authorizedRole.arbitrator.message.toNonRequester"));
            }
        } else if (newDisputeState == MuSigDisputeState.ARBITRATION_CLOSED) {
            // Closed arbitration case still keeps arbitrator chat participation active.
            muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.ARBITRATOR);
        }
    }
}
