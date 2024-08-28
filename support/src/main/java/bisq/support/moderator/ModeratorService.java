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
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkIdWithKeyPair;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

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

    @Getter
    private final ModeratorStore persistableStore = new ModeratorStore();
    @Getter
    private final Persistence<ModeratorStore> persistence;

    private final NetworkService networkService;
    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;
    private final Map<ChatChannelDomain, TwoPartyPrivateChatChannelService> twoPartyPrivateChatChannelServices;
    private final BannedUserService bannedUserService;
    private final boolean staticPublicKeysProvided;
    private final ChatService chatService;

    public ModeratorService(ModeratorService.Config config,
                            PersistenceService persistenceService,
                            NetworkService networkService,
                            UserService userService,
                            BondedRolesService bondedRolesService,
                            ChatService chatService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        twoPartyPrivateChatChannelServices = chatService.getTwoPartyPrivateChatChannelServices();
        bannedUserService = userService.getBannedUserService();
        staticPublicKeysProvided = config.isStaticPublicKeysProvided();
        this.chatService = chatService;
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
        if (envelopePayloadMessage instanceof ReportToModeratorMessage) {
            processReportToModeratorMessage((ReportToModeratorMessage) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ObservableSet<ReportToModeratorMessage> getReportToModeratorMessages() {
        return persistableStore.getReportToModeratorMessages();
    }

    public void reportUserProfile(UserProfile accusedUserProfile, String message, ChatChannelDomain chatChannelDomain) {
        UserIdentity myUserIdentity = userIdentityService.getSelectedUserIdentity();
        checkArgument(!bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile()));

        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        long date = System.currentTimeMillis();
        authorizedBondedRolesService.getAuthorizedBondedRoleStream().filter(e -> e.getBondedRoleType() == BondedRoleType.MODERATOR)
                .forEach(bondedRole -> {
                    String reportSenderUserProfileId = myUserIdentity.getUserProfile().getId();
                    ReportToModeratorMessage report = new ReportToModeratorMessage(date,
                            reportSenderUserProfileId,
                            accusedUserProfile,
                            message,
                            chatChannelDomain);
                    networkService.confidentialSend(report, bondedRole.getNetworkId(), senderNetworkIdWithKeyPair);
                });
    }

    public void deleteReportToModeratorMessage(ReportToModeratorMessage reportToModeratorMessage) {
        getReportToModeratorMessages().remove(reportToModeratorMessage);
        persist();
    }

    public CompletableFuture<BroadcastResult> banReportedUser(ReportToModeratorMessage message) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
        BannedUserProfileData data = new BannedUserProfileData(message.getAccusedUserProfile(), staticPublicKeysProvided);

        // Can be removed once there are no pre 2.1.0 versions out there anymore
        BannedUserProfileData oldVersion = new BannedUserProfileData(0, data.getUserProfile(), data.staticPublicKeysProvided());
        networkService.publishAuthorizedData(oldVersion, keyPair);

        return networkService.publishAuthorizedData(data, keyPair);
    }

    public CompletableFuture<BroadcastResult> unBanReportedUser(BannedUserProfileData data) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();

        // Can be removed once there are no pre 2.1.0 versions out there anymore
        BannedUserProfileData oldVersion = new BannedUserProfileData(0, data.getUserProfile(), data.staticPublicKeysProvided());
        networkService.removeAuthorizedData(oldVersion, keyPair);

        return networkService.removeAuthorizedData(data, keyPair);
    }

    public CompletableFuture<SendMessageResult> contactUser(ChatChannelDomain chatChannelDomain,
                                                            UserProfile userProfile,
                                                            Optional<String> citationMessage,
                                                            boolean isReportingUser) {
        if (chatChannelDomain == ChatChannelDomain.BISQ_EASY_OFFERBOOK ||
                chatChannelDomain == ChatChannelDomain.BISQ_EASY_OPEN_TRADES) {
            chatChannelDomain = ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT;
        }
        ChatChannelSelectionService selectionServices = chatService.getChatChannelSelectionServices().get(chatChannelDomain);
        if (!twoPartyPrivateChatChannelServices.containsKey(chatChannelDomain)) {
            return CompletableFuture.failedFuture(new RuntimeException("No twoPartyPrivateChatChannelService present for " + chatChannelDomain));
        }
        TwoPartyPrivateChatChannelService channelService = twoPartyPrivateChatChannelServices.get(chatChannelDomain);
        return channelService.findOrCreateChannel(chatChannelDomain, userProfile)
                .map(channel -> {
                    selectionServices.selectChannel(channel);

                    if (channel.getChatMessages().isEmpty() && isReportingUser) {
                        return channelService.sendTextMessage(Res.get("authorizedRole.moderator.replyMsg"),
                                citationMessage.map(msg -> new Citation(userProfile.getId(), msg)),
                                channel);
                    } else {
                        return CompletableFuture.completedFuture(new SendMessageResult());
                    }
                })
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No channel found")));
    }

    private void processReportToModeratorMessage(ReportToModeratorMessage message) {
        if (bannedUserService.isUserProfileBanned(message.getReporterUserProfileId())) {
            log.warn("Message ignored as sender is banned");
            return;
        }
        getReportToModeratorMessages().add(message);
        persist();
    }
}