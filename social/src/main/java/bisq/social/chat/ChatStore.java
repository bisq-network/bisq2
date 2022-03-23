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

import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.persistence.PersistableStore;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ChatStore implements PersistableStore<ChatStore> {

    @Getter
    private final ObservableSet<PrivateChannel> privateChannels = new ObservableSet<>();
    @Getter
    private final ObservableSet<PublicChannel> publicChannels = new ObservableSet<>();
    @Getter
    private final Map<String, String> userNameByDomainId = new HashMap<>(); //todo remove
    @Getter
    private final Observable<Channel<? extends ChatMessage>> selectedChannel = new Observable<>();
    @Getter
    private final ObservableSet<String> ignoredChatUserIds = new ObservableSet<>();

    public ChatStore() {
    }

    private ChatStore(Set<PrivateChannel> privateChannels,
                      Set<PublicChannel> publicChannels,
                      Channel<? extends ChatMessage> selectedChannel,
                      Map<String, String> userNameByDomainId,
                      Set<String> ignoredChatUserIds) {
        setAll(privateChannels,
                publicChannels,
                selectedChannel,
                userNameByDomainId,
                ignoredChatUserIds);
    }

    @Override
    public void applyPersisted(ChatStore chatStore) {
        setAll(chatStore.privateChannels,
                chatStore.publicChannels,
                chatStore.selectedChannel.get(),
                chatStore.userNameByDomainId,
                chatStore.ignoredChatUserIds);
    }

    @Override
    public ChatStore getClone() {
        return new ChatStore(privateChannels,
                publicChannels,
                selectedChannel.get(),
                userNameByDomainId,
                ignoredChatUserIds);
    }

    public void setAll(Set<PrivateChannel> privateChannels,
                       Set<PublicChannel> publicChannels,
                       Channel<? extends ChatMessage> selectedChannel,
                       Map<String, String> userNameByDomainId,
                       Set<String> ignoredChatUserIds) {
        this.privateChannels.clear();
        this.privateChannels.addAll(privateChannels);
        this.publicChannels.clear();
        this.publicChannels.addAll(publicChannels);
        this.selectedChannel.set(selectedChannel);
        this.userNameByDomainId.clear();
        this.userNameByDomainId.putAll(userNameByDomainId);
        this.ignoredChatUserIds.clear();
        this.ignoredChatUserIds.addAll(ignoredChatUserIds);
    }

    public Optional<PrivateChannel> findPrivateChannel(String id) {
        return privateChannels.stream().filter(e -> e.getId().equals(id)).findAny();
    }

    public Optional<PublicChannel> findPublicChannel(String id) {
        return publicChannels.stream().filter(e -> e.getId().equals(id)).findAny();
    }
}