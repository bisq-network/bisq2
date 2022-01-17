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
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Manages chatChannels and persistence of the chatModel.
 * ChatUser and ChatIdentity management is not implemented yet. Not 100% clear yet if ChatIdentity management should
 * be rather part of the identity module.
 */
@Slf4j
@Getter
public class ChatService implements PersistenceClient<ChatModel>, MessageListener {
    public interface Listener {
        void onChannelAdded(Channel channel);

        void onChannelSelected(Channel channel);

        void onChatMessageAdded(Channel channel, ChatMessage newChatMessage);
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
        persistence = persistenceService.getOrCreatePersistence(this, "db", chatModel);

        networkService.addMessageListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PersistenceClient
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void applyPersisted(ChatModel persisted) {
        synchronized (chatModel) {
            chatModel.applyPersisted(persisted);
        }
    }

    @Override
    public ChatModel getClone() {
        synchronized (chatModel) {
            return chatModel.getClone();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message) {
        if (message instanceof ChatMessage chatMessage) {
            String domainId = chatMessage.getChannelId();
            String userName = findUserName(domainId).orElse("Maker@" + StringUtils.truncate(domainId, 8));
            ChatIdentity chatIdentity = getOrCreateChatIdentity(userName, domainId);
            ChatPeer chatPeer = new ChatPeer(chatMessage.getSenderUserName(), chatMessage.getSenderNetworkId());
            PrivateChannel privateChannel = getOrCreatePrivateChannel(chatMessage.getChannelId(),
                    chatPeer,
                    chatIdentity);
            addChatMessage(chatMessage, privateChannel);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Channels
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public PrivateChannel getOrCreatePrivateChannel(String id, ChatPeer chatPeer, ChatIdentity chatIdentity) {
        PrivateChannel privateChannel = new PrivateChannel(id, chatPeer, chatIdentity);
        Optional<PrivateChannel> previousChannel;
        synchronized (chatModel) {
            previousChannel = chatModel.findPrivateChannel(id);
            if (previousChannel.isEmpty()) {
                chatModel.getPrivateChannels().add(privateChannel);
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
        chatModel.setSelectedChannel(channel);
        persist();
        listeners.forEach(listener -> listener.onChannelSelected(channel));
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