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

package bisq.chat.pub;

import bisq.chat.*;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.PersistableStore;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class PublicChatChannelService<M extends PublicChatMessage, C extends PublicChatChannel<M>,
        S extends PersistableStore<S>, R extends ChatMessageReaction> extends ChatChannelService<M, C, S> implements DataService.Listener {

    public PublicChatChannelService(NetworkService networkService,
                                    UserService userService,
                                    ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, chatChannelDomain);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        maybeAddDefaultChannels();

        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(dataService ->
                dataService.getAuthenticatedData().forEach(this::handleAuthenticatedDataAdded));

        return CompletableFuture.completedFuture(true);
    }

    protected abstract void handleAuthenticatedDataAdded(AuthenticatedData authenticatedData);

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> publishChatMessage(String text,
                                                                 Optional<Citation> citation,
                                                                 C publicChannel,
                                                                 UserIdentity userIdentity) {
        M chatMessage = createChatMessage(text, citation, publicChannel, userIdentity.getUserProfile());
        return publishChatMessage(chatMessage, userIdentity);
    }

    public CompletableFuture<BroadcastResult> publishChatMessage(M message,
                                                                 UserIdentity userIdentity) {
        if (bannedUserService.isUserProfileBanned(message.getAuthorUserProfileId())) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }
        // Sender adds the message at sending to avoid the delayed display if using the received message from the network.
        findChannel(message.getChannelId()).ifPresent(channel -> addMessage(message, channel));

        KeyPair keyPair = userIdentity.getNetworkIdWithKeyPair().getKeyPair();
        return networkService.publishAuthenticatedData(message, keyPair);
    }

    public CompletableFuture<BroadcastResult> publishEditedChatMessage(M originalChatMessage,
                                                                       String editedText,
                                                                       UserIdentity userIdentity) {
        KeyPair ownerKeyPair = userIdentity.getNetworkIdWithKeyPair().getKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, ownerKeyPair)
                .thenCompose(result -> {
                    M chatMessage = createEditedChatMessage(originalChatMessage, editedText, userIdentity.getUserProfile());
                    return publishChatMessage(chatMessage, userIdentity);
                });
    }

    public CompletableFuture<BroadcastResult> deleteChatMessage(M chatMessage, NetworkIdWithKeyPair networkIdWithKeyPair) {
        return networkService.removeAuthenticatedData(chatMessage, networkIdWithKeyPair.getKeyPair());
    }

    public CompletableFuture<BroadcastResult> publishChatMessageReaction(M message,
                                                                         Reaction reaction,
                                                                         UserIdentity userIdentity) {
        if (bannedUserService.isUserProfileBanned(userIdentity.getId())) {
            return CompletableFuture.failedFuture(new RuntimeException());
        }

        R chatMessageReaction = createChatMessageReaction(message, reaction, userIdentity);
        checkArgument(chatMessageReaction instanceof DistributedData, "A public chat message reaction needs to implement DistributedData.");

        // Sender adds the message at sending to avoid the delayed display if using the received message from the network.
        addMessageReaction(chatMessageReaction, message);

        KeyPair keyPair = userIdentity.getNetworkIdWithKeyPair().getKeyPair();
        return networkService.publishAuthenticatedData((DistributedData) chatMessageReaction, keyPair);
    }

    public CompletableFuture<BroadcastResult> deleteChatMessageReaction(R chatMessageReaction, NetworkIdWithKeyPair networkIdWithKeyPair) {
        checkArgument(chatMessageReaction instanceof DistributedData, "A public chat message reaction needs to implement DistributedData.");
        return networkService.removeAuthenticatedData((DistributedData) chatMessageReaction, networkIdWithKeyPair.getKeyPair());
    }

    @Override
    public String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel) {
        return "";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void processAddedMessage(M message) {
        if (canHandleChannelDomain(message) && isValid(message)) {
            findChannel(message).ifPresent(channel -> addMessage(message, channel));
        }
    }

    protected void processRemovedMessage(M message) {
        if (canHandleChannelDomain(message) && isValid(message)) {
            findChannel(message).ifPresent(channel -> removeMessage(message, channel));
        }
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

    protected void processAddedReaction(R chatMessageReaction) {
        findChannel(chatMessageReaction.getChatChannelId())
                .flatMap(channel -> channel.getChatMessages().stream()
                        .filter(message -> message.getId().equals(chatMessageReaction.getChatMessageId()))
                        .findFirst())
                .ifPresent(message -> addMessageReaction(chatMessageReaction, message));
    }

    protected void processRemovedReaction(R chatMessageReaction) {
        findChannel(chatMessageReaction.getChatChannelId())
                .flatMap(channel -> channel.getChatMessages().stream()
                        .filter(message -> message.getId().equals(chatMessageReaction.getChatMessageId()))
                        .findFirst())
                .ifPresent(message -> removeMessageReaction(chatMessageReaction, message));
    }

    protected abstract R createChatMessageReaction(M message, Reaction reaction, UserIdentity userIdentity);
}
