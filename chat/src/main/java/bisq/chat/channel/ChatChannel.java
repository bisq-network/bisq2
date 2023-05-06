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
import bisq.chat.trade.channel.PrivateTradeChatChannel;
import bisq.chat.trade.channel.PublicTradeChannel;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class ChatChannel<M extends ChatMessage> implements Proto {
    private final ChannelDomain channelDomain;
    protected final String channelName;
    @EqualsAndHashCode.Include
    private transient final String id;
    protected final Observable<ChannelNotificationType> channelNotificationType = new Observable<>();
    protected final ObservableSet<String> seenChatMessageIds = new ObservableSet<>();

    public ChatChannel(ChannelDomain channelDomain, String channelName, ChannelNotificationType channelNotificationType) {
        this.channelDomain = channelDomain;
        this.channelName = channelName;
        this.id = channelDomain.name().toLowerCase() + "." + channelName;
        this.channelNotificationType.set(channelNotificationType);
    }

    public bisq.chat.protobuf.ChatChannel.Builder getChannelBuilder() {
        return bisq.chat.protobuf.ChatChannel.newBuilder()
                .setChannelName(channelName)
                .setChannelDomain(channelDomain.toProto())
                .setChannelNotificationType(channelNotificationType.get().toProto())
                .addAllSeenChatMessageIds(seenChatMessageIds);
    }

    abstract public bisq.chat.protobuf.ChatChannel toProto();

    public static ChatChannel<? extends ChatMessage> fromProto(bisq.chat.protobuf.ChatChannel proto) {
        switch (proto.getMessageCase()) {
            case PRIVATETWOPARTYCHATCHANNEL: {
                return PrivateTwoPartyChatChannel.fromProto(proto, proto.getPrivateTwoPartyChatChannel());
            }

            case PRIVATETRADECHANNEL: {
                return PrivateTradeChatChannel.fromProto(proto, proto.getPrivateTradeChannel());
            }
            case PUBLICTRADECHANNEL: {
                return PublicTradeChannel.fromProto(proto, proto.getPublicTradeChannel());
            }


            case COMMONPUBLICCHATCHANNEL: {
                return CommonPublicChatChannel.fromProto(proto, proto.getCommonPublicChatChannel());
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public void updateSeenChatMessageIds() {
        seenChatMessageIds.clear();
        seenChatMessageIds.addAll(getChatMessages().stream()
                .map(ChatMessage::getMessageId)
                .collect(Collectors.toSet()));
    }


    abstract public Set<String> getMembers();

    abstract public ObservableSet<M> getChatMessages();

    abstract public void addChatMessage(M chatMessage);

    abstract public void removeChatMessage(M chatMessage);

    abstract public void removeChatMessages(Collection<M> messages);

    abstract public String getDisplayString();
}