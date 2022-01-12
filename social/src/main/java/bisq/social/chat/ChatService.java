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
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NodeIdAndPubKey;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Getter
public class ChatService implements PersistenceClient<ChatService.PersistedChatServiceData> {
    public interface Listener {
        void onChatUserAdded(ChatUser chatUser);

        void onChatUserSelected(ChatUser chatUser);
    }

    @Getter
    public static class PersistedChatServiceData implements Serializable {
        private final HashMap<String, ChatUser> map;
        private ChatUser selected;

        public PersistedChatServiceData() {
            this(new HashMap<>(), null);
        }

        public PersistedChatServiceData(HashMap<String, ChatUser> map,
                                        ChatUser selected) {
            this.map = map;
            this.selected = selected;
        }

        public PersistedChatServiceData(PersistedChatServiceData chatServiceData) {
            this(chatServiceData.map, chatServiceData.selected);
        }

        public void setAll(Map<String, ChatUser> map, ChatUser selectedChatUser) {
            this.map.putAll(map);
            this.selected = selectedChatUser;
        }
    }

    private final PersistenceService persistenceService;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final PersistedChatServiceData chatServiceData = new PersistedChatServiceData();
    private final Persistence<PersistedChatServiceData> persistence;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ChatService(PersistenceService persistenceService, IdentityService identityService, NetworkService networkService) {
        this.persistenceService = persistenceService;
        this.identityService = identityService;
        this.networkService = networkService;
        persistence = persistenceService.getOrCreatePersistence(this, "db", "chatUser");
    }

    @Override
    public void applyPersisted(PersistedChatServiceData persisted) {
        synchronized (chatServiceData) {
            chatServiceData.setAll(persisted.getMap(), persisted.getSelected());
        }
    }

    @Override
    public PersistedChatServiceData getClone() {
        synchronized (chatServiceData) {
            return new PersistedChatServiceData(chatServiceData);
        }
    }

    public CompletableFuture<ChatUser> createNewChatUser(String userName) {
        if (chatServiceData.map.containsKey(userName)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Username exists already"));
        }
        NodeIdAndPubKey nodeIdAndPubKey = identityService.getNodeIdAndPubKey(userName);
        return networkService.getInitializedNetworkIdAsync(nodeIdAndPubKey)
                .thenApply(networkId -> {
                    ChatUser chatUser = new ChatUser(StringUtils.createUid(), userName, networkId);
                    addChatUser(chatUser);
                    selectChatUser(chatUser);
                    return chatUser;
                });
    }

    public void selectChatUser(ChatUser chatUser) {
        chatServiceData.selected = chatUser;
        persist();
        listeners.forEach(listener -> listener.onChatUserSelected(chatUser));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void addChatUser(ChatUser chatUser) {
        synchronized (chatServiceData) {
            boolean notContaining = !chatServiceData.map.containsKey(chatUser.userName());
            if (notContaining) {
                chatServiceData.map.put(chatUser.userName(), chatUser);
                listeners.forEach(listener -> listener.onChatUserAdded(chatUser));
                persist();
            }
        }
    }

}