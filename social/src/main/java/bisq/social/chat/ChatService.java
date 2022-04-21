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

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Market;
import bisq.common.monetary.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.social.offer.MarketChatOffer;
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
import java.util.stream.Stream;

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
        setSelectedChannelIfNotSet();
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateChatMessage privateChatMessage) {
            if (!isMyMessage(privateChatMessage)) {
                ChatUser peer = privateChatMessage.getAuthor();
                PrivateChannel privateChannel = getOrCreatePrivateChannel(privateChatMessage.getChannelId(), peer);
                addPrivateChatMessage(privateChatMessage, privateChannel);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof MarketChatMessage marketChatMessage) {
            persistableStore.getMarketChannels().stream()
                    .filter(channel -> channel.getId().equals(marketChatMessage.getChannelId()))
                    .findAny()
                    .ifPresent(channel -> channel.addChatMessage(marketChatMessage));
        } else if (distributedData instanceof PublicChatMessage publicChatMessage) {
            persistableStore.getPublicChannels().stream()
                    .filter(channel -> channel.getId().equals(publicChatMessage.getChannelId()))
                    .findAny()
                    .ifPresent(channel -> channel.addChatMessage(publicChatMessage));
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof MarketChatMessage marketChatMessage) {
            persistableStore.getMarketChannels().stream()
                    .filter(channel -> channel.getId().equals(marketChatMessage.getChannelId()))
                    .findAny()
                    .ifPresent(channel -> channel.removeChatMessage(marketChatMessage));
        } else if (distributedData instanceof PublicChatMessage publicChatMessage) {
            persistableStore.getPublicChannels().stream()
                    .filter(channel -> channel.getId().equals(publicChatMessage.getChannelId()))
                    .findAny()
                    .ifPresent(channel -> channel.removeChatMessage(publicChatMessage));
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Channels
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<PublicChannel>> addChannel(UserProfile userProfile, String channelName, String description) {
        return userProfile.getEntitlements().stream()
                .filter(entitlement -> entitlement.entitlementType() == Entitlement.Type.CHANNEL_ADMIN)
                .filter(entitlement -> entitlement.proof() instanceof Entitlement.BondedRoleProof)
                .map(entitlement -> (Entitlement.BondedRoleProof) entitlement.proof())
                .map(bondedRoleProof -> userProfileService.verifyBondedRole(bondedRoleProof.txId(),
                        bondedRoleProof.signature(),
                        userProfile.getChatUser().getId()))
                .map(future -> future.thenApply(optionalProof -> optionalProof.map(e -> {
                            ChatUser chatUser = new ChatUser(userProfile.getNickName(), userProfile.getIdentity().networkId(), userProfile.getEntitlements());
                            PublicChannel publicChannel = new PublicChannel(StringUtils.createUid(),
                                    channelName,
                                    description,
                                    chatUser,
                                    new HashSet<>());
                            persistableStore.getPublicChannels().add(publicChannel);
                            persist();
                            setSelectedChannelIfNotSet();
                            return Optional.of(publicChannel);
                        })
                        .orElse(Optional.empty())))
                .findAny()
                .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    public Optional<MarketChannel> findMarketChannel(Market selectedMarket) {
        return persistableStore.getMarketChannels().stream().filter(e -> e.getMarket().equals(selectedMarket)).findAny();
    }

    public PrivateChannel getOrCreatePrivateChannel(String id, ChatUser peer) {
        PrivateChannel privateChannel = new PrivateChannel(id,
                peer,
                PrivateChannel.findMyProfileFromChannelId(id, peer, userProfileService));

        Optional<PrivateChannel> previousChannel;
        synchronized (persistableStore) {
            previousChannel = persistableStore.findPrivateChannel(id);
            if (previousChannel.isEmpty()) {
                persistableStore.getPrivateChannels().add(privateChannel);
            }
        }
        if (previousChannel.isEmpty()) {
            persist();
            return privateChannel;
        } else {
            return previousChannel.get();
        }
    }

    public void setSelectedChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateChannel privateChannel) {
            // remove expired messages
            purgeExpired(privateChannel);
        }
        persistableStore.getSelectedChannel().set(channel);
        persist();
    }

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, NotificationSetting notificationSetting) {
        channel.getNotificationSetting().set(notificationSetting);
        persist();
    }

    public Optional<PublicChannel> findPublicChannel(String id) {
        return persistableStore.getPublicChannels().stream().filter(e -> e.getId()
                .equals(id))
                .findAny();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatMessage
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void publishPublicChatMessage(String text,
                                         Optional<QuotedMessage> quotedMessage,
                                         PublicChannel publicChannel,
                                         UserProfile userProfile) {
        PublicChatMessage chatMessage = new PublicChatMessage(publicChannel.getId(),
                userProfile.getChatUser(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        networkService.publishAuthenticatedData(chatMessage,
                userProfile.getIdentity().getNodeIdAndKeyPair());
    }

    public void publishMarketChatTextMessage(String text,
                                             Optional<QuotedMessage> quotedMessage,
                                             MarketChannel marketChannel,
                                             UserProfile userProfile) {
        MarketChatMessage chatMessage = new MarketChatMessage(marketChannel.getId(),
                userProfile.getChatUser(),
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
        networkService.publishAuthenticatedData(chatMessage,
                userProfile.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishMarketChatOffer(MarketChatOffer marketChatOffer,
                                                                                     MarketChannel marketChannel,
                                                                                     UserProfile userProfile) {
        MarketChatMessage chatMessage = new MarketChatMessage(marketChannel.getId(),
                userProfile.getChatUser(),
                Optional.of(marketChatOffer),
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false);
        return networkService.publishAuthenticatedData(chatMessage,
                userProfile.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedPublicChatMessage(PublicChatMessage originalChatMessage,
                                                                                             String editedText,
                                                                                             UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        checkArgument(originalChatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        PublicChatMessage newChatMessage = new PublicChatMessage(originalChatMessage.getChannelId(),
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

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedMarketChatMessage(MarketChatMessage originalChatMessage,
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
                        MarketChatMessage newChatMessage = new MarketChatMessage(originalChatMessage.getChannelId(),
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

    public void deletePublicChatMessage(PublicChatMessage chatMessage, UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        checkArgument(chatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        networkService.removeAuthenticatedData(chatMessage, nodeIdAndKeyPair)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Successfully deleted chatMessage {}", chatMessage);
                    } else {
                        log.error("Delete chatMessage failed. {}", chatMessage);
                        throwable.printStackTrace();
                    }
                });
    }

    public void sendPrivateChatMessage(String text, Optional<QuotedMessage> quotedMessage,
                                       PrivateChannel privateChannel) {
        String channelId = privateChannel.getId();
        UserProfile userProfile = privateChannel.getMyProfile();
        PrivateChatMessage chatMessage = new PrivateChatMessage(channelId,
                userProfile.getChatUser(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateChatMessage(chatMessage, privateChannel);
        NetworkId receiverNetworkId = privateChannel.getPeer().getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userProfile.getIdentity().getNodeIdAndKeyPair();
        networkService.sendMessage(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair)
                .whenComplete((resultMap, throwable2) -> {
                    if (throwable2 != null) {
                        // UIThread.run(() -> model.setSendMessageError(channelId, throwable2));
                        return;
                    }
                    resultMap.forEach((transportType, res) -> {
                        ConfidentialMessageService.Result result = resultMap.get(transportType);
                        result.getMailboxFuture().values().forEach(broadcastFuture -> broadcastFuture
                                .whenComplete((broadcastResult, throwable3) -> {
                                    if (throwable3 != null) {
                                        // UIThread.run(() -> model.setSendMessageError(channelId, throwable3));
                                        return;
                                    }
                                    //  UIThread.run(() -> model.setSendMessageResult(channelId, result, broadcastResult));
                                }));
                    });
                });
    }

    public void addPrivateChatMessage(PrivateChatMessage chatMessage, PrivateChannel privateChannel) {
        synchronized (persistableStore) {
            privateChannel.addChatMessage(chatMessage);
        }
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

    public boolean isMyMessage(ChatMessage chatMessage) {
        String chatId = chatMessage.getAuthor().getId();
        return userProfileService.getUserProfiles().stream()
                .anyMatch(userprofile -> userprofile.getChatUser().getId().equals(chatId));
    }

    public void maybeAddDefaultChannels() {
        if (!persistableStore.getPublicChannels().isEmpty()) {
            return;
        }

        MarketChannel defaultChannel = new MarketChannel(MarketRepository.getDefault());
        persistableStore.getMarketChannels().add(defaultChannel);
        persistableStore.getMarketChannels().add(new MarketChannel(MarketRepository.getBsqMarket()));
        persistableStore.getMarketChannels().add(new MarketChannel(MarketRepository.getXmrMarket()));
        setSelectedChannel(defaultChannel);

        persistableStore.getPublicChannels().add(new PublicChannel("Discussions Bisq",
                "Discussions Bisq",
                "Channel for discussions about Bisq",
                null,
                new HashSet<>()
        ));
        persistableStore.getPublicChannels().add(new PublicChannel("Discussions Bitcoin",
                "Discussions Bitcoin",
                "Channel for discussions about Bitcoin",
                null,
                new HashSet<>()
        ));
        persistableStore.getPublicChannels().add(new PublicChannel("Discussions Monero",
                "Discussions Monero",
                "Channel for discussions about Monero",
                null,
                new HashSet<>()
        ));
        persistableStore.getPublicChannels().add(new PublicChannel("Price",
                "Price",
                "Channel for discussions about market price",
                null,
                new HashSet<>()
        ));
        persistableStore.getPublicChannels().add(new PublicChannel("Economy",
                "Economy",
                "Channel for discussions about economy",
                null,
                new HashSet<>()
        ));
        persistableStore.getPublicChannels().add(new PublicChannel("Off-topic",
                "Off-topic",
                "Channel for anything else",
                null,
                new HashSet<>()
        ));

        Set<String> customTags = Set.of("BTC", "Bitcoin", "bank-transfer", "SEPA", "zelle", "revolut", "BUY", "SELL", "WANT", "RECEIVE",
                "Tor", "I2P", "Trezor", "Ledger", "Wasabi", "Samurai", "Monero");
        persistableStore.getCustomTags().addAll(customTags);
        persist();
    }

    private void setSelectedChannelIfNotSet() {
        if (persistableStore.getSelectedChannel().get() == null) {
            persistableStore.getPublicChannels().stream().findAny()
                    .ifPresent(channel -> persistableStore.getSelectedChannel().set(channel));
        }
    }

    private Set<String> getFiatCurrencyTags() {
        Stream<String> names = FiatCurrencyRepository.getAllCurrencies().stream().map(TradeCurrency::getName);
        Stream<String> codes = FiatCurrencyRepository.getAllCurrencies().stream().map(TradeCurrency::getCode);
        return Stream.concat(names, codes).collect(Collectors.toSet());
    }


    public void removeExpiredPrivateMessages() {
        // will need to go through al channels and check on their messages if they have expired.
        persistableStore.getPrivateChannels().forEach(this::purgeExpired);
    }

/*    private List<MarketChannel> getMarketChannels() {
        return MarketRepository.getAllMarkets().stream().map(MarketChannel::new).collect(Collectors.toList());
    }*/

    /**
     * PrivateChannels can expire iff their messages have expired or there is no message left for other reasons
     * This method actually belongs to Privatechanel. However we cannot do any write operations in Privatechannel
     * if we dont pass around the ChatService.
     */
    public boolean purgeExpired(PrivateChannel privateChannel) {
        Set<PrivateChatMessage> mortem = privateChannel.getChatMessages().stream()
                .filter(PrivateChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!mortem.isEmpty()) {
            synchronized (persistableStore) {
                privateChannel.removeChatMessages(mortem);
            }
            persist();
        }
        return privateChannel.getChatMessages().isEmpty();
    }

    public ObservableSet<MarketChannel> getMarketChannels() {
        return persistableStore.getMarketChannels();
    }

    public ObservableSet<PublicChannel> getPublicChannels() {
        return persistableStore.getPublicChannels();
    }

    public ObservableSet<PrivateChannel> getPrivateChannels() {
        return persistableStore.getPrivateChannels();
    }

    public Observable<Channel<? extends ChatMessage>> getSelectedChannel() {
        return persistableStore.getSelectedChannel();
    }

    public Set<String> getCustomTags() {
        return persistableStore.getCustomTags();
    }

    public ObservableSet<String> getIgnoredChatUserIds() {
        return persistableStore.getIgnoredChatUserIds();

    }
}