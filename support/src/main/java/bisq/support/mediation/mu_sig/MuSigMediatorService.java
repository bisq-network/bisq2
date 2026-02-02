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

package bisq.support.mediation.mu_sig;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
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
public class MuSigMediatorService extends RateLimitedPersistenceClient<MuSigMediatorStore> implements Service, ConfidentialMessageService.Listener {
    @Getter
    private final MuSigMediatorStore persistableStore = new MuSigMediatorStore();
    @Getter
    private final Persistence<MuSigMediatorStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;

    public MuSigMediatorService(PersistenceService persistenceService,
                                NetworkService networkService,
                                ChatService chatService,
                                UserService userService,
                                BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        muSigOpenTradeChannelService = chatService.getMuSigOpenTradeChannelService();
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getConfidentialMessageServices().stream()
                .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                .forEach(this::onMessage);
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigMediationRequest) {
            processMediationRequest((MuSigMediationRequest) envelopePayloadMessage);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void closeMediationCase(MuSigMediationCase muSigMediationCase) {
        if (muSigMediationCase.setClosed(true)) {
            persist();
        }
    }

    public void removeMediationCase(MuSigMediationCase muSigMediationCase) {
        getMediationCases().remove(muSigMediationCase);
        persist();
    }

    public void reOpenMediationCase(MuSigMediationCase muSigMediationCase) {
        if (muSigMediationCase.setClosed(false)) {
            persist();
        }
    }

    public ObservableSet<MuSigMediationCase> getMediationCases() {
        return persistableStore.getMuSigMediationCases();
    }

    public Optional<UserIdentity> findMyMediatorUserIdentity(Optional<UserProfile> mediator) {
        return findMyMediatorUserIdentities()
                .filter(e -> mediator.isPresent())
                .filter(userIdentity -> userIdentity.getUserProfile().getId().equals(mediator.orElseThrow().getId()))
                .findAny();
    }

    public Stream<UserIdentity> findMyMediatorUserIdentities() {
        // If we got banned we still want to show the admin UI
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                .filter(data -> data.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream());
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void processMediationRequest(MuSigMediationRequest muSigMediationRequest) {
        UserProfile requester = muSigMediationRequest.getRequester();
        if (bannedUserService.isUserProfileBanned(requester)) {
            log.warn("Message ignored as sender is banned");
            return;
        }
        MuSigContract contract = muSigMediationRequest.getContract();
        findMyMediatorUserIdentity(contract.getMediator()).ifPresent(myUserIdentity -> {
            String tradeId = muSigMediationRequest.getTradeId();
            UserProfile peer = muSigMediationRequest.getPeer();
            List<MuSigOpenTradeMessage> chatMessages = muSigMediationRequest.getChatMessages();
            MuSigOpenTradeChannel channel = muSigOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    tradeId,
                    myUserIdentity,
                    requester,
                    peer
            );

            muSigOpenTradeChannelService.setIsInMediation(channel, true);

            chatMessages.forEach(chatMessage ->
                    muSigOpenTradeChannelService.addMessage(chatMessage, channel));

            // We apply the muSigMediationCase after the channel is set up as clients will expect a channel.
            MuSigMediationCase muSigMediationCase = new MuSigMediationCase(muSigMediationRequest);
            addNewMediationCase(muSigMediationCase);

            NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

            // Send to requester
            networkService.confidentialSend(new MuSigMediatorsResponse(tradeId),
                    requester.getNetworkId(),
                    networkIdWithKeyPair);
            muSigOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));

            // Send to peer
            networkService.confidentialSend(new MuSigMediatorsResponse(tradeId),
                    peer.getNetworkId(),
                    networkIdWithKeyPair);
            muSigOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));
        });
    }

    private void addNewMediationCase(MuSigMediationCase muSigMediationCase) {
        getMediationCases().add(muSigMediationCase);
        persist();
    }
}
