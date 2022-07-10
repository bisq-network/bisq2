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

package bisq.social.chat;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.chat.channels.*;
import bisq.social.chat.messages.*;
import bisq.social.offer.TradeChatOffer;
import bisq.identity.profile.PublicUserProfile;
import bisq.identity.profile.UserProfile;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages chatChannels and persistence of the chatModel.
 * ChatUser and ChatIdentity management is not implemented yet. Not 100% clear yet if ChatIdentity management should
 * be rather part of the identity module.
 */
@Slf4j
@Getter
public class ChatService implements PersistenceClient<ChatStore>, MessageListener, DataService.Listener {
    private final ChatStore persistableStore = new ChatStore();
    private final Persistence<ChatStore> persistence;
    private final ChatUserService chatUserService;
    private final PersistenceService persistenceService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final ProofOfWorkService proofOfWorkService;
    private final Map<String, PublicUserProfile> chatUserById = new ConcurrentHashMap<>();

    public ChatService(PersistenceService persistenceService,
                       IdentityService identityService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       ChatUserService chatUserService) {
        this.persistenceService = persistenceService;
        this.identityService = identityService;
        this.proofOfWorkService = proofOfWorkService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.chatUserService = chatUserService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        maybeAddDefaultChannels();
        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAllAuthenticatedPayload().forEach(this::onAuthenticatedDataAdded));

        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateTradeChatMessage) {
            PrivateTradeChatMessage message = (PrivateTradeChatMessage) networkMessage;
            if (!isMyMessage(message) && hasAuthorValidProofOfWork(message.getAuthor().getProofOfWork())) {
                getOrCreatePrivateTradeChannel(message)
                        .ifPresent(channel -> addPrivateTradeChatMessage(message, channel));
            }
        } else if (networkMessage instanceof PrivateDiscussionChatMessage) {
            PrivateDiscussionChatMessage message = (PrivateDiscussionChatMessage) networkMessage;
            if (!isMyMessage(message) && hasAuthorValidProofOfWork(message.getAuthor().getProofOfWork())) {
                getOrCreatePrivateDiscussionChannel(message)
                        .ifPresent(channel -> addPrivateDiscussionChatMessage(message, channel));
            }
        }
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

        if (distributedData instanceof PublicUserProfile &&
                hasAuthorValidProofOfWork(((PublicUserProfile) distributedData).getProofOfWork())) {
            // Only if we have not already that chatUser we apply it
            PublicUserProfile publicUserProfile = (PublicUserProfile) distributedData;
            Optional<PublicUserProfile> optionalChatUser = findChatUser(publicUserProfile.getId());
            if (optionalChatUser.isEmpty()) {
                log.info("We got a new chatUser {}", publicUserProfile);
                // It might be that we received the chat message before the chat user. In that case the 
                // message would not be displayed. To avoid this situation we check if there are messages containing the 
                // new chat user and if so, we remove and later add the messages to trigger an update for the clients.
                Set<PublicDiscussionChatMessage> publicDiscussionChatMessages = getPublicDiscussionChannels().stream()
                        .flatMap(channel -> channel.getChatMessages().stream())
                        .filter(message -> message.getAuthorId().equals(publicUserProfile.getId()))
                        .collect(Collectors.toSet());
                Set<PublicTradeChatMessage> publicTradeChatMessages = getPublicTradeChannels().stream()
                        .flatMap(channel -> channel.getChatMessages().stream())
                        .filter(message -> message.getAuthorId().equals(publicUserProfile.getId()))
                        .collect(Collectors.toSet());

                if (!publicDiscussionChatMessages.isEmpty()) {
                    log.info("We have {} publicDiscussionChatMessages with that chat users ID which have not been displayed yet. " +
                            "We remove them and add them to trigger a list update.", publicDiscussionChatMessages.size());
                }
                if (!publicTradeChatMessages.isEmpty()) {
                    log.info("We have {} publicTradeChatMessages with that chat users ID which have not been displayed yet. " +
                            "We remove them and add them to trigger a list update.", publicTradeChatMessages.size());
                }

                // Remove chat messages containing that chatUser
                publicDiscussionChatMessages.forEach(message ->
                        findPublicDiscussionChannel(message.getChannelId())
                                .ifPresent(channel -> removePublicDiscussionChatMessage(message, channel)));
                publicTradeChatMessages.forEach(message ->
                        findPublicTradeChannel(message.getChannelId())
                                .ifPresent(channel -> removePublicTradeChatMessage(message, channel)));

                putChatUser(publicUserProfile);

                // Now we add them again
                publicDiscussionChatMessages.forEach(message ->
                        findPublicDiscussionChannel(message.getChannelId())
                                .ifPresent(channel -> addPublicDiscussionChatMessage(message, channel)));
                publicTradeChatMessages.forEach(message ->
                        findPublicTradeChannel(message.getChannelId())
                                .ifPresent(channel -> addPublicTradeChatMessage(message, channel)));
            } else if (!optionalChatUser.get().equals(publicUserProfile)) {
                // We have that chat user but data are different (e.g. edited user)
                putChatUser(publicUserProfile);
                chatUserService.replaceChatUser(publicUserProfile);

            }
        } else if (distributedData instanceof PublicTradeChatMessage &&
                isValidProofOfWorkOrChatUserNotFound((PublicTradeChatMessage) distributedData)) {
            PublicTradeChatMessage message = (PublicTradeChatMessage) distributedData;
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicTradeChatMessage(message, channel));
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
        if (distributedData instanceof PublicTradeChatMessage) {
            PublicTradeChatMessage message = (PublicTradeChatMessage) distributedData;
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicTradeChatMessage(message, channel));
        } else if (distributedData instanceof PublicDiscussionChatMessage) {
            PublicDiscussionChatMessage message = (PublicDiscussionChatMessage) distributedData;
            findPublicDiscussionChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicDiscussionChatMessage(message, channel));
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Public Trade domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<PublicTradeChannel> showPublicTradeChannel(Optional<Market> market) {
        return findPublicTradeChannel(PublicTradeChannel.getId(market))
                .map(channel -> {
                    channel.setVisible(true);
                    persist();
                    return channel;
                });
    }

    public void hidePublicTradeChannel(PublicTradeChannel channel) {
        channel.setVisible(false);
        persist();
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatTextMessage(String text,
                                                                                          Optional<Quotation> quotedMessage,
                                                                                          PublicTradeChannel publicTradeChannel,
                                                                                          UserProfile userProfile) {
        PublicUserProfile publicUserProfile = userProfile.getPublicUserProfile();
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                publicUserProfile.getId(),
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
        return publish(userProfile, publicUserProfile, chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatOffer(TradeChatOffer tradeChatOffer,
                                                                                    PublicTradeChannel publicTradeChannel,
                                                                                    UserProfile userProfile) {
        PublicUserProfile publicUserProfile = userProfile.getPublicUserProfile();
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                publicUserProfile.getId(),
                Optional.of(tradeChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        return publish(userProfile, publicUserProfile, chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishPublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                            UserProfile userProfile) {
        return publish(userProfile, userProfile.getPublicUserProfile(), chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedTradeChatMessage(PublicTradeChatMessage originalChatMessage,
                                                                                            String editedText,
                                                                                            UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    // We do not support editing the MarketChatOffer directly but remove it and replace it with 
                    // the edited text.
                    PublicUserProfile publicUserProfile = userProfile.getPublicUserProfile();
                    PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(originalChatMessage.getChannelId(),
                            publicUserProfile.getId(),
                            Optional.empty(),
                            Optional.of(editedText),
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return publish(userProfile, publicUserProfile, chatMessage);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                           UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(chatMessage, nodeIdAndKeyPair);
    }

    public Optional<PublicTradeChannel> findPublicTradeChannel(String channelId) {
        return getPublicTradeChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    private void addPublicTradeChatMessage(PublicTradeChatMessage message, PublicTradeChannel channel) {
        channel.addChatMessage(message);
        persist();
    }

    private void removePublicTradeChatMessage(PublicTradeChatMessage message, PublicTradeChannel channel) {
        channel.removeChatMessage(message);
        persist();
    }

    public ObservableSet<PublicTradeChannel> getPublicTradeChannels() {
        return persistableStore.getPublicTradeChannels();
    }

    public void maybeAddPublicTradeChannel(PublicTradeChannel channel) {
        if (!getPublicTradeChannels().contains(channel)) {
            getPublicTradeChannels().add(channel);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Trade domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateTradeChatMessage(String text,
                                                                                           Optional<Quotation> quotedMessage,
                                                                                           PrivateTradeChannel privateTradeChannel) {
        String channelId = privateTradeChannel.getId();
        UserProfile userProfile = privateTradeChannel.getMyProfile();
        PublicUserProfile peer = privateTradeChannel.getPeer();
        PrivateTradeChatMessage chatMessage = new PrivateTradeChatMessage(channelId,
                userProfile.getPublicUserProfile(),
                peer.getNym(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateTradeChatMessage(chatMessage, privateTradeChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userProfile.getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public Optional<PrivateTradeChannel> getOrCreatePrivateTradeChannel(PrivateTradeChatMessage privateTradeChatMessage) {
        return findPrivateTradeChannel(privateTradeChatMessage.getChannelId())
                .or(() -> createPrivateTradeChannel(privateTradeChatMessage.getAuthor(), privateTradeChatMessage.getReceiversNym()));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(PublicUserProfile peer) {
        return Optional.ofNullable(chatUserService.getSelectedChatUserIdentity().get())
                .flatMap(userProfile -> createPrivateTradeChannel(peer, userProfile.getProfileId()));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(PublicUserProfile peer, String receiversProfileId) {
        return chatUserService.findChatUserIdentity(receiversProfileId)
                .map(myUserProfile -> {
                            PrivateTradeChannel privateTradeChannel = new PrivateTradeChannel(peer, myUserProfile);
                            getPrivateTradeChannels().add(privateTradeChannel);
                            persist();
                            return privateTradeChannel;
                        }
                );
    }

    public void addPrivateTradeChatMessage(PrivateTradeChatMessage chatMessage, PrivateTradeChannel privateTradeChannel) {
        synchronized (persistableStore) {
            privateTradeChannel.addChatMessage(chatMessage);
        }
        persist();
    }

    public Optional<PrivateTradeChannel> findPrivateTradeChannel(String channelId) {
        return getPrivateTradeChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    public ObservableSet<PrivateTradeChannel> getPrivateTradeChannels() {
        return persistableStore.getPrivateTradeChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Selected Trade channel
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<Channel<? extends ChatMessage>> getSelectedTradeChannel() {
        return persistableStore.getSelectedTradeChannel();
    }

    public void selectTradeChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateTradeChannel) {
            // remove expired messages
            purgePrivateTradeChannel((PrivateTradeChannel) channel);
        }
        getSelectedTradeChannel().set(channel);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Public Discussion domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

   /* public CompletableFuture<Optional<PublicDiscussionChannel>> addPublicDiscussionChannel(ChatUserIdentity chatUserIdentity,
                                                                                           String channelName,
                                                                                           String description) {
        return chatUserIdentity.getChatUser().getRoles().stream()
                .filter(entitlement -> entitlement.type() == Role.Type.CHANNEL_ADMIN)
                .filter(entitlement -> entitlement.proof() instanceof BondedRoleProof)
                .map(entitlement -> (BondedRoleProof) entitlement.proof())
                .map(bondedRoleProof -> chatUserService.verifyBondedRole(bondedRoleProof.txId(),
                        bondedRoleProof.signature(),
                        chatUserIdentity.getChatUser().getId()))
                .map(future -> future.thenApply(optionalProof -> optionalProof.map(proof -> {
                            ChatUser chatUser = new ChatUser(chatUserIdentity.getChatUser().getNickName(),
                                    chatUserIdentity.getIdentity().proofOfWork(),
                                    chatUserIdentity.getIdentity().networkId(),
                                    chatUserIdentity.getChatUser().getReputation(),
                                    chatUserIdentity.getChatUser().getRoles());
                            PublicDiscussionChannel publicDiscussionChannel = new PublicDiscussionChannel(StringUtils.createUid(),
                                    channelName,
                                    description,
                                    chatUser.getId(),
                                    new HashSet<>());
                            getPublicDiscussionChannels().add(publicDiscussionChannel);
                            persist();
                            return Optional.of(publicDiscussionChannel);
                        })
                        .orElse(Optional.empty())))
                .findAny()
                .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }*/

    public CompletableFuture<DataService.BroadCastDataResult> publishDiscussionChatMessage(String text,
                                                                                           Optional<Quotation> quotedMessage,
                                                                                           PublicDiscussionChannel publicDiscussionChannel,
                                                                                           UserProfile userProfile) {
        PublicUserProfile publicUserProfile = userProfile.getPublicUserProfile();
        PublicDiscussionChatMessage chatMessage = new PublicDiscussionChatMessage(publicDiscussionChannel.getId(),
                publicUserProfile.getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        return publish(userProfile, publicUserProfile, chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedDiscussionChatMessage(PublicDiscussionChatMessage originalChatMessage,
                                                                                                 String editedText,
                                                                                                 UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    PublicUserProfile publicUserProfile = userProfile.getPublicUserProfile();
                    PublicDiscussionChatMessage chatMessage = new PublicDiscussionChatMessage(originalChatMessage.getChannelId(),
                            publicUserProfile.getId(),
                            editedText,
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return publish(userProfile, publicUserProfile, chatMessage);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicDiscussionChatMessage(PublicDiscussionChatMessage chatMessage,
                                                                                                UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getNodeIdAndKeyPair();
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
        return getPublicDiscussionChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    public ObservableSet<PublicDiscussionChannel> getPublicDiscussionChannels() {
        return persistableStore.getPublicDiscussionChannels();
    }

    public Collection<? extends Channel<?>> getMentionableChannels() {
        // TODO: implement logic
        return getPublicDiscussionChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Discussion domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateDiscussionChatMessage(String text,
                                                                                                Optional<Quotation> quotedMessage,
                                                                                                PrivateDiscussionChannel privateDiscussionChannel) {
        String channelId = privateDiscussionChannel.getId();
        UserProfile userProfile = privateDiscussionChannel.getMyProfile();
        PublicUserProfile peer = privateDiscussionChannel.getPeer();
        PrivateDiscussionChatMessage chatMessage = new PrivateDiscussionChatMessage(channelId,
                userProfile.getPublicUserProfile(),
                peer.getNym(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateDiscussionChatMessage(chatMessage, privateDiscussionChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userProfile.getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public Optional<PrivateDiscussionChannel> getOrCreatePrivateDiscussionChannel(PrivateDiscussionChatMessage message) {
        return findPrivateDiscussionChannel(message.getChannelId())
                .or(() -> createPrivateDiscussionChannel(message.getAuthor(), message.getReceiversNym()));
    }

    public Optional<PrivateDiscussionChannel> createPrivateDiscussionChannel(PublicUserProfile peer) {
        return Optional.ofNullable(chatUserService.getSelectedChatUserIdentity().get())
                .flatMap(e -> createPrivateDiscussionChannel(peer, e.getProfileId()));
    }

    public Optional<PrivateDiscussionChannel> createPrivateDiscussionChannel(PublicUserProfile peer, String receiversProfileId) {
        return chatUserService.findChatUserIdentity(receiversProfileId)
                .map(myUserProfile -> {
                            PrivateDiscussionChannel privateDiscussionChannel = new PrivateDiscussionChannel(peer, myUserProfile);
                            getPrivateDiscussionChannels().add(privateDiscussionChannel);
                            persist();
                            return privateDiscussionChannel;
                        }
                );
    }

    public void addPrivateDiscussionChatMessage(PrivateDiscussionChatMessage chatMessage, PrivateDiscussionChannel privateDiscussionChannel) {
        synchronized (persistableStore) {
            privateDiscussionChannel.addChatMessage(chatMessage);
        }
        persist();
    }

    public Optional<PrivateDiscussionChannel> findPrivateDiscussionChannel(String channelId) {
        return getPrivateDiscussionChannels().stream().filter(channel -> channel.getId().equals(channelId)).findAny();
    }

    public ObservableSet<PrivateDiscussionChannel> getPrivateDiscussionChannels() {
        return persistableStore.getPrivateDiscussionChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Selected Discussion Channel
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<Channel<? extends ChatMessage>> getSelectedDiscussionChannel() {
        return persistableStore.getSelectedDiscussionChannel();
    }


    public void selectDiscussionChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateDiscussionChannel) {
            // remove expired messages
            purgePrivateDiscussionChannel((PrivateDiscussionChannel) channel);
        }
        getSelectedDiscussionChannel().set(channel);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatUser
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void reportChatUser(PublicUserProfile publicUserProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", publicUserProfile, reason);
    }

    public void ignoreChatUser(PublicUserProfile publicUserProfile) {
        persistableStore.getIgnoredChatUserIds().add(publicUserProfile.getId());
        persist();
    }

    public void undoIgnoreChatUser(PublicUserProfile publicUserProfile) {
        persistableStore.getIgnoredChatUserIds().remove(publicUserProfile.getId());
        persist();
    }

    public ObservableSet<String> getIgnoredChatUserIds() {
        return persistableStore.getIgnoredChatUserIds();
    }

    public boolean isChatUserIgnored(PublicUserProfile publicUserProfile) {
        return getIgnoredChatUserIds().contains(publicUserProfile.getId());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, NotificationSetting notificationSetting) {
        channel.getNotificationSetting().set(notificationSetting);
        persist();
    }

    public Set<String> getCustomTags() {
        return persistableStore.getCustomTags();
    }

    public Optional<PublicUserProfile> findChatUser(String chatUserId) {
        return Optional.ofNullable(chatUserById.get(chatUserId));
    }

    private void putChatUser(PublicUserProfile publicUserProfile) {
        chatUserById.put(publicUserProfile.getId(), publicUserProfile);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<DataService.BroadCastDataResult> publish(UserProfile userProfile,
                                                                       PublicUserProfile publicUserProfile,
                                                                       DistributedData distributedData) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getNodeIdAndKeyPair();
        return chatUserService.maybePublishChatUser(publicUserProfile, nodeIdAndKeyPair)
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

    public boolean isMyMessage(ChatMessage chatMessage) {
        String authorId = chatMessage.getAuthorId();
        return chatUserService.getChatUserIdentities().stream()
                .anyMatch(userprofile -> userprofile.getPublicUserProfile().getId().equals(authorId));
    }

    private void maybeAddDefaultChannels() {
        if (!getPublicDiscussionChannels().isEmpty()) {
            return;
        }

        PublicTradeChannel defaultChannel = new PublicTradeChannel(MarketRepository.getDefault(), true);
        selectTradeChannel(defaultChannel);
        maybeAddPublicTradeChannel(defaultChannel);
        List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
        allMarkets.remove(MarketRepository.getDefault());
        allMarkets.forEach(market ->
                maybeAddPublicTradeChannel(new PublicTradeChannel(market, false)));

        // todo channelAdmin not supported atm
        String channelAdminId = "";
        PublicDiscussionChannel defaultDiscussionChannel = new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BISQ_ID.name(),
                "Discussions Bisq",
                "Channel for discussions about Bisq",
                channelAdminId,
                new HashSet<>()
        );
        selectDiscussionChannel(defaultDiscussionChannel);
        getPublicDiscussionChannels().add(defaultDiscussionChannel);
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BITCOIN_ID.name(),
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                channelAdminId,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.MONERO_ID.name(),
                "Discussions Monero",
                "Channel for discussions about Monero",
                channelAdminId,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.PRICE_ID.name(),
                "Price",
                "Channel for discussions about market price",
                channelAdminId,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.ECONOMY_ID.name(),
                "Economy",
                "Channel for discussions about economy",
                channelAdminId,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.OFF_TOPIC_ID.name(),
                "Off-topic",
                "Channel for anything else",
                channelAdminId,
                new HashSet<>()
        ));

        Set<String> customTags = Set.of("BTC", "Bitcoin", "bank-transfer", "SEPA", "zelle", "revolut", "BUY", "SELL", "WANT", "RECEIVE",
                "Tor", "I2P", "Trezor", "Ledger", "Wasabi", "Samurai", "Monero");
        getCustomTags().addAll(customTags);
        persist();
    }

    private void purgePrivateDiscussionChannel(PrivateDiscussionChannel channel) {
        Set<PrivateDiscussionChatMessage> toRemove = channel.getChatMessages().stream()
                .filter(PrivateDiscussionChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (persistableStore) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }

    private void purgePrivateTradeChannel(PrivateTradeChannel channel) {
        Set<PrivateTradeChatMessage> toRemove = channel.getChatMessages().stream()
                .filter(PrivateTradeChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (persistableStore) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }
}