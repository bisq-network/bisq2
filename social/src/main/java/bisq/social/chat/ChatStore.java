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

import bisq.persistence.PersistableStore;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatStore implements PersistableStore<ChatStore> {

    @Getter
    private final Set<PrivateChannel> privateChannels = new CopyOnWriteArraySet<>();
    @Getter
    private final Set<PublicChannel> publicChannels = new CopyOnWriteArraySet<>();
    @Getter
    private final Map<String, String> userNameByDomainId = new HashMap<>();
    @Nullable
    @Setter
    private Channel selectedChannel;

    public ChatStore() {
    }

    private ChatStore(Set<PrivateChannel> privateChannels,
                      Set<PublicChannel> publicChannels,
                      Channel selectedChannel,
                      Map<String, String> userNameByDomainId) {
        setAll(privateChannels,
                publicChannels,
                selectedChannel,
                userNameByDomainId);
    }

    @Override
    public void applyPersisted(ChatStore chatStore) {
        setAll(chatStore.privateChannels,
                chatStore.publicChannels,
                chatStore.selectedChannel,
                chatStore.userNameByDomainId);
    }

    @Override
    public ChatStore getClone() {
        return new ChatStore(privateChannels,
                publicChannels,
                selectedChannel,
                userNameByDomainId);
    }

    public void setAll(Set<PrivateChannel> privateChannels,
                       Set<PublicChannel> publicChannels,
                       Channel selectedChannel,
                       Map<String, String> userNameByDomainId) {
        this.privateChannels.clear();
        this.privateChannels.addAll(privateChannels);
        this.publicChannels.clear();
        this.publicChannels.addAll(publicChannels);
        this.selectedChannel = selectedChannel;
        this.userNameByDomainId.clear();
        this.userNameByDomainId.putAll(userNameByDomainId);
    }

    public Optional<Channel> getSelectedChannel() {
        return Optional.ofNullable(selectedChannel);
    }

    public Optional<PrivateChannel> findPrivateChannel(String id) {
        return privateChannels.stream().filter(e -> e.getId().equals(id)).findAny();
    }

    public Optional<PublicChannel> findPublicChannel(String id) {
        return publicChannels.stream().filter(e -> e.getId().equals(id)).findAny();
    }
}