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
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service used by traders to select mediators, request mediation and process MediationResponses
 */
@Slf4j
public class MuSigMediationRequestService implements Service, ConfidentialMessageService.Listener {
    private final NetworkService networkService;
    private final UserProfileService userProfileService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;
    private final Set<MuSigMediatorsResponse> pendingMuSigMediatorsResponseMessages = new CopyOnWriteArraySet<>();
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
        pendingMuSigMediatorsResponseMessages.clear();
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigMediatorsResponse) {
            processMediationResponse((MuSigMediatorsResponse) envelopePayloadMessage);
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
                Optional.of(mediatorNetworkId));
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

    private void processMediationResponse(MuSigMediatorsResponse muSigMediatorsResponse) {
        muSigOpenTradeChannelService.findChannelByTradeId(muSigMediatorsResponse.getTradeId())
                .ifPresentOrElse(channel -> {
                            // Requester had it activated at request time
                            if (channel.isInMediation()) {
                                muSigOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));
                            } else {
                                muSigOpenTradeChannelService.setIsInMediation(channel, true);
                                muSigOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));

                                //todo (Critical) - check if we do sent from both peers
                                // Peer who has not requested sends their messages as well, so mediator can be sure to get all messages
                            }
                            pendingMuSigMediatorsResponseMessages.remove(muSigMediatorsResponse);
                        },
                        () -> {
                            // This handles an edge case that the MuSigMediatorsResponse arrives before the take offer request was
                            // processed (in case we are the maker and have been offline at take offer).
                            log.warn("We received a MuSigMediatorsResponse but did not find a matching muSigOpenTradeChannel for trade ID {}.\n" +
                                            "We add it to the pendingMuSigMediatorsResponseMessages set and reprocess it once a new trade channel has been added.",
                                    muSigMediatorsResponse.getTradeId());
                            pendingMuSigMediatorsResponseMessages.add(muSigMediatorsResponse);
                            if (channeldPin == null) {
                                channeldPin = muSigOpenTradeChannelService.getChannels().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void onAdded(MuSigOpenTradeChannel element) {
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

    private void maybeProcessPendingMediatorsResponseMessages() {
        new HashSet<>(pendingMuSigMediatorsResponseMessages).forEach(this::processMediationResponse);
    }
}

