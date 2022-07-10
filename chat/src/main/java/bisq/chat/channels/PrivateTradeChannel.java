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

package bisq.chat.channels;

import bisq.common.observable.ObservableSet;
import bisq.chat.NotificationSetting;
import bisq.chat.messages.PrivateTradeChatMessage;
import bisq.user.profile.UserProfile;
import bisq.user.identity.UserIdentity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateTradeChannel extends Channel<PrivateTradeChatMessage> implements PrivateChannel {
    private final UserProfile peer;
    private final UserIdentity myProfile;

    // We persist the messages as they are NOT persisted in the P2P data store.
    private final ObservableSet<PrivateTradeChatMessage> chatMessages = new ObservableSet<>();

    public PrivateTradeChannel(UserProfile peer, UserIdentity myProfile) {
        this(PrivateChannel.createChannelId(peer.getId(), myProfile.getId()),
                peer,
                myProfile,
                NotificationSetting.ALL,
                new HashSet<>());
    }

    public PrivateTradeChannel(String id, UserProfile peer, UserIdentity myProfile) {
        this(id, peer, myProfile, NotificationSetting.ALL, new HashSet<>());
    }

    private PrivateTradeChannel(String id,
                                UserProfile peer,
                                UserIdentity myProfile,
                                NotificationSetting notificationSetting,
                                Set<PrivateTradeChatMessage> chatMessages) {
        super(id, notificationSetting);
        this.peer = peer;
        this.myProfile = myProfile;
        this.chatMessages.addAll(chatMessages);
    }

    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateTradeChannel(bisq.chat.protobuf.PrivateTradeChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyUserIdentity(myProfile.toProto())
                        .addAllChatMessages(chatMessages.stream().map(this::getChatMessageProto).collect(Collectors.toList())))
                .build();
    }

    public static PrivateTradeChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                bisq.chat.protobuf.PrivateTradeChannel proto) {
        return new PrivateTradeChannel(
                baseProto.getId(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                NotificationSetting.fromProto(baseProto.getNotificationSetting()),
                proto.getChatMessagesList().stream()
                        .map(PrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    protected bisq.chat.protobuf.ChatMessage getChatMessageProto(PrivateTradeChatMessage chatMessage) {
        return chatMessage.toChatMessageProto();
    }

    @Override
    public void addChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PrivateTradeChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return peer.getUserName();
    }
}