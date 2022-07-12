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
import bisq.chat.messages.PublicChatMessage;
import bisq.chat.messages.Quotation;
import bisq.common.application.Service;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class PublicChannelService<M extends PublicChatMessage, C extends PublicChannel<M>, S extends PersistableStore<S>>
        implements DataService.Listener, Service, PersistenceClient<S> {
    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;

    public PublicChannelService(NetworkService networkService,
                                UserIdentityService userIdentityService) {
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
        networkService.getDataService().ifPresent(dataService ->
                dataService.getAllAuthenticatedPayload().forEach(this::onAuthenticatedDataAdded));
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
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<DataService.BroadCastDataResult> publishChatMessage(String text,
                                                                                 Optional<Quotation> quotedMessage,
                                                                                 C publicChannel,
                                                                                 UserIdentity userIdentity) {
        M chatMessage = createNewChatMessage(text, quotedMessage, publicChannel, userIdentity.getUserProfile());
        return publishChatMessage(chatMessage, userIdentity);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishChatMessage(M chatMessage,
                                                                                 UserIdentity userIdentity) {
        return publishChatMessage(userIdentity, userIdentity.getUserProfile(), chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedChatMessage(M originalChatMessage,
                                                                                       String editedText,
                                                                                       UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    M chatMessage = createNewChatMessage(originalChatMessage, editedText, userIdentity.getUserProfile());
                    return publishChatMessage(chatMessage, userIdentity);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deleteChatMessage(M chatMessage,
                                                                                UserIdentity userIdentity) {
        return networkService.removeAuthenticatedData(chatMessage, userIdentity.getNodeIdAndKeyPair());
    }

    public Optional<C> findChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    public abstract ObservableSet<C> getChannels();

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, ChannelNotificationType channelNotificationType) {
        channel.getChannelNotificationType().set(channelNotificationType);
        persist();
    }

    public Collection<C> getMentionableChannels() {
        // TODO: implement logic
        return getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void processAddedMessage(M message) {
        findChannel(message.getChannelId())
                .ifPresent(channel -> addMessage(message, channel));
    }

    protected void processRemovedMessage(M message) {
        findChannel(message.getChannelId())
                .ifPresent(channel -> removeMessage(message, channel));
    }

    protected void addMessage(M message, C channel) {
        channel.addChatMessage(message);
        persist();
    }

    protected void removeMessage(M message, C channel) {
        channel.removeChatMessage(message);
        persist();
    }

    protected abstract M createNewChatMessage(String text,
                                              Optional<Quotation> quotedMessage,
                                              C publicChannel,
                                              UserProfile userProfile);

    protected abstract M createNewChatMessage(M originalChatMessage, String editedText, UserProfile userProfile);

    protected CompletableFuture<DataService.BroadCastDataResult> publishChatMessage(UserIdentity userIdentity,
                                                                                    UserProfile userProfile,
                                                                                    DistributedData distributedData) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return userIdentityService.maybePublicUserProfile(userProfile, nodeIdAndKeyPair)
                .thenCompose(result -> networkService.publishAuthenticatedData(distributedData, nodeIdAndKeyPair));
    }

    protected abstract void maybeAddDefaultChannels();
}