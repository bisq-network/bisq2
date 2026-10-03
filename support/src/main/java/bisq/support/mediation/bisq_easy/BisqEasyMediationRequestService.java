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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.timer.Scheduler;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.support.dispute.ChatMessagePruning;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static bisq.support.dispute.DisputeAgentSelection.selectDeterministicProfileId;
import static bisq.support.mediation.bisq_easy.BisqEasyMediationContractIdentityChecks.hasMatchingContractParties;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service used by traders to select mediators, request mediation and process MediationResponses
 */
@Slf4j
public class BisqEasyMediationRequestService implements Service, ConfidentialMessageService.Listener {
    private final NetworkService networkService;
    private final UserProfileService userProfileService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;
    private final Set<BisqEasyMediatorsResponse> pendingBisqEasyMediatorsResponseMessages = new CopyOnWriteArraySet<>();
    @Nullable
    private Pin channeldPin;
    @Nullable
    private Scheduler throttleUpdatesScheduler;

    public BisqEasyMediationRequestService(NetworkService networkService,
                                           ChatService chatService,
                                           UserService userService,
                                           BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userProfileService = userService.getUserProfileService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
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
        if (channeldPin != null) {
            channeldPin.unbind();
            channeldPin = null;
        }
        if (throttleUpdatesScheduler != null) {
            throttleUpdatesScheduler.stop();
            throttleUpdatesScheduler = null;
        }
        pendingBisqEasyMediatorsResponseMessages.clear();
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof BisqEasyMediatorsResponse) {
            processMediationResponse((BisqEasyMediatorsResponse) envelopePayloadMessage);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void requestMediation(BisqEasyOpenTradeChannel channel,
                                 BisqEasyContract contract) {
        // The channel's mediator is a copy. The request goes to the mediator of the signed contract.
        UserProfile mediator = contract.getMediator().orElseThrow();
        findChannelMismatch(channel, contract).ifPresent(mismatch -> {
            throw new IllegalArgumentException(mismatch);
        });
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        checkArgument(!bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile()));

        UserProfile peer = channel.getPeer();
        NetworkId mediatorNetworkId = mediator.getNetworkId();
        // The mediator ignores a request which carries a message of another trade, channel or sender. The channel can
        // hold such a message, from the peer or from the mediator, so the request carries only the messages of the two
        // traders of this trade.
        String channelIdOfTrade = BisqEasyOpenTradeChannel.createId(channel.getTradeId());
        List<BisqEasyOpenTradeMessage> messagesOfTrade = channel.getChatMessages().stream()
                .filter(message -> message.getTradeId().equals(channel.getTradeId()) &&
                        message.getChannelId().equals(channelIdOfTrade) &&
                        (message.getSenderUserProfile().getId().equals(myUserIdentity.getId()) ||
                                message.getSenderUserProfile().getId().equals(peer.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
        BisqEasyMediationRequest bisqEasyMediationRequest = ChatMessagePruning.createWithMaybePrunedMessages(
                messagesOfTrade,
                channel.getTradeId(),
                chatMessages -> new BisqEasyMediationRequest(channel.getTradeId(),
                        contract,
                        myUserIdentity.getUserProfile(),
                        peer,
                        chatMessages,
                        mediatorNetworkId));
        networkService.confidentialSend(bisqEasyMediationRequest,
                mediatorNetworkId,
                myUserIdentity.getNetworkIdWithKeyPair());
    }

    // A request is refused while the channel does not match the contract: the chat routes messages to the channel's
    // mediator and accepts responses only from it, and the mediator ignores a request whose requester and peer are
    // not the parties of the contract. The check has no side effect, so a client can run it before it adds the trade
    // log message which the request carries.
    public Optional<String> findChannelMismatch(BisqEasyOpenTradeChannel channel, BisqEasyContract contract) {
        if (!channel.getBisqEasyOffer().equals(contract.getOffer())) {
            return Optional.of("The channel's offer does not match the contract's offer");
        }
        Optional<NetworkId> channelMediator = channel.getMediator().map(UserProfile::getNetworkId);
        Optional<NetworkId> contractMediator = contract.getMediator().map(UserProfile::getNetworkId);
        if (!channelMediator.equals(contractMediator)) {
            return Optional.of("The channel's mediator does not match the contract's mediator");
        }
        if (!hasMatchingContractParties(contract, channel.getMyUserIdentity().getUserProfile(), channel.getPeer())) {
            return Optional.of("The channel's parties do not match the contract's parties");
        }
        return Optional.empty();
    }

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

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void processMediationResponse(BisqEasyMediatorsResponse bisqEasyMediatorsResponse) {
        bisqEasyOpenTradeChannelService.findChannelByTradeId(bisqEasyMediatorsResponse.getTradeId())
                .ifPresentOrElse(channel -> {
                            if (!isMediationResponseValid(bisqEasyMediatorsResponse, channel)) {
                                pendingBisqEasyMediatorsResponseMessages.remove(bisqEasyMediatorsResponse);
                                return;
                            }

                            // Requester had it activated at request time
                            if (channel.isInMediation()) {
                                bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));
                            } else {
                                bisqEasyOpenTradeChannelService.setIsInMediation(channel, true);
                                bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));

                                //todo (Critical) - check if we do sent from both peers
                                // Peer who has not requested sends their messages as well, so mediator can be sure to get all messages
                            }
                            pendingBisqEasyMediatorsResponseMessages.remove(bisqEasyMediatorsResponse);
                        },
                        () -> {
                            // This handles an edge case that the BisqEasyMediatorsResponse arrives before the take offer request was
                            // processed (in case we are the maker and have been offline at take offer).
                            log.warn("We received a BisqEasyMediatorsResponse but did not find a matching bisqEasyOpenTradeChannel for trade ID {}.\n" +
                                            "We add it to the pendingBisqEasyMediatorsResponseMessages set and reprocess it once a new trade channel has been added.",
                                    bisqEasyMediatorsResponse.getTradeId());
                            pendingBisqEasyMediatorsResponseMessages.add(bisqEasyMediatorsResponse);
                            if (channeldPin == null) {
                                channeldPin = bisqEasyOpenTradeChannelService.getChannels().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void onAdded(BisqEasyOpenTradeChannel element) {
                                        // Delay and ignore too frequent updates
                                        if (throttleUpdatesScheduler == null) {
                                            throttleUpdatesScheduler = Scheduler.run(() -> {
                                                        maybeProcessPendingMediatorsResponseMessages();
                                                        throttleUpdatesScheduler = null;
                                                    })
                                                    .after(1000);
                                        }
                                    }

                                    @Override
                                    public void onRemoved(Object element) {
                                    }

                                    @Override
                                    public void onCleared() {
                                    }
                                });
                            }
                        });
    }

    private boolean isMediationResponseValid(BisqEasyMediatorsResponse bisqEasyMediatorsResponse,
                                             BisqEasyOpenTradeChannel channel) {
        String tradeId = bisqEasyMediatorsResponse.getTradeId();
        Optional<UserProfile> expectedMediator = channel.getMediator();
        if (expectedMediator.isEmpty()) {
            log.warn("Ignoring BisqEasyMediatorsResponse for trade {} because the channel has no selected mediator.", tradeId);
            return false;
        }

        UserProfile mediator = expectedMediator.orElseThrow();
        NetworkId sender = bisqEasyMediatorsResponse.getSenderNetworkId();
        if (!mediator.getId().equals(sender.getId())) {
            log.warn("Ignoring BisqEasyMediatorsResponse for trade {} from unexpected mediator {}. Expected mediator {}.",
                    tradeId, sender.getId(), mediator.getId());
            return false;
        }

        if (bannedUserService.isUserProfileBanned(sender)) {
            log.warn("Ignoring BisqEasyMediatorsResponse for trade {} from banned mediator {}.", tradeId, sender.getId());
            return false;
        }

        return true;
    }

    private void maybeProcessPendingMediatorsResponseMessages() {
        new HashSet<>(pendingBisqEasyMediatorsResponseMessages).forEach(this::processMediationResponse);
    }
}
