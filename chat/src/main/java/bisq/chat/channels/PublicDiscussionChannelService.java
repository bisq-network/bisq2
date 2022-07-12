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

package bisq.chat.channels;

import bisq.chat.ChannelNotificationType;
import bisq.chat.messages.ChatMessage;
import bisq.chat.messages.PublicDiscussionChatMessage;
import bisq.chat.messages.Quotation;
import bisq.common.application.Service;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PublicDiscussionChannelService implements PersistenceClient<PublicDiscussionChannelStore>, 
        DataService.Listener, Service {
    @Getter
    private final PublicDiscussionChannelStore persistableStore = new PublicDiscussionChannelStore();
    @Getter
    private final Persistence<PublicDiscussionChannelStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;

    public PublicDiscussionChannelService(PersistenceService persistenceService,
                                          NetworkService networkService,
                                          UserIdentityService userIdentityService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
    }
    

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAllAuthenticatedPayload().forEach(this::onAuthenticatedDataAdded));
        maybeAddDefaultChannels();
        return CompletableFuture.completedFuture(true);
    }
    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicDiscussionChatMessage) {
            PublicDiscussionChatMessage message = (PublicDiscussionChatMessage) distributedData;
            findPublicDiscussionChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicDiscussionChatMessage(message, channel));
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicDiscussionChatMessage) {
            PublicDiscussionChatMessage message = (PublicDiscussionChatMessage) distributedData;
            findPublicDiscussionChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicDiscussionChatMessage(message, channel));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<DataService.BroadCastDataResult> publishDiscussionChatMessage(String text,
                                                                                           Optional<Quotation> quotedMessage,
                                                                                           PublicDiscussionChannel publicDiscussionChannel,
                                                                                           UserIdentity userIdentity) {
        UserProfile userProfile = userIdentity.getUserProfile();
        PublicDiscussionChatMessage chatMessage = new PublicDiscussionChatMessage(publicDiscussionChannel.getId(),
                userProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        return publish(userIdentity, userProfile, chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedDiscussionChatMessage(PublicDiscussionChatMessage originalChatMessage,
                                                                                                 String editedText,
                                                                                                 UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    UserProfile userProfile = userIdentity.getUserProfile();
                    PublicDiscussionChatMessage chatMessage = new PublicDiscussionChatMessage(originalChatMessage.getChannelId(),
                            userProfile.getId(),
                            editedText,
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return publish(userIdentity, userProfile, chatMessage);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicDiscussionChatMessage(PublicDiscussionChatMessage chatMessage,
                                                                                                UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(chatMessage, nodeIdAndKeyPair);
    }

    private void addPublicDiscussionChatMessage(PublicDiscussionChatMessage message, PublicDiscussionChannel channel) {
        channel.addChatMessage(message);
        persist();
    }

    private void removePublicDiscussionChatMessage(PublicDiscussionChatMessage message, PublicDiscussionChannel channel) {
        channel.removeChatMessage(message);
        persist();
    }

    public Optional<PublicDiscussionChannel> findPublicDiscussionChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    public ObservableSet<PublicDiscussionChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public Collection<? extends Channel<?>> getMentionableChannels() {
        // TODO: implement logic
        return getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, ChannelNotificationType channelNotificationType) {
        channel.getChannelNotificationType().set(channelNotificationType);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<DataService.BroadCastDataResult> publish(UserIdentity userIdentity,
                                                                       UserProfile userProfile,
                                                                       DistributedData distributedData) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return userIdentityService.maybePublicUserProfile(userProfile, nodeIdAndKeyPair)
                .thenCompose(result -> networkService.publishAuthenticatedData(distributedData, nodeIdAndKeyPair));
    }

    private void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }
        // todo channelAdmin not supported atm
        String channelAdminId = "";
        PublicDiscussionChannel defaultDiscussionChannel = new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BISQ.name(),
                "Discussions Bisq",
                "Channel for discussions about Bisq",
                channelAdminId,
                new HashSet<>()
        );
        ObservableSet<PublicDiscussionChannel> channels = getChannels();
        channels.add(defaultDiscussionChannel);
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BITCOIN.name(),
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.MONERO.name(),
                "Discussions Monero",
                "Channel for discussions about Monero",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.MARKETS.name(),
                "Price",
                "Channel for discussions about market price",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.ECONOMY.name(),
                "Economy",
                "Channel for discussions about economy",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.OFF_TOPIC.name(),
                "Off-topic",
                "Channel for anything else",
                channelAdminId,
                new HashSet<>()
        ));
        persist();
    }
}