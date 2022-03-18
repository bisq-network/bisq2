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
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Manages chatChannels and persistence of the chatModel.
 * ChatUser and ChatIdentity management is not implemented yet. Not 100% clear yet if ChatIdentity management should
 * be rather part of the identity module.
 */
@Slf4j
@Getter
public class ChatService implements PersistenceClient<ChatStore>, MessageListener, DataService.Listener {
    public interface Listener {
        void onChannelAdded(Channel channel);

        void onChannelSelected(Channel channel);

        void onChatMessageAdded(Channel channel, ChatMessage newChatMessage);
    }

    private final ChatStore persistableStore = new ChatStore();
    private final Persistence<ChatStore> persistence;
    private final UserProfileService userProfileService;
    private final PersistenceService persistenceService;
    private final IdentityService identityService;
    private final NetworkService networkService;

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

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

        addDummyChannels();
    }

    public void addDummyChannels() {
        PublicChannel element = new PublicChannel("BTC-EUR Market",
                "BTC-EUR Market",
                "Channel for trading Bitcoin with EUR",
                null);
        persistableStore.getPublicChannels().add(element);
        persistableStore.getPublicChannels().add(new PublicChannel("BTC-USD Market",
                "BTC-USD Market",
                "Channel for trading Bitcoin with USD",
                null));
        persistableStore.getPublicChannels().add(new PublicChannel("Other Markets",
                "Other Markets",
                "Channel for trading any market",
                null));
        persistableStore.getPublicChannels().add(new PublicChannel("Off-topic",
                "Off-topic",
                "Channel for off topic",
                null));
        persistableStore.getSelectedChannel().set(element); //todo
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message) {
        if (message instanceof ChatMessage chatMessage) {
            log.error("chatMessage " + chatMessage);
            String domainId = chatMessage.getChannelId();
            String userName = findUserName(domainId).orElse("Maker@" + StringUtils.truncate(domainId, 8));
            ChatIdentity chatIdentity = getOrCreateChatIdentity(userName, domainId);
            ChatUser chatUser = new ChatUser(chatMessage.getSenderNetworkId());
            PrivateChannel privateChannel = getOrCreatePrivateChannel(chatMessage.getChannelId(),
                    chatUser,
                    chatIdentity);
            addChatMessage(chatMessage, privateChannel);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            if (authenticatedPayload.getData() instanceof ChatMessage chatMessage) {
                if (chatMessage.getChannelType() == ChannelType.PUBLIC) {
                    persistableStore.getPublicChannels().stream()
                            .filter(publicChannel -> publicChannel.getId().equals(chatMessage.getChannelId()))
                            .findAny()
                            .ifPresent(publicChannel -> {
                                synchronized (persistableStore) {
                                    publicChannel.addChatMessage(chatMessage);
                                }
                                persist();
                                listeners.forEach(listener -> listener.onChatMessageAdded(publicChannel, chatMessage));
                            });
                }
            }
        }
    }

    @Override
    public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {

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
                            PublicChannel publicChannel = new PublicChannel(StringUtils.createUid(),
                                    channelName,
                                    description,
                                    userProfile);
                            persistableStore.getPublicChannels().add(publicChannel);
                            persist();
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
            listeners.forEach(listener -> listener.onChannelAdded(privateChannel));
            return privateChannel;
        } else {
            return previousChannel.get();
        }
    }

    public void selectChannel(Channel channel) {
        persistableStore.getSelectedChannel().set(channel);
        persist();
        listeners.forEach(listener -> listener.onChannelSelected(channel));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatMessage
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addChatMessage(ChatMessage chatMessage, Channel privateChannel) {
        synchronized (persistableStore) {
            privateChannel.addChatMessage(chatMessage);
        }
        persist();
        listeners.forEach(listener -> listener.onChatMessageAdded(privateChannel, chatMessage));
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
    // Misc
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}