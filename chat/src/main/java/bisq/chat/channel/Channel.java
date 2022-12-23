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

import bisq.chat.ChannelKind;
import bisq.chat.discuss.priv.PrivateDiscussionChannel;
import bisq.chat.discuss.pub.PublicDiscussionChannel;
import bisq.chat.events.priv.PrivateEventsChannel;
import bisq.chat.events.pub.PublicEventsChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.MessageType;
import bisq.chat.support.priv.PrivateSupportChannel;
import bisq.chat.support.pub.PublicSupportChannel;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.common.data.Pair;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableArray;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Channel<T extends ChatMessage> implements Proto {
    @EqualsAndHashCode.Include
    protected final String id;
    private final ChannelKind channelKind;
    protected final Observable<ChannelNotificationType> channelNotificationType = new Observable<>();

    public Channel(String id, ChannelNotificationType channelNotificationType, ChannelKind channelKind) {
        this.id = id;
        this.channelKind = channelKind;
        this.channelNotificationType.set(channelNotificationType);
    }

    public bisq.chat.protobuf.Channel.Builder getChannelBuilder() {
        return bisq.chat.protobuf.Channel.newBuilder()
                .setId(id)
                .setChannelNotificationType(channelNotificationType.get().toProto());
    }

    abstract public bisq.chat.protobuf.Channel toProto();

    public static Channel<? extends ChatMessage> fromProto(bisq.chat.protobuf.Channel proto) {
        switch (proto.getMessageCase()) {
            case PRIVATETRADECHANNEL: {
                return PrivateTradeChannel.fromProto(proto, proto.getPrivateTradeChannel());
            }
            case PUBLICTRADECHANNEL: {
                return PublicTradeChannel.fromProto(proto, proto.getPublicTradeChannel());
            }

            case PRIVATEDISCUSSIONCHANNEL: {
                return PrivateDiscussionChannel.fromProto(proto, proto.getPrivateDiscussionChannel());
            }
            case PUBLICDISCUSSIONCHANNEL: {
                return PublicDiscussionChannel.fromProto(proto, proto.getPublicDiscussionChannel());
            }

            case PRIVATEEVENTSCHANNEL: {
                return PrivateEventsChannel.fromProto(proto, proto.getPrivateEventsChannel());
            }
            case PUBLICEVENTSCHANNEL: {
                return PublicEventsChannel.fromProto(proto, proto.getPublicEventsChannel());
            }

            case PRIVATESUPPORTCHANNEL: {
                return PrivateSupportChannel.fromProto(proto, proto.getPrivateSupportChannel());
            }
            case PUBLICSUPPORTCHANNEL: {
                return PublicSupportChannel.fromProto(proto, proto.getPublicSupportChannel());
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    abstract public ObservableArray<T> getChatMessages();

    abstract public void addChatMessage(T chatMessage);

    abstract public void removeChatMessage(T chatMessage);

    abstract public void removeChatMessages(Collection<T> removeMessages);

    public String getDisplayString() {
        return id;
    }

    public Set<String> getMembers() {
        Map<String, List<ChatMessage>> chatMessagesByAuthor = new HashMap<>();
        getChatMessages().forEach(chatMessage -> {
            String authorId = chatMessage.getAuthorId();
            chatMessagesByAuthor.putIfAbsent(authorId, new ArrayList<>());
            chatMessagesByAuthor.get(authorId).add(chatMessage);
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
}