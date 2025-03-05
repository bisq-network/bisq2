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

package bisq.support.moderator;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserProfileData;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ModeratorService implements PersistenceClient<ModeratorStore>, Service, ConfidentialMessageService.Listener {
    @Getter
    public static class Config {
        private final boolean staticPublicKeysProvided;

        public Config(boolean staticPublicKeysProvided) {
            this.staticPublicKeysProvided = staticPublicKeysProvided;
        }

        public static ModeratorService.Config from(com.typesafe.config.Config config) {
            return new ModeratorService.Config(config.getBoolean("staticPublicKeysProvided"));
        }
    }

    private final NetworkService networkService;
    private final ChatService chatService;
    private final UserIdentityService userIdentityService;
    private final TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;
    private final BannedUserService bannedUserService;
    private final UserProfileService userProfileService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final boolean staticPublicKeysProvided;

    @Getter
    private final ModeratorStore persistableStore = new ModeratorStore();
    @Getter
    private final Persistence<ModeratorStore> persistence;

    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private Pin rateLimitExceedingUserProfileIdMapPin;

    public ModeratorService(ModeratorService.Config config,
                            PersistenceService persistenceService,
                            NetworkService networkService,
                            UserService userService,
                            BondedRolesService bondedRolesService,
                            ChatService chatService) {
        this.networkService = networkService;
        this.chatService = chatService;
        userIdentityService = userService.getUserIdentityService();
        twoPartyPrivateChatChannelService = chatService.getTwoPartyPrivateChatChannelService();
        bannedUserService = userService.getBannedUserService();
        userProfileService = userService.getUserProfileService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        staticPublicKeysProvided = config.isStaticPublicKeysProvided();

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
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

        addObserverIfModerator();

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (rateLimitExceedingUserProfileIdMapPin != null) {
            rateLimitExceedingUserProfileIdMapPin.unbind();
            rateLimitExceedingUserProfileIdMapPin = null;
        }
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // MessageListener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof ReportToModeratorMessage) {
            processReportToModeratorMessage((ReportToModeratorMessage) envelopePayloadMessage);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void deleteReportToModeratorMessage(ReportToModeratorMessage reportToModeratorMessage) {
        getReportToModeratorMessages().remove(reportToModeratorMessage);
        persist();
    }

    public CompletableFuture<BroadcastResult> banReportedUser(ReportToModeratorMessage message) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
        BannedUserProfileData data = new BannedUserProfileData(message.getAccusedUserProfile(), staticPublicKeysProvided);
        return networkService.publishAuthorizedData(data, keyPair);
    }

    public CompletableFuture<BroadcastResult> unBanReportedUser(BannedUserProfileData data) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
        return networkService.removeAuthorizedData(data, keyPair);
    }

    public CompletableFuture<SendMessageResult> contactUser(UserProfile userProfile,
                                                            Optional<String> citationMessage,
                                                            boolean isReportingUser) {
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        ChatChannelSelectionService selectionServices = chatService.getChatChannelSelectionServices().get(chatChannelDomain);
        return twoPartyPrivateChatChannelService.findOrCreateChannel(chatChannelDomain, userProfile)
                .map(channel -> {
                    selectionServices.selectChannel(channel);

                    if (channel.getChatMessages().isEmpty() && isReportingUser) {
                        return twoPartyPrivateChatChannelService.sendTextMessage(Res.get("authorizedRole.moderator.replyMsg"),
                                citationMessage.map(msg -> new Citation(userProfile.getId(), msg, "")),
                                channel);
                    } else {
                        return CompletableFuture.completedFuture(new SendMessageResult());
                    }
                })
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No channel found")));
    }

    public ObservableSet<ReportToModeratorMessage> getReportToModeratorMessages() {
        return persistableStore.getReportToModeratorMessages();
    }

    private void processReportToModeratorMessage(ReportToModeratorMessage message) {
        if (bannedUserService.isUserProfileBanned(message.getReporterUserProfileId())) {
            log.warn("Message ignored as sender is banned");
            return;
        }
        getReportToModeratorMessages().add(message);
        persist();
    }

    private void addObserverIfModerator() {
        Set<String> myUserProfileIds = userIdentityService.getMyUserProfileIds();
        authorizedBondedRolesService.getBondedRoles().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BondedRole bondedRole) {
                AuthorizedBondedRole authorizedBondedRole = bondedRole.getAuthorizedBondedRole();
                boolean isMyProfile = myUserProfileIds.contains(authorizedBondedRole.getProfileId());
                if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.MODERATOR &&
                        isMyProfile &&
                        rateLimitExceedingUserProfileIdMapPin == null) {
                    rateLimitExceedingUserProfileIdMapPin = bannedUserService.getRateLimitExceedingUserProfiles().addObserver(new CollectionObserver<>() {
                        @Override
                        public void add(String userProfileId) {
                            selfReportRateLimitExceedingUserProfileId(userProfileId);
                        }

                        @Override
                        public void remove(Object element) {
                        }

                        @Override
                        public void clear() {
                        }
                    });
                }
            }

            @Override
            public void remove(Object element) {
            }

            @Override
            public void clear() {
            }
        });
    }

    private void selfReportRateLimitExceedingUserProfileId(String userProfileId) {
        ObservableSet<ReportToModeratorMessage> reportToModeratorMessages = getReportToModeratorMessages();
        boolean notYetReported = reportToModeratorMessages.stream()
                .noneMatch(message -> message.getAccusedUserProfile().getId().equals(userProfileId));
        if (notYetReported) {
            userProfileService.findUserProfile(userProfileId)
                    .ifPresent(accusedUserProfile -> {
                        String myUserProfileId = userIdentityService.getSelectedUserIdentity().getUserProfile().getId();
                        ReportToModeratorMessage report = new ReportToModeratorMessage(System.currentTimeMillis(),
                                myUserProfileId,
                                accusedUserProfile,
                                "Moderator self-reported user who exceeded message rate limit", // Only for moderator, thus not translated
                                ChatChannelDomain.DISCUSSION);
                        reportToModeratorMessages.add(report);
                        persist();
                    });
        }
    }
}