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

import bisq.common.monetary.Market;
import bisq.common.monetary.MarketRepository;
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
import bisq.social.user.Entitlement;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
    private final UserProfileService userProfileService;
    private final PersistenceService persistenceService;
    private final IdentityService identityService;
    private final NetworkService networkService;

    public ChatService(PersistenceService persistenceService,
                       IdentityService identityService,
                       NetworkService networkService,
                       UserProfileService userProfileService) {
        this.persistenceService = persistenceService;
        this.identityService = identityService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.userProfileService = userProfileService;
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

    public PublicTradeChannel addPublicTradeChannel(Market market) {
        PublicTradeChannel tradeChannel = new PublicTradeChannel(market);
        persistableStore.getPublicTradeChannels().add(tradeChannel);
        persist();
        
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAllAuthenticatedPayload()
                .forEach(this::onAuthenticatedDataAdded));
        
        return tradeChannel;
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatTextMessage(String text,
                                                                                          Optional<Quotation> quotedMessage,
                                                                                          PublicTradeChannel publicTradeChannel,
                                                                                          UserProfile userProfile) {
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                userProfile.getChatUser(),
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
        return networkService.publishAuthenticatedData(chatMessage, userProfile.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatOffer(TradeChatOffer tradeChatOffer,
                                                                                    PublicTradeChannel publicTradeChannel,
                                                                                    UserProfile userProfile) {
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                userProfile.getChatUser(),
                Optional.of(tradeChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        return networkService.publishAuthenticatedData(chatMessage, userProfile.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedTradeChatMessage(PublicTradeChatMessage originalChatMessage,
                                                                                            String editedText,
                                                                                            UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        checkArgument(originalChatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        // We do not support editing the MarketChatOffer directly but remove it and replace it with 
                        // the edited text.
                        PublicTradeChatMessage newChatMessage = new PublicTradeChatMessage(originalChatMessage.getChannelId(),
                                userProfile.getChatUser(),
                                Optional.empty(),
                                Optional.of(editedText),
                                originalChatMessage.getQuotedMessage(),
                                originalChatMessage.getDate(),
                                true);
                        networkService.publishAuthenticatedData(newChatMessage, nodeIdAndKeyPair);
                    } else {
                        log.error("Error at deleting old message", throwable);
                    }
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                           UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
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
        UserProfile userProfile = privateTradeChannel.getMyProfile();
        ChatUser peer = privateTradeChannel.getPeer();
        PrivateTradeChatMessage chatMessage = new PrivateTradeChatMessage(channelId,
                userProfile.getChatUser(),
                peer.getProfileId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateTradeChatMessage(chatMessage, privateTradeChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public Optional<PrivateTradeChannel> getOrCreatePrivateTradeChannel(PrivateTradeChatMessage privateTradeChatMessage) {
        return findPrivateTradeChannel(privateTradeChatMessage.getChannelId())
                .or(() -> createPrivateTradeChannel(privateTradeChatMessage.getAuthor(), privateTradeChatMessage.getReceiversProfileId()));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(ChatUser peer) {
        return Optional.ofNullable(userProfileService.getSelectedUserProfile().get())
                .flatMap(e -> createPrivateTradeChannel(peer, e.getProfileId()));
    }

    public Optional<PrivateTradeChannel> createPrivateTradeChannel(ChatUser peer, String receiversProfileId) {
        return userProfileService.findUserProfile(receiversProfileId)
                .map(myUserProfile -> {
                            PrivateTradeChannel privateTradeChannel = new PrivateTradeChannel(peer, myUserProfile);
                            persistableStore.getPrivateTradeChannels().add(privateTradeChannel);
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

    public CompletableFuture<Optional<PublicDiscussionChannel>> addPublicDiscussionChannel(UserProfile userProfile,
                                                                                           String channelName,
                                                                                           String description) {
        return userProfile.getEntitlements().stream()
                .filter(entitlement -> entitlement.entitlementType() == Entitlement.Type.CHANNEL_ADMIN)
                .filter(entitlement -> entitlement.proof() instanceof Entitlement.BondedRoleProof)
                .map(entitlement -> (Entitlement.BondedRoleProof) entitlement.proof())
                .map(bondedRoleProof -> userProfileService.verifyBondedRole(bondedRoleProof.txId(),
                        bondedRoleProof.signature(),
                        userProfile.getChatUser().getId()))
                .map(future -> future.thenApply(optionalProof -> optionalProof.map(e -> {
                            ChatUser chatUser = new ChatUser(userProfile.getNickName(),
                                    userProfile.getIdentity().networkId(),
                                    userProfile.getEntitlements());
                            PublicDiscussionChannel publicDiscussionChannel = new PublicDiscussionChannel(StringUtils.createUid(),
                                    channelName,
                                    description,
                                    chatUser,
                                    new HashSet<>());
                            persistableStore.getPublicDiscussionChannels().add(publicDiscussionChannel);
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
                                                                                           UserProfile userProfile) {
        PublicDiscussionChatMessage chatMessage = new PublicDiscussionChatMessage(publicDiscussionChannel.getId(),
                userProfile.getChatUser(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        return networkService.publishAuthenticatedData(chatMessage, userProfile.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedDiscussionChatMessage(PublicDiscussionChatMessage originalChatMessage,
                                                                                                 String editedText,
                                                                                                 UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        checkArgument(originalChatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        PublicDiscussionChatMessage newChatMessage = new PublicDiscussionChatMessage(originalChatMessage.getChannelId(),
                                userProfile.getChatUser(),
                                editedText,
                                originalChatMessage.getQuotedMessage(),
                                originalChatMessage.getDate(),
                                true);
                        networkService.publishAuthenticatedData(newChatMessage, nodeIdAndKeyPair);
                    } else {
                        log.error("Error at deleting old message", throwable);
                    }
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicDiscussionChatMessage(PublicDiscussionChatMessage chatMessage,
                                                                                                UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
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
        UserProfile userProfile = privateDiscussionChannel.getMyProfile();
        ChatUser peer = privateDiscussionChannel.getPeer();
        PrivateDiscussionChatMessage chatMessage = new PrivateDiscussionChatMessage(channelId,
                userProfile.getChatUser(),
                peer.getProfileId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateDiscussionChatMessage(chatMessage, privateDiscussionChannel);
        NetworkId receiverNetworkId = peer.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        return networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }

    public Optional<PrivateDiscussionChannel> getOrCreatePrivateDiscussionChannel(PrivateDiscussionChatMessage message) {
        return findPrivateDiscussionChannel(message.getChannelId())
                .or(() -> createPrivateDiscussionChannel(message.getAuthor(), message.getReceiversProfileId()));
    }

    public Optional<PrivateDiscussionChannel> createPrivateDiscussionChannel(ChatUser peer) {
        return Optional.ofNullable(userProfileService.getSelectedUserProfile().get())
                .flatMap(e -> createPrivateDiscussionChannel(peer, e.getProfileId()));
    }

    public Optional<PrivateDiscussionChannel> createPrivateDiscussionChannel(ChatUser peer, String receiversProfileId) {
        return userProfileService.findUserProfile(receiversProfileId)
                .map(myUserProfile -> {
                            PrivateDiscussionChannel privateDiscussionChannel = new PrivateDiscussionChannel(peer, myUserProfile);
                            persistableStore.getPrivateDiscussionChannels().add(privateDiscussionChannel);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isMyMessage(ChatMessage chatMessage) {
        String chatId = chatMessage.getAuthor().getId();
        return userProfileService.getUserProfiles().stream()
                .anyMatch(userprofile -> userprofile.getChatUser().getId().equals(chatId));
    }

    private void maybeAddDefaultChannels() {
        if (!persistableStore.getPublicDiscussionChannels().isEmpty()) {
            return;
        }

        PublicTradeChannel defaultChannel = new PublicTradeChannel(MarketRepository.getDefault());
        selectTradeChannel(defaultChannel);
        persistableStore.getPublicTradeChannels().add(defaultChannel);
        persistableStore.getPublicTradeChannels().add(new PublicTradeChannel(MarketRepository.getBsqMarket()));
        persistableStore.getPublicTradeChannels().add(new PublicTradeChannel(MarketRepository.getXmrMarket()));

        // Dummy admin
        Identity channelAdminIdentity = identityService.getOrCreateIdentity(IdentityService.DEFAULT).join();
        ChatUser channelAdmin = new ChatUser("Admin", channelAdminIdentity.networkId());

        PublicDiscussionChannel defaultDiscussionChannel = new PublicDiscussionChannel("Discussions Bisq",
                "Discussions Bisq",
                "Channel for discussions about Bisq",
                channelAdmin,
                new HashSet<>()
        );
        selectDiscussionChannel(defaultDiscussionChannel);
        persistableStore.getPublicDiscussionChannels().add(defaultDiscussionChannel);
        persistableStore.getPublicDiscussionChannels().add(new PublicDiscussionChannel("Discussions Bitcoin",
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                channelAdmin,
                new HashSet<>()
        ));
        persistableStore.getPublicDiscussionChannels().add(new PublicDiscussionChannel("Discussions Monero",
                "Discussions Monero",
                "Channel for discussions about Monero",
                channelAdmin,
                new HashSet<>()
        ));
        persistableStore.getPublicDiscussionChannels().add(new PublicDiscussionChannel("Price",
                "Price",
                "Channel for discussions about market price",
                channelAdmin,
                new HashSet<>()
        ));
        persistableStore.getPublicDiscussionChannels().add(new PublicDiscussionChannel("Economy",
                "Economy",
                "Channel for discussions about economy",
                channelAdmin,
                new HashSet<>()
        ));
        persistableStore.getPublicDiscussionChannels().add(new PublicDiscussionChannel("Off-topic",
                "Off-topic",
                "Channel for anything else",
                channelAdmin,
                new HashSet<>()
        ));

        Set<String> customTags = Set.of("BTC", "Bitcoin", "bank-transfer", "SEPA", "zelle", "revolut", "BUY", "SELL", "WANT", "RECEIVE",
                "Tor", "I2P", "Trezor", "Ledger", "Wasabi", "Samurai", "Monero");
        persistableStore.getCustomTags().addAll(customTags);
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