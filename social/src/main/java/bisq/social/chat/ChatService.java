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
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.social.intent.TradeIntent;
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

        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        maybeAddDummyChannels();
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
        if (authenticatedData.getDistributedData() instanceof PublicChatMessage publicChatMessage) {
            if (publicChatMessage.getChannelType() == ChannelType.PUBLIC) {
                persistableStore.getPublicChannels().stream()
                        .filter(publicChannel -> publicChannel.getId().equals(publicChatMessage.getChannelId()))
                        .findAny()
                        .ifPresent(publicChannel -> {
                            synchronized (persistableStore) {
                                publicChannel.addChatMessage(publicChatMessage);
                            }
                            persist();
                        });
            }
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof PublicChatMessage publicChatMessage) {
            if (publicChatMessage.getChannelType() == ChannelType.PUBLIC) {
                persistableStore.getPublicChannels().stream()
                        .filter(publicChannel -> publicChannel.getId().equals(publicChatMessage.getChannelId()))
                        .findAny()
                        .ifPresent(publicChannel -> {
                            synchronized (persistableStore) {
                                publicChannel.removeChatMessage(publicChatMessage);
                            }
                            persist();
                        });
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Channels
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<PublicChannel>> addChannel(UserProfile userProfile, String channelName, String description) {
        Set<String> currencyTags = getFiatCurrencyTags();
        Set<String> tradeTags = getTradeTags();
        Set<String> paymentMethodTags = getPaymentMethodTags();
        Set<String> customTags = getCustomTags();
        return userProfile.entitlements().stream()
                .filter(entitlement -> entitlement.entitlementType() == Entitlement.Type.CHANNEL_ADMIN)
                .filter(entitlement -> entitlement.proof() instanceof Entitlement.BondedRoleProof)
                .map(entitlement -> (Entitlement.BondedRoleProof) entitlement.proof())
                .map(bondedRoleProof -> userProfileService.verifyBondedRole(bondedRoleProof.txId(),
                        bondedRoleProof.signature(),
                        userProfile.chatUser().getId()))
                .map(future -> future.thenApply(optionalProof -> optionalProof.map(e -> {
                            ChatUser chatUser = new ChatUser(userProfile.identity().networkId(), userProfile.entitlements());
                            PublicChannel publicChannel = new PublicChannel(StringUtils.createUid(),
                                    channelName,
                                    description,
                                    chatUser,
                                    new HashSet<>(),
                                    tradeTags,
                                    currencyTags,
                                    paymentMethodTags,
                                    customTags);
                            persistableStore.getPublicChannels().add(publicChannel);
                            persist();
                            setSelectedChannelIfNotSet();
                            return Optional.of(publicChannel);
                        })
                        .orElse(Optional.empty())))
                .findAny()
                .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    public Optional<PublicChannel> findPublicChannelForMarket(Market selectedMarket) {
        if (selectedMarket == null) {
            return Optional.empty();
        }
        ObservableSet<PublicChannel> publicChannels = getPersistableStore().getPublicChannels();
        return Optional.of(publicChannels.stream()
                .filter(e -> e.getChannelName().toLowerCase().contains(selectedMarket.quoteCurrencyCode().toLowerCase()))
                .findAny()
                .orElse(publicChannels.stream()
                        .filter(e -> e.getChannelName().toLowerCase().contains("other")) //todo 
                        .findAny()
                        .orElseThrow()));
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

    public void selectChannel(Channel<? extends ChatMessage> channel) {
        persistableStore.getSelectedChannel().set(channel);
        persist();
    }

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, NotificationSetting notificationSetting) {
        channel.getNotificationSetting().set(notificationSetting);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatMessage
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void publishPublicChatMessage(String text,
                                         Optional<QuotedMessage> quotedMessage,
                                         PublicChannel publicChannel,
                                         UserProfile userProfile) {
        PublicChatMessage chatMessage = new PublicChatMessage(publicChannel.getId(),
                userProfile.chatUser(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        networkService.publishAuthenticatedData(chatMessage,
                userProfile.identity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedPublicChatMessage(PublicChatMessage originalChatMessage,
                                                                                             String editedText,
                                                                                             UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.identity().getNodeIdAndKeyPair();
        checkArgument(originalChatMessage.getAuthor().getNetworkId().equals(nodeIdAndKeyPair.networkId()),
                "NetworkId must match");
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        PublicChatMessage newChatMessage = new PublicChatMessage(originalChatMessage.getChannelId(),
                                userProfile.chatUser(),
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

    public void deletePublicChatMessage(PublicChatMessage chatMessage, UserProfile userProfile) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userProfile.identity().getNodeIdAndKeyPair();
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

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatMessage(TradeIntent tradeIntent,
                                                                                      PublicChannel publicChannel,
                                                                                      UserProfile userProfile) {
        TradeChatMessage chatMessage = new TradeChatMessage(publicChannel.getId(),
                userProfile.chatUser(),
                tradeIntent,
                new Date().getTime(),
                false);
        return networkService.publishAuthenticatedData(chatMessage,
                userProfile.identity().getNodeIdAndKeyPair());
    }

    public void sendPrivateChatMessage(String text, Optional<QuotedMessage> quotedMessage,
                                       PrivateChannel privateChannel) {
        String channelId = privateChannel.getId();
        UserProfile userProfile = privateChannel.getMyProfile();
        PrivateChatMessage chatMessage = new PrivateChatMessage(channelId,
                userProfile.chatUser(),
                text,
                quotedMessage,
                new Date().getTime(),
                false);
        addPrivateChatMessage(chatMessage, privateChannel);
        NetworkId receiverNetworkId = privateChannel.getPeer().getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = userProfile.identity().getNodeIdAndKeyPair();
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
        return userProfileService.getPersistableStore().getUserProfiles().stream()
                .anyMatch(userprofile -> userprofile.chatUser().getId().equals(chatId));
    }

    public void maybeAddDummyChannels() {
        UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
        if (userProfile == null || !persistableStore.getPublicChannels().isEmpty()) {
            return;
        }

        Set<String> currencyTags = getFiatCurrencyTags();
        Set<String> tradeTags = getTradeTags();
        Set<String> paymentMethodTags = getPaymentMethodTags();
        Set<String> customTags = getCustomTags();
        ChatUser dummyChannelAdmin = new ChatUser(userProfile.identity().networkId());
        Set<ChatUser> dummyChannelModerators = userProfileService.getPersistableStore().getUserProfiles().stream()
                .map(p -> new ChatUser(p.identity().networkId()))
                .collect(Collectors.toSet());

        PublicChannel defaultChannel = new PublicChannel("BTC-EUR Market",
                "BTC-EUR Market",
                "Channel for trading Bitcoin with EUR",
                dummyChannelAdmin,
                dummyChannelModerators,
                tradeTags,
                currencyTags,
                paymentMethodTags,
                customTags);
        persistableStore.getPublicChannels().add(defaultChannel);
        persistableStore.getPublicChannels().add(new PublicChannel("BTC-USD Market",
                "BTC-USD Market",
                "Channel for trading Bitcoin with USD",
                dummyChannelAdmin,
                dummyChannelModerators,
                tradeTags,
                currencyTags,
                paymentMethodTags,
                customTags));
        persistableStore.getPublicChannels().add(new PublicChannel("Other Markets",
                "Other Markets",
                "Channel for trading any market",
                dummyChannelAdmin,
                dummyChannelModerators,
                tradeTags,
                currencyTags,
                paymentMethodTags,
                customTags));
        persistableStore.getPublicChannels().add(new PublicChannel("Off-topic",
                "Off-topic",
                "Channel for off topic",
                dummyChannelAdmin,
                dummyChannelModerators,
                tradeTags,
                currencyTags,
                paymentMethodTags,
                customTags));
        persistableStore.getSelectedChannel().set(defaultChannel);
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

    private Set<String> getCustomTags() {
        return Set.of("Tor", "I2P", "Trezor", "Ledger", "Wasabi", "Samurai", "Monero");
    }

    private Set<String> getPaymentMethodTags() {
        return Set.of("sepa", "bank-transfer", "zelle", "revolut");
    }

    private Set<String> getTradeTags() {
        return Set.of("BUY", "SELL", "WANT", "RECEIVE");
    }
}