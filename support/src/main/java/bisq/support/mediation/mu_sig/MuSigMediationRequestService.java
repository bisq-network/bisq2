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
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.timer.Scheduler;
import bisq.contract.mu_sig.MuSigContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.security.DigestUtil;
import bisq.support.mediation.MediationCaseState;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service used by traders to select mediators, request mediation and process mediation state changes.
 */
@Slf4j
public class MuSigMediationRequestService implements Service, ConfidentialMessageService.Listener {
    private final NetworkService networkService;
    private final UserProfileService userProfileService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;
    private final Set<MuSigMediationStateChangeMessage> pendingMuSigMediationStateChangeMessages = new CopyOnWriteArraySet<>();
    @Nullable
    private Pin channeldPin;
    @Nullable
    private Scheduler throttleUpdatesScheduler;

    public MuSigMediationRequestService(NetworkService networkService,
                                        ChatService chatService,
                                        UserService userService,
                                        BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userProfileService = userService.getUserProfileService();
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
        if (channeldPin != null) {
            channeldPin.unbind();
            channeldPin = null;
        }
        if (throttleUpdatesScheduler != null) {
            throttleUpdatesScheduler.stop();
            throttleUpdatesScheduler = null;
        }
        pendingMuSigMediationStateChangeMessages.clear();
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigMediationStateChangeMessage) {
            processMediationStateChangeMessage((MuSigMediationStateChangeMessage) envelopePayloadMessage);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void requestMediation(MuSigOpenTradeChannel channel,
                                 MuSigContract contract) {
        // checkArgument(channel.getMuSigOffer().equals(contract.getOffer()));
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        checkArgument(!bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile()));

        UserProfile peer = channel.getPeer();
        UserProfile mediator = channel.getMediator().orElseThrow();
        NetworkId mediatorNetworkId = mediator.getNetworkId();

        MuSigMediationRequest muSigMediationRequest = new MuSigMediationRequest(channel.getTradeId(),
                contract,
                myUserIdentity.getUserProfile(),
                peer,
                new ArrayList<>(channel.getChatMessages()),
                mediatorNetworkId);
        networkService.confidentialSend(muSigMediationRequest,
                mediatorNetworkId,
                myUserIdentity.getNetworkIdWithKeyPair());
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
        if (mediators.isEmpty()) {
            return Optional.empty();
        }

        if (mediators.size() == 1) {
            return userProfileService.findUserProfile(mediators.iterator().next().getProfileId());
        }

        int index = getDeterministicIndex(mediators, makersProfileId, takersProfileId, offerId);

        ArrayList<AuthorizedBondedRole> list = new ArrayList<>(mediators);
        list.sort(Comparator.comparing(AuthorizedBondedRole::getProfileId));
        return userProfileService.findUserProfile(list.get(index).getProfileId());
    }

    private int getDeterministicIndex(Set<AuthorizedBondedRole> mediators,
                                      String makersProfileId,
                                      String takersProfileId,
                                      String offerId) {
        String input = makersProfileId + takersProfileId + offerId;
        byte[] hash = DigestUtil.hash(input.getBytes(StandardCharsets.UTF_8)); // returns 20 bytes
        // XOR multiple 4-byte chunks to use more of the hash
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        int space = buffer.getInt(); // First 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with next 4 bytes
        space ^= buffer.getInt();    // XOR with last 4 bytes (20 bytes total)
        return Math.floorMod(space, mediators.size());
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void processMediationStateChangeMessage(MuSigMediationStateChangeMessage message) {
        muSigOpenTradeChannelService.findChannelByTradeId(message.getTradeId())
                .ifPresentOrElse(channel -> {
                            Optional<UserProfile> mediator = channel.getMediator();
                            if (mediator.isEmpty()) {
                                log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} because mediator is missing in contract.",
                                        message.getTradeId());
                                return;
                            }
                            if (!mediator.orElseThrow().getId().equals(message.getSenderUserProfile().getId())) {
                                log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} with unexpected senderUserProfile {}.",
                                        message.getTradeId(), message.getSenderUserProfile());
                                return;
                            }

                            if (bannedUserService.isUserProfileBanned(message.getSenderUserProfile())) {
                                log.warn("Ignoring MuSigMediationStateChangeMessage as sender is banned");
                                return;
                            }

                            MediationCaseState mediationCaseState = message.getMediationCaseState();
                            if (mediationCaseState == MediationCaseState.OPEN) {
                                // Requester had it activated at request time
                                if (channel.getDisputeAgentType() == MuSigDisputeAgentType.MEDIATOR) {
                                    muSigOpenTradeChannelService.addMediationOpenedMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));
                                } else {
                                    muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
                                    muSigOpenTradeChannelService.addMediationOpenedMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));
                                }
                            } else if (mediationCaseState == MediationCaseState.RE_OPENED) {
                                muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
                            } else if (mediationCaseState == MediationCaseState.CLOSED) {
                                if (message.getMuSigMediationResult().isEmpty()) {
                                    log.warn("Ignoring MuSigMediationStateChangeMessage with CLOSED state and missing MuSigMediationResult for trade {}.",
                                            message.getTradeId());
                                    pendingMuSigMediationStateChangeMessages.remove(message);
                                    return;
                                }
                                if (!hasValidMediatorSignature(channel, message)) {
                                    pendingMuSigMediationStateChangeMessages.remove(message);
                                    return;
                                }
                                // Closed mediation case still keeps mediator chat participation active.
                                muSigOpenTradeChannelService.setDisputeAgentType(channel, MuSigDisputeAgentType.MEDIATOR);
                            } else {
                                log.warn("Ignoring MuSigMediationStateChangeMessage with unsupported state {} for trade {}.",
                                        mediationCaseState, message.getTradeId());
                                pendingMuSigMediationStateChangeMessages.remove(message);
                                return;
                            }
                            pendingMuSigMediationStateChangeMessages.remove(message);
                        },
                        () -> {
                            log.warn("We received a MuSigMediationStateChangeMessage but did not find a matching muSigOpenTradeChannel for trade ID {}.\n" +
                                            "We add it to the pendingMuSigMediationStateChangeMessages set and reprocess it once a new trade channel has been added.",
                                    message.getTradeId());
                            pendingMuSigMediationStateChangeMessages.add(message);
                            if (channeldPin == null) {
                                channeldPin = muSigOpenTradeChannelService.getChannels().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void onAdded(MuSigOpenTradeChannel element) {
                                        // Delay and ignore too frequent updates
                                        if (throttleUpdatesScheduler == null) {
                                            throttleUpdatesScheduler = Scheduler.run(() -> {
                                                        maybeProcessPendingMessages();
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

    private boolean hasValidMediatorSignature(MuSigOpenTradeChannel channel,
                                              MuSigMediationStateChangeMessage message) {
        try {
            if (channel.getMediator().isEmpty()) {
                log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} because channel mediator is missing.",
                        message.getTradeId());
                return false;
            }
            if (message.getMediationResultSignature().isEmpty()) {
                log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} because mediator signature is missing.",
                        message.getTradeId());
                return false;
            }
            if (!MuSigMediationResultService.verifyMediationResult(message.getMuSigMediationResult().orElseThrow(),
                    message.getMediationResultSignature().orElseThrow(),
                    channel.getMediator().orElseThrow().getNetworkId().getPubKey().getPublicKey())) {
                log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} because mediator signature verification failed.",
                        message.getTradeId());
                return false;
            }
            return true;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Ignoring MuSigMediationStateChangeMessage for trade {} because mediator signature verification failed.",
                    message.getTradeId(), e);
            return false;
        }
    }

    private void maybeProcessPendingMessages() {
        new ArrayList<>(pendingMuSigMediationStateChangeMessages).forEach(this::processMediationStateChangeMessage);
    }
}
