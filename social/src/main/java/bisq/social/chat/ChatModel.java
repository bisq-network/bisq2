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

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class ChatModel implements Serializable {
    @Getter
    private final Map<String, PrivateChannel> privateChannelsById = new HashMap<>();
    @Getter
    private final Map<String, ChatPeer> chatPeerByUserName = new HashMap<>();
    @Getter
    private final Map<String, String> userNameByDomainId = new HashMap<>();

    @Nullable
    @Setter
    private ChatPeer selectedChatPeer;

    @Nullable
    @Setter
    private Channel selectedChannel;

    public ChatModel() {
    }

    public void fromPersisted(ChatModel chatModel) {
        setAll(chatModel.chatPeerByUserName,
                chatModel.selectedChatPeer,
                chatModel.privateChannelsById,
                chatModel.selectedChannel,
                chatModel.userNameByDomainId);
    }

    public ChatModel(Map<String, ChatPeer> chatPeerByUserName,
                     ChatPeer selectedChatPeer,
                     Map<String, PrivateChannel> privateChannelsById,
                     Channel selectedChannel,
                     Map<String, String> userNameByDomainId) {
        setAll(chatPeerByUserName, selectedChatPeer, privateChannelsById, selectedChannel, userNameByDomainId);
    }

    public ChatModel(ChatModel chatModel) {
        this(chatModel.chatPeerByUserName,
                chatModel.selectedChatPeer,
                chatModel.privateChannelsById,
                chatModel.selectedChannel,
                chatModel.userNameByDomainId);
    }

    public void setAll(Map<String, ChatPeer> chatUserByUserName,
                       ChatPeer selectedChatPeer,
                       Map<String, PrivateChannel> channelsById,
                       Channel selectedChannel,
                       Map<String, String> userNameByDomainId) {
        this.chatPeerByUserName.putAll(chatUserByUserName);
        this.selectedChatPeer = selectedChatPeer;
        this.privateChannelsById.putAll(channelsById);
        this.selectedChannel = selectedChannel;
        this.userNameByDomainId.putAll(userNameByDomainId);
    }

    public Optional<Channel> getSelectedChannel() {
        return Optional.ofNullable(selectedChannel);
    }

    public Optional<ChatPeer> getSelectedChatPeer() {
        return Optional.ofNullable(selectedChatPeer);
    }
}