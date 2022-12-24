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

package bisq.chat.channel;

import bisq.chat.message.ChatMessage;
import bisq.chat.message.MessageType;
import bisq.chat.message.PrivateChatMessage;
import bisq.common.data.Pair;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateChannel extends BasePrivateChannel<PrivateChatMessage> {
    private final UserProfile peer;

    public PrivateChannel(UserProfile peer, UserIdentity myUserIdentity, ChannelDomain channelDomain) {
        this(channelDomain,
                BasePrivateChannel.createChannelName(new Pair<>(peer.getId(), myUserIdentity.getId())),
                peer,
                myUserIdentity,
                new ArrayList<>(),
                ChannelNotificationType.ALL
        );
    }

    private PrivateChannel(ChannelDomain channelDomain,
                           String channelName,
                           UserProfile peer,
                           UserIdentity myProfile,
                           List<PrivateChatMessage> chatMessages,
                           ChannelNotificationType channelNotificationType) {
        super(channelDomain, channelName, myProfile, chatMessages, channelNotificationType);

        this.peer = peer;
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateChannel(bisq.chat.protobuf.PrivateChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyUserIdentity(myUserIdentity.toProto())
                        .addAllChatMessages(chatMessages.stream()
                                .map(PrivateChatMessage::toChatMessageProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static PrivateChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                           bisq.chat.protobuf.PrivateChannel proto) {
        return new PrivateChannel(ChannelDomain.fromProto(baseProto.getChannelDomain()),
                baseProto.getChannelName(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getChatMessagesList().stream()
                        .map(PrivateChatMessage::fromProto)
                        .collect(Collectors.toList()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType())
        );
    }

    @Override
    public void addChatMessage(PrivateChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PrivateChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PrivateChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public Set<String> getMembers() {
        Map<String, List<ChatMessage>> chatMessagesByAuthor = new HashMap<>();
        getChatMessages().forEach(chatMessage -> {
            String authorId = chatMessage.getAuthorId();
            chatMessagesByAuthor.putIfAbsent(authorId, new ArrayList<>());
            chatMessagesByAuthor.get(authorId).add(chatMessage);

            String receiversId = chatMessage.getReceiversId();
            chatMessagesByAuthor.putIfAbsent(receiversId, new ArrayList<>());
            chatMessagesByAuthor.get(receiversId).add(chatMessage);
        });

        return chatMessagesByAuthor.entrySet().stream().map(entry -> {
                    List<ChatMessage> chatMessages = entry.getValue();
                    chatMessages.sort(Comparator.comparing(chatMessage -> new Date(chatMessage.getDate())));
                    ChatMessage lastChatMessage = chatMessages.get(chatMessages.size() - 1);
                    return new Pair<>(entry.getKey(), lastChatMessage);
                })
                .filter(pair -> pair.getSecond().getMessageType() != MessageType.LEAVE)
                .map(Pair::getFirst)
                .collect(Collectors.toSet());
    }

    @Override
    public String getDisplayString() {
        return peer.getUserName() + "-" + myUserIdentity.getUserName();
    }
}