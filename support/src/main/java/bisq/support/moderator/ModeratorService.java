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
import bisq.chat.ChatService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.message.Citation;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ModeratorService implements PersistenceClient<ModeratorStore>, Service, ConfidentialMessageListener, DataService.Listener {
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
    @javax.annotation.Nonnull
    private final ChatService chatService;
    private final UserProfileService userProfileService;

    public ModeratorService(PersistenceService persistenceService,
                            NetworkService networkService,
                            UserService userService,
                            BondedRolesService bondedRolesService,
                            ChatService chatService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        twoPartyPrivateChatChannelServices = chatService.getTwoPartyPrivateChatChannelServices();
        this.chatService = chatService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getDataService().ifPresent(service -> service.getAuthorizedData().forEach(this::onAuthorizedDataAdded));
        networkService.addDataServiceListener(this);
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConfidentialMessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage, PublicKey senderPublicKey) {
        if (networkMessage instanceof ReportToModeratorMessage) {
            ReportToModeratorMessage reportToModeratorMessage = ((ReportToModeratorMessage) networkMessage);
            getReportToModeratorMessages().add(reportToModeratorMessage);
            persist();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof BannedUserProfileData) {
            BannedUserProfileData bannedUserProfileData = (BannedUserProfileData) authorizedData.getAuthorizedDistributedData();
            getBannedUserProfileDataSet().add(bannedUserProfileData);
            persist();
        }
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof BannedUserProfileData) {
            BannedUserProfileData bannedUserProfileData = (BannedUserProfileData) authorizedData.getAuthorizedDistributedData();
            getBannedUserProfileDataSet().remove(bannedUserProfileData);
            persist();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void reportUserProfile(String reportedUserProfileId, String message, ChatChannelDomain chatChannelDomain) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            return;
        }

        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = selectedUserIdentity.getNodeIdAndKeyPair();
        long date = System.currentTimeMillis();
        authorizedBondedRolesService.getAuthorizedBondedRoleStream().filter(e -> e.getBondedRoleType() == BondedRoleType.MODERATOR)
                .forEach(bondedRole -> {
                    String reportSenderUserProfileId = selectedUserIdentity.getUserProfile().getId();
                    networkService.confidentialSend(new ReportToModeratorMessage(date, reportSenderUserProfileId, reportedUserProfileId, message, chatChannelDomain),
                            bondedRole.getNetworkId(), senderNetworkIdWithKeyPair);
                });

    }

    public void deleteReportToModeratorMessage(ReportToModeratorMessage reportToModeratorMessage) {
        getReportToModeratorMessages().remove(reportToModeratorMessage);
        persist();
    }

    public ObservableSet<ReportToModeratorMessage> getReportToModeratorMessages() {
        return persistableStore.getReportToModeratorMessages();
    }

    public ObservableSet<BannedUserProfileData> getBannedUserProfileDataSet() {
        return persistableStore.getBannedUserProfileDataSet();
    }

    public CompletableFuture<DataService.BroadCastDataResult> banReportedUser(ReportToModeratorMessage reportToModeratorMessage) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            return CompletableFuture.failedFuture(new RuntimeException("selectedUserIdentity must not be null"));
        }
        KeyPair keyPair = selectedUserIdentity.getNodeIdAndKeyPair().getKeyPair();
        BannedUserProfileData data = new BannedUserProfileData(reportToModeratorMessage.getAccusedUserProfileId(), false);
        return networkService.publishAuthorizedData(data, keyPair);
    }

    public CompletableFuture<DataService.BroadCastDataResult> unBanReportedUser(BannedUserProfileData data) {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        if (selectedUserIdentity == null) {
            return CompletableFuture.failedFuture(new RuntimeException("selectedUserIdentity must not be null"));
        }
        KeyPair keyPair = selectedUserIdentity.getNodeIdAndKeyPair().getKeyPair();
        return networkService.removeAuthorizedData(data, keyPair);
    }

    public CompletableFuture<NetworkService.SendMessageResult> contactUser(ChatChannelDomain chatChannelDomain,
                                                                           String userProfileId,
                                                                           Optional<String> citationMessage) {
        if (!twoPartyPrivateChatChannelServices.containsKey(chatChannelDomain)) {
            return CompletableFuture.failedFuture(new RuntimeException("No twoPartyPrivateChatChannelService present for " + chatChannelDomain));
        }
        TwoPartyPrivateChatChannelService channelService = twoPartyPrivateChatChannelServices.get(chatChannelDomain);

        return userProfileService.findUserProfile(userProfileId)
                .flatMap(userProfile -> channelService.findOrCreateChannel(chatChannelDomain, userProfile)
                        .map(channel -> channelService.sendTextMessage(Res.get("authorizedRole.moderator.replyMsg"),
                                citationMessage.map(msg -> new Citation(userProfileId, msg)),
                                channel)))
                .orElse(CompletableFuture.failedFuture(new RuntimeException("No userProfile found for " + userProfileId)));
    }

    public void openPrivateChannel(ReportToModeratorMessage reportToModeratorMessage) {
        ChatChannelDomain chatChannelDomain = reportToModeratorMessage.getChatChannelDomain();
        if (twoPartyPrivateChatChannelServices.containsKey(chatChannelDomain)) {
            String reportSenderUserProfileId = reportToModeratorMessage.getReporterUserProfileId();
            userProfileService.findUserProfile(reportSenderUserProfileId)
                    .ifPresent(userProfile -> chatService.createAndSelectTwoPartyPrivateChatChannel(chatChannelDomain, userProfile));
        }
        /*
        return userProfileService.findUserProfile(reportSenderUserProfileId)
                .flatMap(userProfile -> channelService.findOrCreateChannel(chatChannelDomain, userProfile)).stream().findAny();*/
    }
}