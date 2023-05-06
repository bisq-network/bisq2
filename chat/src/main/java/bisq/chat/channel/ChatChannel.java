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

import bisq.chat.bisqeasy.channel.priv.PrivateBisqEasyTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.PublicBisqEasyOfferChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.message.ChatMessage;
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
    private final ChatChannelDomain chatChannelDomain;
    protected final String channelName;
    @EqualsAndHashCode.Include
    private transient final String id;
    protected final Observable<ChatChannelNotificationType> chatChannelNotificationType = new Observable<>();
    protected final ObservableSet<String> seenChatMessageIds = new ObservableSet<>();

    public ChatChannel(ChatChannelDomain chatChannelDomain, String channelName, ChatChannelNotificationType chatChannelNotificationType) {
        this.chatChannelDomain = chatChannelDomain;
        this.channelName = channelName;
        this.id = chatChannelDomain.name().toLowerCase() + "." + channelName;
        this.chatChannelNotificationType.set(chatChannelNotificationType);
    }

    public bisq.chat.protobuf.ChatChannel.Builder getChannelBuilder() {
        return bisq.chat.protobuf.ChatChannel.newBuilder()
                .setChannelName(channelName)
                .setChatChannelDomain(chatChannelDomain.toProto())
                .setChatChannelNotificationType(chatChannelNotificationType.get().toProto())
                .addAllSeenChatMessageIds(seenChatMessageIds);
    }

    abstract public bisq.chat.protobuf.ChatChannel toProto();

    public static ChatChannel<? extends ChatMessage> fromProto(bisq.chat.protobuf.ChatChannel proto) {
        switch (proto.getMessageCase()) {
            case TWOPARTYPRIVATECHATCHANNEL: {
                return TwoPartyPrivateChatChannel.fromProto(proto, proto.getTwoPartyPrivateChatChannel());
            }

            case PRIVATEBISQEASYTRADECHATCHANNEL: {
                return PrivateBisqEasyTradeChatChannel.fromProto(proto, proto.getPrivateBisqEasyTradeChatChannel());
            }
            case PUBLICBISQEASYOFFERCHATCHANNEL: {
                return PublicBisqEasyOfferChatChannel.fromProto(proto, proto.getPublicBisqEasyOfferChatChannel());
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