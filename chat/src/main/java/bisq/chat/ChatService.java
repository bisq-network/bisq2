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

package bisq.chat;

import bisq.chat.channels.*;
import bisq.chat.messages.*;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Observable;
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

/**
 * Manages chatChannels and persistence of the chatModel.
 * ChatUser and ChatIdentity management is not implemented yet. Not 100% clear yet if ChatIdentity management should
 * be rather part of the identity module.
 */
@Slf4j
public class ChatService implements PersistenceClient<ChatStore>, DataService.Listener {
    @Getter
    private final ChatStore persistableStore = new ChatStore();
    @Getter
    private final Persistence<ChatStore> persistence;
    private final UserIdentityService userIdentityService;
    private final NetworkService networkService;
    private final ProofOfWorkService proofOfWorkService;
    private final Map<String, UserProfile> chatUserById = new ConcurrentHashMap<>();
    @Getter
    private final PrivateTradeChannelService privateTradeChannelService;
    @Getter
    private final PrivateDiscussionChannelService privateDiscussionChannelService;
    @Getter
    private final PublicDiscussionChannelService publicDiscussionChannelService;

    public ChatService(PersistenceService persistenceService,
                       ProofOfWorkService proofOfWorkService,
                       NetworkService networkService,
                       UserIdentityService userIdentityService) {
        this.proofOfWorkService = proofOfWorkService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.userIdentityService = userIdentityService;

        privateTradeChannelService = new PrivateTradeChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);

        privateDiscussionChannelService = new PrivateDiscussionChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);

        publicDiscussionChannelService = new PublicDiscussionChannelService(persistenceService,
                networkService,
                userIdentityService,
                proofOfWorkService);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        privateTradeChannelService.initialize();
        privateDiscussionChannelService.initialize();
        publicDiscussionChannelService.initialize();
        maybeAddDefaultChannels();
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAllAuthenticatedPayload().forEach(this::onAuthenticatedDataAdded));

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        privateTradeChannelService.shutdown();
        privateDiscussionChannelService.shutdown();
        publicDiscussionChannelService.shutdown();

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
             /*   Set<PublicDiscussionChatMessage> publicDiscussionChatMessages = getPublicDiscussionChannels().stream()
                        .flatMap(channel -> channel.getChatMessages().stream())
                        .filter(message -> message.getAuthorId().equals(userProfile.getId()))
                        .collect(Collectors.toSet());*/
                Set<PublicTradeChatMessage> publicTradeChatMessages = getPublicTradeChannels().stream()
                        .flatMap(channel -> channel.getChatMessages().stream())
                        .filter(message -> message.getAuthorId().equals(userProfile.getId()))
                        .collect(Collectors.toSet());

              /*  if (!publicDiscussionChatMessages.isEmpty()) {
                    log.info("We have {} publicDiscussionChatMessages with that chat users ID which have not been displayed yet. " +
                            "We remove them and add them to trigger a list update.", publicDiscussionChatMessages.size());
                }*/
                if (!publicTradeChatMessages.isEmpty()) {
                    log.info("We have {} publicTradeChatMessages with that chat users ID which have not been displayed yet. " +
                            "We remove them and add them to trigger a list update.", publicTradeChatMessages.size());
                }

                // Remove chat messages containing that chatUser
               /* publicDiscussionChatMessages.forEach(message ->
                        findPublicDiscussionChannel(message.getChannelId())
                                .ifPresent(channel -> removePublicDiscussionChatMessage(message, channel)));*/
                publicTradeChatMessages.forEach(message ->
                        findPublicTradeChannel(message.getChannelId())
                                .ifPresent(channel -> removePublicTradeChatMessage(message, channel)));

                putChatUser(userProfile);

                // Now we add them again
              /*  publicDiscussionChatMessages.forEach(message ->
                        findPublicDiscussionChannel(message.getChannelId())
                                .ifPresent(channel -> addPublicDiscussionChatMessage(message, channel)));*/
                publicTradeChatMessages.forEach(message ->
                        findPublicTradeChannel(message.getChannelId())
                                .ifPresent(channel -> addPublicTradeChatMessage(message, channel)));
            } else if (!optionalChatUser.get().equals(userProfile)) {
                // We have that chat user but data are different (e.g. edited user)
                putChatUser(userProfile);

            }
        } else if (distributedData instanceof PublicTradeChatMessage &&
                isValidProofOfWorkOrChatUserNotFound((PublicTradeChatMessage) distributedData)) {
            PublicTradeChatMessage message = (PublicTradeChatMessage) distributedData;
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicTradeChatMessage(message, channel));
        } else if (distributedData instanceof PublicDiscussionChatMessage &&
                isValidProofOfWorkOrChatUserNotFound((PublicDiscussionChatMessage) distributedData)) {
            PublicDiscussionChatMessage message = (PublicDiscussionChatMessage) distributedData;
          /*  findPublicDiscussionChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicDiscussionChatMessage(message, channel));*/
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicTradeChatMessage) {
            PublicTradeChatMessage message = (PublicTradeChatMessage) distributedData;
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicTradeChatMessage(message, channel));
        } /*else if (distributedData instanceof PublicDiscussionChatMessage) {
            PublicDiscussionChatMessage message = (PublicDiscussionChatMessage) distributedData;
            findPublicDiscussionChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicDiscussionChatMessage(message, channel));
        }*/
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Public Trade domain
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<PublicTradeChannel> showPublicTradeChannel(Market market) {
        return findPublicTradeChannel(PublicTradeChannel.getId(market))
                .map(channel -> {
                    channel.setVisible(true);
                    persist();
                    return channel;
                });
    }

    public void showPublicTradeChannel(PublicTradeChannel channel) {
        channel.setVisible(true);
        persist();
    }

    public void hidePublicTradeChannel(PublicTradeChannel channel) {
        channel.setVisible(false);
        persist();
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatTextMessage(String text,
                                                                                          Optional<Quotation> quotedMessage,
                                                                                          PublicTradeChannel publicTradeChannel,
                                                                                          UserIdentity userIdentity) {
        UserProfile userProfile = userIdentity.getUserProfile();
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
        return publish(userIdentity, userProfile, chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishPublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                            UserIdentity userIdentity) {
        return publish(userIdentity, userIdentity.getUserProfile(), chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedTradeChatMessage(PublicTradeChatMessage originalChatMessage,
                                                                                            String editedText,
                                                                                            UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    // We do not support editing the MarketChatOffer directly but remove it and replace it with 
                    // the edited text.
                    UserProfile userProfile = userIdentity.getUserProfile();
                    PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(originalChatMessage.getChannelId(),
                            userProfile.getId(),
                            Optional.empty(),
                            Optional.of(editedText),
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return publish(userIdentity, userProfile, chatMessage);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                           UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
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
    // Selected Trade channel
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<Channel<? extends ChatMessage>> getSelectedTradeChannel() {
        return persistableStore.getSelectedTradeChannel();
    }

    public void selectTradeChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateTradeChannel) {
            privateTradeChannelService.removeExpiredMessages((PrivateTradeChannel) channel);
        }

        getSelectedTradeChannel().set(channel);
        persist();
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Selected Discussion Channel
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<Channel<? extends ChatMessage>> getSelectedDiscussionChannel() {
        return persistableStore.getSelectedDiscussionChannel();
    }


    public void selectDiscussionChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateDiscussionChannel) {
            privateDiscussionChannelService.removeExpiredMessages((PrivateDiscussionChannel) channel);
        }
        getSelectedDiscussionChannel().set(channel);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatUser
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void reportChatUser(UserProfile userProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", userProfile, reason);
    }

    public void ignoreChatUser(UserProfile userProfile) {
        persistableStore.getIgnoredChatUserIds().add(userProfile.getId());
        persist();
    }

    public void undoIgnoreChatUser(UserProfile userProfile) {
        persistableStore.getIgnoredChatUserIds().remove(userProfile.getId());
        persist();
    }

    public ObservableSet<String> getIgnoredChatUserIds() {
        return persistableStore.getIgnoredChatUserIds();
    }

    public boolean isChatUserIgnored(UserProfile userProfile) {
        return getIgnoredChatUserIds().contains(userProfile.getId());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, ChannelNotificationType channelNotificationType) {
        channel.getChannelNotificationType().set(channelNotificationType);
        persist();
    }

    public Set<String> getCustomTags() {
        return persistableStore.getCustomTags();
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

    public boolean isMyMessage(ChatMessage chatMessage) {
        String authorId = chatMessage.getAuthorId();
        return userIdentityService.getUserIdentities().stream()
                .anyMatch(userprofile -> userprofile.getUserProfile().getId().equals(authorId));
    }

    private void maybeAddDefaultChannels() {
        if (!publicDiscussionChannelService.getChannels().isEmpty()) {
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
        ObservableSet<PublicDiscussionChannel> channels = publicDiscussionChannelService.getChannels();
        channels.add(defaultDiscussionChannel);
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.BITCOIN_ID.name(),
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.MONERO_ID.name(),
                "Discussions Monero",
                "Channel for discussions about Monero",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.PRICE_ID.name(),
                "Price",
                "Channel for discussions about market price",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.ECONOMY_ID.name(),
                "Economy",
                "Channel for discussions about economy",
                channelAdminId,
                new HashSet<>()
        ));
        channels.add(new PublicDiscussionChannel(PublicDiscussionChannel.ChannelId.OFF_TOPIC_ID.name(),
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
}