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
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class PublicDiscussionChannelService extends ChannelService<PublicDiscussionChannel>
        implements PersistenceClient<PublicDiscussionChannelStore>, DataService.Listener {
    @Getter
    private final PublicDiscussionChannelStore persistableStore = new PublicDiscussionChannelStore();
    @Getter
    private final Persistence<PublicDiscussionChannelStore> persistence;
    private final ProofOfWorkService proofOfWorkService;
    private final Map<String, UserProfile> chatUserById = new ConcurrentHashMap<>();

    public PublicDiscussionChannelService(PersistenceService persistenceService,
                                          NetworkService networkService,
                                          UserIdentityService userIdentityService,
                                          ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.proofOfWorkService = proofOfWorkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAllAuthenticatedPayload().forEach(this::onAuthenticatedDataAdded));
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();

        // We do not have control about the order of the data we receive. 
        // The chat user is required for showing a chat message. In case we get the messages first, we keep it in our
        // list but the UI will not show it as the chat user is missing. After we get the associated chat user we 
        // re-apply the messages to the list, triggering a refresh in the UI list.
        // We do not persist the chat user as it is kept in the p2p store, and we use the TTL for its validity.

        if (distributedData instanceof UserProfile &&
                hasAuthorValidProofOfWork(((UserProfile) distributedData).getProofOfWork())) {
            // Only if we have not already that chatUser we apply it
            UserProfile userProfile = (UserProfile) distributedData;
            Optional<UserProfile> optionalChatUser = findChatUser(userProfile.getId());
            if (optionalChatUser.isEmpty()) {
                log.info("We got a new chatUser {}", userProfile);
                // It might be that we received the chat message before the chat user. In that case the 
                // message would not be displayed. To avoid this situation we check if there are messages containing the 
                // new chat user and if so, we remove and later add the messages to trigger an update for the clients.
                Set<PublicDiscussionChatMessage> publicDiscussionChatMessages = getChannels().stream()
                        .flatMap(channel -> channel.getChatMessages().stream())
                        .filter(message -> message.getAuthorId().equals(userProfile.getId()))
                        .collect(Collectors.toSet());

                if (!publicDiscussionChatMessages.isEmpty()) {
                    log.info("We have {} publicDiscussionChatMessages with that chat users ID which have not been displayed yet. " +
                            "We remove them and add them to trigger a list update.", publicDiscussionChatMessages.size());
                }

                // Remove chat messages containing that chatUser
                publicDiscussionChatMessages.forEach(message ->
                        findPublicDiscussionChannel(message.getChannelId())
                                .ifPresent(channel -> removePublicDiscussionChatMessage(message, channel)));

                putChatUser(userProfile);

                // Now we add them again
                publicDiscussionChatMessages.forEach(message ->
                        findPublicDiscussionChannel(message.getChannelId())
                                .ifPresent(channel -> addPublicDiscussionChatMessage(message, channel)));
            } else if (!optionalChatUser.get().equals(userProfile)) {
                // We have that chat user but data are different (e.g. edited user)
                putChatUser(userProfile);

            }
        } else if (distributedData instanceof PublicDiscussionChatMessage &&
                isValidProofOfWorkOrChatUserNotFound((PublicDiscussionChatMessage) distributedData)) {
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

    public Optional<UserProfile> findChatUser(String chatUserId) {
        return Optional.ofNullable(chatUserById.get(chatUserId));
    }

    private void putChatUser(UserProfile userProfile) {
        chatUserById.put(userProfile.getId(), userProfile);
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

    private boolean hasAuthorValidProofOfWork(ProofOfWork proofOfWork) {
        return proofOfWorkService.verify(proofOfWork);
    }

    private boolean isValidProofOfWorkOrChatUserNotFound(ChatMessage message) {
        // In case we don't find the chat user we still return true as it might be that the chat user gets added later.
        // In that case we check again if the chat user has a valid proof of work.
        return findChatUser(message.getAuthorId())
                .map(chatUser -> hasAuthorValidProofOfWork(chatUser.getProofOfWork()))
                .orElse(true);
    }
}