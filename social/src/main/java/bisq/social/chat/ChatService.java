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
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
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
import bisq.social.chat.channels.*;
import bisq.social.chat.messages.*;
import bisq.social.offer.TradeChatOffer;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import bisq.social.user.entitlement.Role;
import bisq.social.user.proof.BondedRoleProof;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

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

    public ChatService(PersistenceService persistenceService,
                       IdentityService identityService,
                       NetworkService networkService,
                       ChatUserService chatUserService) {
        this.persistenceService = persistenceService;
        this.identityService = identityService;
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
        if (networkMessage instanceof PrivateTradeChatMessage message) {
            if (!isMyMessage(message)) {
                getOrCreatePrivateTradeChannel(message)
                        .ifPresent(channel -> addPrivateTradeChatMessage(message, channel));
            }
        } else if (networkMessage instanceof PrivateDiscussionChatMessage message) {
            if (!isMyMessage(message)) {
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
        if (distributedData instanceof PublicTradeChatMessage message) {
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicTradeChatMessage(message, channel));
        } else if (distributedData instanceof PublicDiscussionChatMessage message) {
            findPublicDiscussionChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicDiscussionChatMessage(message, channel));
        }
    }


    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicTradeChatMessage message) {
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicTradeChatMessage(message, channel));
        } else if (distributedData instanceof PublicDiscussionChatMessage message) {
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
                                                                                          ChatUserIdentity chatUserIdentity) {
        ChatUser chatUser = chatUserIdentity.getChatUser();
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                chatUser,
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
        return chatUserService.maybePublishChatUser(chatUser, chatUserIdentity.getIdentity())
                .thenCompose(result -> networkService.publishAuthenticatedData(chatMessage, chatUserIdentity.getIdentity().getNodeIdAndKeyPair()));
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatOffer(TradeChatOffer tradeChatOffer,
                                                                                    PublicTradeChannel publicTradeChannel,
                                                                                    ChatUserIdentity chatUserIdentity) {
        ChatUser chatUser = chatUserIdentity.getChatUser();
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                chatUser,
                Optional.of(tradeChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        return chatUserService.maybePublishChatUser(chatUser, chatUserIdentity.getIdentity())
                .thenCompose(result -> networkService.publishAuthenticatedData(chatMessage, chatUserIdentity.getIdentity().getNodeIdAndKeyPair()));
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedTradeChatMessage(PublicTradeChatMessage originalChatMessage,
                                                                                            String editedText,
                                                                                            ChatUserIdentity chatUserIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = chatUserIdentity.getIdentity().getNodeIdAndKeyPair();
        checkArgument(originalChatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    // We do not support editing the MarketChatOffer directly but remove it and replace it with 
                    // the edited text.
                    ChatUser chatUser = chatUserIdentity.getChatUser();
                    PublicTradeChatMessage newChatMessage = new PublicTradeChatMessage(originalChatMessage.getChannelId(),
                            chatUser,
                            Optional.empty(),
                            Optional.of(editedText),
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return chatUserService.maybePublishChatUser(chatUser, chatUserIdentity.getIdentity())
                            .thenCompose(r -> networkService.publishAuthenticatedData(newChatMessage, nodeIdAndKeyPair));
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                           ChatUserIdentity chatUserIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = chatUserIdentity.getIdentity().getNodeIdAndKeyPair();
        checkArgument(chatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Trade domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateTradeChatMessage(String text,
                                                                                           Optional<Quotation> quotedMessage,
                                                                                           PrivateTradeChannel privateTradeChannel) {
        String channelId = privateTradeChannel.getId();
        ChatUserIdentity chatUserIdentity = privateTradeChannel.getMyProfile();
        ChatUser peer = privateTradeChannel.getPeer();
        PrivateTradeChatMessage chatMessage = new PrivateTradeChatMessage(channelId,
                chatUserIdentity.getChatUser(),
                peer.getNym(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateTradeChatMessage(chatMessage, privateTradeChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = chatUserIdentity.getIdentity().getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public Optional<PrivateTradeChannel> getOrCreatePrivateTradeChannel(PrivateTradeChatMessage privateTradeChatMessage) {
        return findPrivateTradeChannel(privateTradeChatMessage.getChannelId())
                .or(() -> createPrivateTradeChannel(privateTradeChatMessage.getAuthor(), privateTradeChatMessage.getReceiversProfileId()));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(ChatUser peer) {
        return Optional.ofNullable(chatUserService.getSelectedUserProfile().get())
                .flatMap(userProfile -> createPrivateTradeChannel(peer, userProfile.getProfileId()));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(ChatUser peer, String receiversProfileId) {
        return chatUserService.findUserProfile(receiversProfileId)
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
        if (channel instanceof PrivateTradeChannel privateTradeChannel) {
            // remove expired messages
            purgePrivateTradeChannel(privateTradeChannel);
        }
        getSelectedTradeChannel().set(channel);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Public Discussion domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<PublicDiscussionChannel>> addPublicDiscussionChannel(ChatUserIdentity chatUserIdentity,
                                                                                           String channelName,
                                                                                           String description) {
        return chatUserIdentity.getChatUser().getRoles().stream()
                .filter(entitlement -> entitlement.type() == Role.Type.CHANNEL_ADMIN)
                .filter(entitlement -> entitlement.proof() instanceof BondedRoleProof)
                .map(entitlement -> (BondedRoleProof) entitlement.proof())
                .map(bondedRoleProof -> chatUserService.verifyBondedRole(bondedRoleProof.txId(),
                        bondedRoleProof.signature(),
                        chatUserIdentity.getChatUser().getId()))
                .map(future -> future.thenApply(optionalProof -> optionalProof.map(e -> {
                            ChatUser chatUser = new ChatUser(chatUserIdentity.getChatUser().getNickName(),
                                    chatUserIdentity.getIdentity().networkId(),
                                    chatUserIdentity.getChatUser().getReputation(),
                                    chatUserIdentity.getChatUser().getRoles());
                            PublicDiscussionChannel publicDiscussionChannel = new PublicDiscussionChannel(StringUtils.createUid(),
                                    channelName,
                                    description,
                                    chatUser,
                                    new HashSet<>());
                            getPublicDiscussionChannels().add(publicDiscussionChannel);
                            persist();
                            return Optional.of(publicDiscussionChannel);
                        })
                        .orElse(Optional.empty())))
                .findAny()
                .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishDiscussionChatMessage(String text,
                                                                                           Optional<Quotation> quotedMessage,
                                                                                           PublicDiscussionChannel publicDiscussionChannel,
                                                                                           ChatUserIdentity chatUserIdentity) {
        ChatUser chatUser = chatUserIdentity.getChatUser();
        PublicDiscussionChatMessage chatMessage = new PublicDiscussionChatMessage(publicDiscussionChannel.getId(),
                chatUser,
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        return chatUserService.maybePublishChatUser(chatUser, chatUserIdentity.getIdentity())
                .thenCompose(r -> networkService.publishAuthenticatedData(chatMessage, chatUserIdentity.getIdentity().getNodeIdAndKeyPair()));
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedDiscussionChatMessage(PublicDiscussionChatMessage originalChatMessage,
                                                                                                 String editedText,
                                                                                                 ChatUserIdentity chatUserIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = chatUserIdentity.getIdentity().getNodeIdAndKeyPair();
        checkArgument(originalChatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    ChatUser chatUser = chatUserIdentity.getChatUser();
                    PublicDiscussionChatMessage newChatMessage = new PublicDiscussionChatMessage(originalChatMessage.getChannelId(),
                            chatUser,
                            editedText,
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return chatUserService.maybePublishChatUser(chatUser, chatUserIdentity.getIdentity())
                            .thenCompose(r -> networkService.publishAuthenticatedData(newChatMessage, nodeIdAndKeyPair));
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicDiscussionChatMessage(PublicDiscussionChatMessage chatMessage,
                                                                                                ChatUserIdentity chatUserIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = chatUserIdentity.getIdentity().getNodeIdAndKeyPair();
        checkArgument(chatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Discussion domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateDiscussionChatMessage(String text,
                                                                                                Optional<Quotation> quotedMessage,
                                                                                                PrivateDiscussionChannel privateDiscussionChannel) {
        String channelId = privateDiscussionChannel.getId();
        ChatUserIdentity chatUserIdentity = privateDiscussionChannel.getMyProfile();
        ChatUser peer = privateDiscussionChannel.getPeer();
        PrivateDiscussionChatMessage chatMessage = new PrivateDiscussionChatMessage(channelId,
                chatUserIdentity.getChatUser(),
                peer.getNym(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateDiscussionChatMessage(chatMessage, privateDiscussionChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = chatUserIdentity.getIdentity().getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public Optional<PrivateDiscussionChannel> getOrCreatePrivateDiscussionChannel(PrivateDiscussionChatMessage message) {
        return findPrivateDiscussionChannel(message.getChannelId())
                .or(() -> createPrivateDiscussionChannel(message.getAuthor(), message.getReceiversProfileId()));
    }

    public Optional<PrivateDiscussionChannel> createPrivateDiscussionChannel(ChatUser peer) {
        return Optional.ofNullable(chatUserService.getSelectedUserProfile().get())
                .flatMap(e -> createPrivateDiscussionChannel(peer, e.getProfileId()));
    }

    public Optional<PrivateDiscussionChannel> createPrivateDiscussionChannel(ChatUser peer, String receiversProfileId) {
        return chatUserService.findUserProfile(receiversProfileId)
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
        if (channel instanceof PrivateDiscussionChannel privateDiscussionChannel) {
            // remove expired messages
            purgePrivateDiscussionChannel(privateDiscussionChannel);
        }
        getSelectedDiscussionChannel().set(channel);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatUser
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void reportChatUser(ChatUser chatUser, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", chatUser, reason);
    }

    public void ignoreChatUser(ChatUser chatUser) {
        persistableStore.getIgnoredChatUserIds().add(chatUser.getId());
        persist();
    }

    public void undoIgnoreChatUser(ChatUser chatUser) {
        persistableStore.getIgnoredChatUserIds().remove(chatUser.getId());
        persist();
    }

    public ObservableSet<String> getIgnoredChatUserIds() {
        return persistableStore.getIgnoredChatUserIds();
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

    public List<Market> getAllMarketsForTradeChannel() {
        List<Market> markets = new ArrayList<>();
        markets.add(MarketRepository.getDefault());
        markets.addAll(MarketRepository.getMajorMarkets());
        return markets;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isMyMessage(ChatMessage chatMessage) {
        String chatId = chatMessage.getAuthor().getId();
        return chatUserService.getUserProfiles().stream()
                .anyMatch(userprofile -> userprofile.getChatUser().getId().equals(chatId));
    }

    private void maybeAddDefaultChannels() {
        if (!getPublicDiscussionChannels().isEmpty()) {
            return;
        }

        PublicTradeChannel defaultChannel = new PublicTradeChannel(MarketRepository.getDefault(), true);
        selectTradeChannel(defaultChannel);
        getPublicTradeChannels().add(defaultChannel);
        getPublicTradeChannels().add(new PublicTradeChannel(MarketRepository.getBsqMarket(), true));
        getPublicTradeChannels().add(new PublicTradeChannel(MarketRepository.getXmrMarket(), true));
        // for the ANY entry
        getPublicTradeChannels().add(new PublicTradeChannel(Optional.empty(), true));
        List<Market> allMarketsForTradeChannel = getAllMarketsForTradeChannel();
        allMarketsForTradeChannel.remove(MarketRepository.getDefault());
        allMarketsForTradeChannel.remove(MarketRepository.getBsqMarket());
        allMarketsForTradeChannel.remove(MarketRepository.getXmrMarket());
        allMarketsForTradeChannel.forEach(market ->
                getPublicTradeChannels().add(new PublicTradeChannel(market, false)));

        // Dummy admin
        Identity channelAdminIdentity = identityService.getOrCreateIdentity(IdentityService.DEFAULT).join();
        ChatUser channelAdmin = new ChatUser("Admin", channelAdminIdentity.networkId());

        PublicDiscussionChannel defaultDiscussionChannel = new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BISQ_ID.name(),
                "Discussions Bisq",
                "Channel for discussions about Bisq",
                channelAdmin,
                new HashSet<>()
        );
        selectDiscussionChannel(defaultDiscussionChannel);
        getPublicDiscussionChannels().add(defaultDiscussionChannel);
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BITCOIN_ID.name(),
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                channelAdmin,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.MONERO_ID.name(),
                "Discussions Monero",
                "Channel for discussions about Monero",
                channelAdmin,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.PRICE_ID.name(),
                "Price",
                "Channel for discussions about market price",
                channelAdmin,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.ECONOMY_ID.name(),
                "Economy",
                "Channel for discussions about economy",
                channelAdmin,
                new HashSet<>()
        ));
        getPublicDiscussionChannels().add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.OFF_TOPIC_ID.name(),
                "Off-topic",
                "Channel for anything else",
                channelAdmin,
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