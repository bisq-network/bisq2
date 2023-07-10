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

package bisq.chat.channel.pub;

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Citation;
import bisq.chat.message.PublicChatMessage;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.persistence.PersistableStore;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class PublicChatChannelService<M extends PublicChatMessage, C extends PublicChatChannel<M>, S extends PersistableStore<S>>
        extends ChatChannelService<M, C, S> implements DataService.Listener {

    public PublicChatChannelService(NetworkService networkService,
                                    UserIdentityService userIdentityService,
                                    UserProfileService userProfileService,
                                    ChatChannelDomain chatChannelDomain) {
        super(networkService, userIdentityService, userProfileService, chatChannelDomain);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        maybeAddDefaultChannels();

        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(dataService ->
                dataService.getAllAuthenticatedData().forEach(this::onAuthenticatedDataAdded));

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
                                                                                 Optional<Citation> citation,
                                                                                 C publicChannel,
                                                                                 UserIdentity userIdentity) {
        M chatMessage = createChatMessage(text, citation, publicChannel, userIdentity.getUserProfile());
        return publishChatMessage(chatMessage, userIdentity);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishChatMessage(M chatMessage,
                                                                                 UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return userIdentityService.maybePublicUserProfile(userIdentity.getUserProfile(), nodeIdAndKeyPair)
                .thenCompose(result -> networkService.publishAuthenticatedData(chatMessage, nodeIdAndKeyPair));
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedChatMessage(M originalChatMessage,
                                                                                       String editedText,
                                                                                       UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    M chatMessage = createEditedChatMessage(originalChatMessage, editedText, userIdentity.getUserProfile());
                    return publishChatMessage(chatMessage, userIdentity);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deleteChatMessage(M chatMessage,
                                                                                UserIdentity userIdentity) {
        return networkService.removeAuthenticatedData(chatMessage, userIdentity.getNodeIdAndKeyPair());
    }

    public Collection<C> getMentionableChannels() {
        // TODO: implement logic
        return getChannels();
    }

    @Override
    public String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel) {
        return "";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void processAddedMessage(M message) {
        findChannel(message).ifPresent(channel -> addMessage(message, channel));
    }

    protected void processRemovedMessage(M message) {
        findChannel(message).ifPresent(channel -> removeMessage(message, channel));
    }

    protected void removeMessage(M message, C channel) {
        synchronized (getPersistableStore()) {
            channel.removeChatMessage(message);
        }
        persist();
    }

    protected abstract M createChatMessage(String text,
                                           Optional<Citation> citation,
                                           C publicChannel,
                                           UserProfile userProfile);

    protected abstract M createEditedChatMessage(M originalChatMessage, String editedText, UserProfile userProfile);

    protected abstract void maybeAddDefaultChannels();
}