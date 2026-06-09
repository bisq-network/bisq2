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

package bisq.trade.mu_sig.mediation;

import bisq.account.accounts.AccountPayload;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.support.dispute.ChatMessagePruning;
import bisq.support.dispute.mu_sig.MuSigDisputeCaseDataMessage;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.support.mediation.mu_sig.MuSigMediationResultAcceptanceMessage;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static bisq.support.dispute.DisputeAgentSelection.selectDeterministicProfileId;

/**
 * Service used by traders to select mediators, request mediation and process mediation state changes.
 */
@Slf4j
public class MuSigTraderMediationService {
    private final NetworkService networkService;
    private final UserProfileService userProfileService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public MuSigTraderMediationService(NetworkService networkService,
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

    public Optional<UserProfile> selectMediator(String makersUserProfileId,
                                                String takersUserProfileId,
                                                String offerId) {
        Set<AuthorizedBondedRole> mediators = authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(role -> role.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .filter(role -> !role.getProfileId().equals(makersUserProfileId) &&
                        !role.getProfileId().equals(takersUserProfileId))
                .collect(Collectors.toSet());
        return selectMediator(mediators, makersUserProfileId, takersUserProfileId, offerId);
    }

    // This method can be used for verification when taker provides mediators list.
    // If mediator list was not matching the expected one present in the network it might have been a manipulation attempt.
    public Optional<UserProfile> selectMediator(Set<AuthorizedBondedRole> mediators,
                                                String makersProfileId,
                                                String takersProfileId,
                                                String offerId) {
        return selectDeterministicProfileId(mediators, makersProfileId, takersProfileId, offerId)
                .flatMap(userProfileService::findUserProfile);
    }

    public void requestMediation(String tradeId,
                                 Identity myIdentity,
                                 MuSigTradeParty peer,
                                 UserProfile mediator,
                                 MuSigContract contract,
                                 MuSigOpenTradeChannel channel) {
        String encoded = Res.encode("muSig.mediation.requester.tradeLogMessage", channel.getMyUserIdentity().getUserName());
        muSigOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);

        NetworkId mediatorNetworkId = mediator.getNetworkId();

        UserProfile requester = userProfileService.findUserProfile(myIdentity.getId()).orElseThrow();
        UserProfile peerUserProfile = userProfileService.findUserProfile(peer.getNetworkId().getId()).orElseThrow();
        MuSigMediationRequest muSigMediationRequest = ChatMessagePruning.createWithMaybePrunedMessages(
                new ArrayList<>(channel.getChatMessages()),
                tradeId,
                chatMessages -> new MuSigMediationRequest(tradeId,
                        contract,
                        requester,
                        peerUserProfile,
                        chatMessages,
                        mediatorNetworkId));
        networkService.confidentialSend(muSigMediationRequest,
                mediatorNetworkId,
                myIdentity.getNetworkIdWithKeyPair());
    }

    public void applyMediationStateToChannel(String tradeId,
                                             MuSigDisputeState newTradeDispute,
                                             MuSigDisputeState previousDisputeState,
                                             MuSigOpenTradeChannel channel) {
        if (newTradeDispute == MuSigDisputeState.MEDIATION_OPEN) {
            if (previousDisputeState == MuSigDisputeState.MEDIATION_REQUESTED) {
                muSigOpenTradeChannelService.addMediationOpenedMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));
            } else if (previousDisputeState == MuSigDisputeState.NO_DISPUTE) {
                muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
                muSigOpenTradeChannelService.addMediationOpenedMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));
            }
        } else if (newTradeDispute == MuSigDisputeState.MEDIATION_RE_OPENED) {
            muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
        } else if (newTradeDispute == MuSigDisputeState.MEDIATION_CLOSED) {
            // Closed mediation case still keeps mediator chat participation active.
            muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
        }
    }

    public void sendDisputeCaseDataMessage(String tradeId,
                                           Identity myIdentity,
                                           UserProfile mediator,
                                           MuSigContract contract) {
        byte[] contractHash = ContractService.getContractHash(contract);
        MuSigDisputeCaseDataMessage message = ChatMessagePruning.createWithMaybePrunedMessages(
                muSigOpenTradeChannelService.findChannelByTradeId(tradeId)
                        .map(channel -> new ArrayList<>(channel.getChatMessages()))
                        .orElseGet(ArrayList::new),
                tradeId,
                chatMessages -> new MuSigDisputeCaseDataMessage(
                        tradeId,
                        myIdentity.getNetworkId(),
                        contractHash,
                        chatMessages));
        networkService.confidentialSend(message,
                mediator.getNetworkId(),
                myIdentity.getNetworkIdWithKeyPair());
    }

    public void sendMediationResultAcceptanceMessage(String tradeId,
                                                     Identity myIdentity,
                                                     MuSigTradeParty peer,
                                                     boolean mediationResultAccepted,
                                                     MuSigOpenTradeChannel channel) {
        networkService.confidentialSend(new MuSigMediationResultAcceptanceMessage(tradeId,
                        myIdentity.getNetworkId(),
                        mediationResultAccepted),
                peer.getNetworkId(),
                myIdentity.getNetworkIdWithKeyPair());

        String key = mediationResultAccepted
                ? "muSig.mediation.result.accepted.tradeLogMessage"
                : "muSig.mediation.result.rejected.tradeLogMessage";
        String encoded = Res.encode(key, channel.getMyUserIdentity().getUserName());
        muSigOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
    }

    public void sendDisputeCasePaymentDetailsResponse(String tradeId,
                                                      Identity myIdentity,
                                                      NetworkId receiverNetworkId,
                                                      AccountPayload<?> takerAccountPayload,
                                                      AccountPayload<?> makerAccountPayload) {
        MuSigDisputeCasePaymentDetailsResponse paymentDetailsResponse = new MuSigDisputeCasePaymentDetailsResponse(
                tradeId,
                myIdentity.getNetworkId(),
                takerAccountPayload,
                makerAccountPayload
        );
        networkService.confidentialSend(paymentDetailsResponse,
                receiverNetworkId,
                myIdentity.getNetworkIdWithKeyPair());
    }
}
