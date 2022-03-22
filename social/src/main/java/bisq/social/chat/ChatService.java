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

import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
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

        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        addDummyChannels();
        setSelectedChannelIfNotSet();
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message) {
        if (message instanceof PrivateChatMessage privateChatMessage) {
            String domainId = privateChatMessage.getChannelId();
            //todo outdated userName concept
            String userName = findUserName(domainId).orElse("Maker@" + StringUtils.truncate(domainId, 8));
            ChatIdentity chatIdentity = getOrCreateChatIdentity(userName, domainId);
            ChatUser chatUser = privateChatMessage.getChatUser();
            PrivateChannel privateChannel = getOrCreatePrivateChannel(privateChatMessage.getChannelId(),
                    chatUser,
                    chatIdentity);
            addPrivateChatMessage(privateChatMessage, privateChannel);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            if (authenticatedPayload.getData() instanceof PublicChatMessage publicChatMessage) {
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
    }

    @Override
    public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            if (authenticatedPayload.getData() instanceof PublicChatMessage publicChatMessage) {
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
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Channels
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<PublicChannel>> addChannel(UserProfile userProfile, String channelName, String description) {
        return userProfile.entitlements().stream()
                .filter(entitlement -> entitlement.entitlementType() == Entitlement.Type.CHANNEL_ADMIN)
                .filter(entitlement -> entitlement.proof() instanceof Entitlement.BondedRoleProof)
                .map(entitlement -> (Entitlement.BondedRoleProof) entitlement.proof())
                .map(bondedRoleProof -> userProfileService.verifyBondedRole(bondedRoleProof.txId(),
                        bondedRoleProof.signature(),
                        userProfile.identity().id()))
                .map(future -> future.thenApply(optionalProof -> optionalProof.map(e -> {
                            ChatUser chatUser = new ChatUser(userProfile.identity().networkId(), userProfile.entitlements());
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

    public PrivateChannel getOrCreatePrivateChannel(String id, ChatUser chatUser, ChatIdentity chatIdentity) {
        PrivateChannel privateChannel = new PrivateChannel(id, chatUser, chatIdentity);
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

    public void publishPublicChatMessage(String text, Optional<QuotedMessage> quotedMessage, PublicChannel publicChannel, Identity identity) {
        PublicChatMessage chatMessage = new PublicChatMessage(publicChannel.getId(),
                text,
                quotedMessage,
                identity.networkId(),
                new Date().getTime(),
                false);
        networkService.addData(chatMessage,
                identity.getNodeIdAndKeyPair());
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedPublicChatMessage(PublicChatMessage originalChatMessage, String editedText, Identity identity) {
        return networkService.removeData(originalChatMessage, identity.getNodeIdAndKeyPair())
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        ChatMessage newChatMessage = new PublicChatMessage(originalChatMessage.getChannelId(),
                                editedText,
                                originalChatMessage.getQuotedMessage(),
                                originalChatMessage.getSenderNetworkId(),
                                originalChatMessage.getDate(),
                                true);
                        networkService.addData(newChatMessage, identity.getNodeIdAndKeyPair());
                    } else {
                        log.error("Error at deleting old message", throwable);
                    }
                });
    }

    public void deletePublicChatMessage(PublicChatMessage chatMessage, Identity identity) {
        networkService.removeData(chatMessage, identity.getNodeIdAndKeyPair())
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.error("Successfully deleted chatMessage {}", chatMessage);
                    } else {
                        log.error("Delete chatMessage failed. {}", chatMessage);
                        throwable.printStackTrace();
                    }
                });
    }

    public void sendPrivateChatMessage(String text, Optional<QuotedMessage> quotedMessage, PrivateChannel privateChannel, Identity identity) {
        String channelId = privateChannel.getId();
        PrivateChatMessage chatMessage = new PrivateChatMessage(channelId,
                text,
                quotedMessage,
                identity.networkId(),
                new Date().getTime(),
                false);
        addPrivateChatMessage(chatMessage, privateChannel);
        NetworkId receiverNetworkId = privateChannel.getPeer().networkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = identity.getNodeIdAndKeyPair();
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
    // ChatIdentity
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<ChatIdentity> findChatIdentity(String domainId) {
        if (persistableStore.getUserNameByDomainId().containsKey(domainId)) {
            String userName = persistableStore.getUserNameByDomainId().get(domainId);
            Identity identity = identityService.getOrCreateIdentity(domainId).join();
            return Optional.of(new ChatIdentity(userName, identity));
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> findUserName(String domainId) {
        //todo add mapping strategy
        return Optional.ofNullable(persistableStore.getUserNameByDomainId().get(domainId));
    }

    public ChatIdentity getOrCreateChatIdentity(String userName, String domainId) {
        synchronized (persistableStore) {
            if (persistableStore.getUserNameByDomainId().containsKey(domainId)) {
                checkArgument(persistableStore.getUserNameByDomainId().get(domainId).equals(userName));
            } else {
                persistableStore.getUserNameByDomainId().put(domainId, userName);
            }
        }
        persist();
        Identity identity = identityService.getOrCreateIdentity(domainId).join(); //todo
        return new ChatIdentity(userName, identity);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatUser
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void reportChatUser(ChatUser chatUser, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", chatUser, reason);
    }

    public void ignoreChatUser(ChatUser chatUser) {
        persistableStore.getIgnoredChatUserIds().add(chatUser.id());
        persist();
    }

    public void undoIgnoreChatUser(ChatUser chatUser) {
        persistableStore.getIgnoredChatUserIds().remove(chatUser.id());
        persist();
    }

    private void addDummyChannels() {
        UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
        if (userProfile == null) {
            return;
        }
        ChatUser dummyChannelAdmin = new ChatUser(userProfile.identity().networkId());
        Set<ChatUser> dummyChannelModerators = userProfileService.getPersistableStore().getUserProfiles().stream()
                .map(p -> new ChatUser(p.identity().networkId()))
                .collect(Collectors.toSet());

        PublicChannel defaultChannel = new PublicChannel("BTC-EUR Market",
                "BTC-EUR Market",
                "Channel for trading Bitcoin with EUR",
                dummyChannelAdmin,
                dummyChannelModerators);
        persistableStore.getPublicChannels().add(defaultChannel);
        persistableStore.getPublicChannels().add(new PublicChannel("BTC-USD Market",
                "BTC-USD Market",
                "Channel for trading Bitcoin with USD",
                dummyChannelAdmin,
                dummyChannelModerators));
        persistableStore.getPublicChannels().add(new PublicChannel("Other Markets",
                "Other Markets",
                "Channel for trading any market",
                dummyChannelAdmin,
                dummyChannelModerators));
        persistableStore.getPublicChannels().add(new PublicChannel("Off-topic",
                "Off-topic",
                "Channel for off topic",
                dummyChannelAdmin,
                dummyChannelModerators));
        persistableStore.getSelectedChannel().set(defaultChannel);
    }

    private void setSelectedChannelIfNotSet() {
        if (persistableStore.getSelectedChannel().get() == null) {
            persistableStore.getPublicChannels().stream().findAny()
                    .ifPresent(channel -> persistableStore.getSelectedChannel().set(channel));
        }
    }
}