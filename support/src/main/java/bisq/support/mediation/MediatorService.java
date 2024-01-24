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

package bisq.support.mediation;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Service used by mediators
 */
@Slf4j
public class MediatorService implements PersistenceClient<MediatorStore>, Service, ConfidentialMessageService.Listener {
    @Getter
    private final MediatorStore persistableStore = new MediatorStore();
    @Getter
    private final Persistence<MediatorStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;

    public MediatorService(PersistenceService persistenceService,
                           NetworkService networkService,
                           ChatService chatService,
                           UserService userService,
                           BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MediationRequest) {
            processMediationRequest((MediationRequest) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void closeMediationCase(MediationCase mediationCase) {
        mediationCase.setClosed(true);
        persist();
    }

    public void reOpenMediationCase(MediationCase mediationCase) {
        mediationCase.setClosed(false);
        persist();
    }

    public ObservableSet<MediationCase> getMediationCases() {
        return persistableStore.getMediationCases();
    }

    public Optional<UserIdentity> findMyMediatorUserIdentity(Optional<UserProfile> mediator) {
        return findMyMediatorUserIdentities()
                .filter(e -> mediator.isPresent())
                .filter(userIdentity -> userIdentity.getUserProfile().getId().equals(mediator.orElseThrow().getId()))
                .findAny();
    }

    public Stream<UserIdentity> findMyMediatorUserIdentities() {
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(data -> data.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processMediationRequest(MediationRequest mediationRequest) {
        UserProfile requester = mediationRequest.getRequester();
        if (bannedUserService.isUserProfileBanned(requester)) {
            log.warn("Message ignored as sender is banned");
            return;
        }
        BisqEasyContract contract = mediationRequest.getContract();
        findMyMediatorUserIdentity(contract.getMediator()).ifPresent(myUserIdentity -> {
            String tradeId = mediationRequest.getTradeId();
            UserProfile peer = mediationRequest.getPeer();
            List<BisqEasyOpenTradeMessage> chatMessages = mediationRequest.getChatMessages();
            BisqEasyOpenTradeChannel channel = bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    tradeId,
                    contract.getOffer(),
                    myUserIdentity,
                    requester,
                    peer
            );

            bisqEasyOpenTradeChannelService.setIsInMediation(channel, true);

            chatMessages.forEach(chatMessage ->
                    bisqEasyOpenTradeChannelService.addMessage(chatMessage, channel));

            // We apply the mediationCase after the channel is set up as clients will expect a channel.
            MediationCase mediationCase = new MediationCase(mediationRequest);
            addNewMediationCase(mediationCase);

            NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

            // Send to requester
            networkService.confidentialSend(new MediatorsResponse(tradeId),
                    requester.getNetworkId(),
                    networkIdWithKeyPair);
            bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.get("authorizedRole.mediator.message.toRequester"));

            // Send to peer
            networkService.confidentialSend(new MediatorsResponse(tradeId),
                    peer.getNetworkId(),
                    networkIdWithKeyPair);
            bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.get("authorizedRole.mediator.message.toNonRequester"));
        });
    }

    private void addNewMediationCase(MediationCase mediationCase) {
        getMediationCases().add(mediationCase);
        persist();
    }
}

