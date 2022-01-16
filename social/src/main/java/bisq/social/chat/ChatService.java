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
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public class ChatService implements PersistenceClient<ChatModel>, Node.Listener {


    public interface Listener {
        void onChatUserAdded(ChatPeer chatPeer); //todo

        void onChatUserSelected(ChatPeer chatPeer);

        void onChannelAdded(Channel channel);

        void onChannelSelected(Channel channel);

        void onChatMessageAdded(Channel channel, ChatMessage newChatMessage);

        void onChatIdentityChanged(ChatIdentity previousValue, ChatIdentity newValue);
    }

    private final PersistenceService persistenceService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final ChatModel chatModel = new ChatModel();
    private final Persistence<ChatModel> persistence;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ChatService(PersistenceService persistenceService, IdentityService identityService, NetworkService networkService) {
        this.persistenceService = persistenceService;
        this.identityService = identityService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, "db", "chatModel");

        networkService.addListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PersistenceClient
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void applyPersisted(ChatModel persisted) {
        synchronized (chatModel) {
            chatModel.fromPersisted(persisted);
        }
    }

    @Override
    public ChatModel getClone() {
        synchronized (chatModel) {
            return new ChatModel(chatModel);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof ChatMessage chatMessage) {
            String domainId = chatMessage.getChannelId();
            String userName = findUserName(domainId).orElse("Maker@" + StringUtils.truncate(domainId, 8));
            ChatIdentity chatIdentity = getOrCreateChatIdentity(userName, domainId);
            ChatPeer chatPeer = new ChatPeer(chatMessage.getSenderUserName(), chatMessage.getSenderNetworkId());
            PrivateChannel privateChannel = getOrCreatePrivateChannel(chatMessage.getChannelId(),
                    chatPeer,
                    chatIdentity,
                    chatMessage.getContext());
            addChatMessage(chatMessage, privateChannel);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Channels
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public PrivateChannel getOrCreatePrivateChannel(String id, ChatPeer chatPeer, ChatIdentity chatIdentity, PrivateChannel.Context context) {
        PrivateChannel previousChannel;
        PrivateChannel privateChannel = new PrivateChannel(id, chatPeer, chatIdentity, context);
        synchronized (chatModel) {
            previousChannel = chatModel.getPrivateChannelsById().putIfAbsent(id, privateChannel);
        }

        if (previousChannel == null) {
            persist();
            listeners.forEach(listener -> listener.onChannelAdded(privateChannel));
            return privateChannel;
        } else {
            return previousChannel;
        }
    }

    public void selectChannel(Channel channel) {
        chatModel.setSelectedChannel(channel);
        persist();
        listeners.forEach(listener -> listener.onChannelSelected(channel));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatPeer
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<ChatPeer> createNewChatUser(String domainId, String userName) {
        if (chatModel.getChatPeerByUserName().containsKey(userName)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Username exists already"));
        }

        return identityService.getOrCreateIdentity(domainId)
                .thenApply(identity -> {
                    ChatPeer chatPeer = new ChatPeer(userName, identity.networkId());
                    boolean alreadyExist;
                    synchronized (chatModel) {
                        alreadyExist = chatModel.getChatPeerByUserName().containsKey(userName);
                        if (!alreadyExist) {
                            chatModel.getChatPeerByUserName().put(chatPeer.userName(), chatPeer);
                        }
                    }
                    if (!alreadyExist) {
                        persist();
                        listeners.forEach(listener -> listener.onChatUserAdded(chatPeer));
                    }
                    return chatPeer;
                });

      /*  NetworkIdWithKeyPair nodeIdAndPubKey = identityService.getNodeIdAndKeyPair(userName);
        //todo move to identity service? 
        return networkService.getInitializedNetworkIdAsync(nodeIdAndPubKey)
                .thenApply(networkId -> {
                    //todo add check if user got added in the meantime 
                    ChatUser chatUser = new ChatUser(userName, networkId);
                    synchronized (chatModel) {
                        chatModel.getChatPeerByUserName().put(chatUser.userName(), chatUser);
                    }
                    // persist for add can be omitted as we do it in selectChatUser
                    listeners.forEach(listener -> listener.onChatUserAdded(chatUser));
                    selectChatPeer(chatUser);
                    return chatUser;
                });*/
    }

    public void selectChatPeer(ChatPeer chatPeer) {
        chatModel.setSelectedChatPeer(chatPeer);
        persist();
        listeners.forEach(listener -> listener.onChatUserSelected(chatPeer));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatMessage
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addChatMessage(ChatMessage chatMessage, Channel privateChannel) {
        synchronized (chatModel) {
            privateChannel.addChatMessage(chatMessage);
        }
        persist();
        listeners.forEach(listener -> listener.onChatMessageAdded(privateChannel, chatMessage));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatIdentity
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<ChatIdentity> findChatIdentity(String domainId) {
        if (chatModel.getUserNameByDomainId().containsKey(domainId)) {
            String userName = chatModel.getUserNameByDomainId().get(domainId);
            Identity identity = identityService.getOrCreateIdentity(domainId).join();
            return Optional.of(new ChatIdentity(userName, identity));
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> findUserName(String domainId) {
        //todo add mapping strategy
        return Optional.ofNullable(chatModel.getUserNameByDomainId().get(domainId));
    }

    public ChatIdentity getOrCreateChatIdentity(String userName, String domainId) {
        synchronized (chatModel) {
            if (chatModel.getUserNameByDomainId().containsKey(domainId)) {
                checkArgument(chatModel.getUserNameByDomainId().get(domainId).equals(userName));
            } else {
                chatModel.getUserNameByDomainId().put(domainId, userName);
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