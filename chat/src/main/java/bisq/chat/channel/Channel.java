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
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Channel<T extends ChatMessage> implements Proto {
    private final ChannelDomain channelDomain;
    protected final String channelName;
    @EqualsAndHashCode.Include
    private transient final String id;
    protected final Observable<ChannelNotificationType> channelNotificationType = new Observable<>();
    protected final ObservableSet<String> seenChatMessageIds = new ObservableSet<>();
    protected final ObservableSet<ChannelMember> channelMembers = new ObservableSet<>();
    protected final ObservableArray<UserProfile> peers = new ObservableArray<>();

    public Channel(ChannelDomain channelDomain, String channelName, ChannelNotificationType channelNotificationType) {
        this.channelDomain = channelDomain;
        this.channelName = channelName;
        this.id = channelDomain.name().toLowerCase() + "." + channelName;
        this.channelNotificationType.set(channelNotificationType);
    }

    public bisq.chat.protobuf.Channel.Builder getChannelBuilder() {
        return bisq.chat.protobuf.Channel.newBuilder()
                .setChannelName(channelName)
                .setChannelDomain(channelDomain.toProto())
                .setChannelNotificationType(channelNotificationType.get().toProto())
                .addAllSeenChatMessageIds(seenChatMessageIds);
    }

    abstract public bisq.chat.protobuf.Channel toProto();

    public static Channel<? extends ChatMessage> fromProto(bisq.chat.protobuf.Channel proto) {
        switch (proto.getMessageCase()) {
            case PRIVATETWOPARTYCHANNEL: {
                return PrivateTwoPartyChannel.fromProto(proto, proto.getPrivateTwoPartyChannel());
            }

            case PRIVATETRADECHANNEL: {
                return PrivateTradeChannel.fromProto(proto, proto.getPrivateTradeChannel());
            }
            case PUBLICTRADECHANNEL: {
                return PublicTradeChannel.fromProto(proto, proto.getPublicTradeChannel());
            }


            case PUBLICCHATCHANNEL: {
                return PublicChatChannel.fromProto(proto, proto.getPublicChatChannel());
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

    public void addChannelMember(ChannelMember channelMember) {
        channelMembers.add(channelMember);
        if (channelMember.getType() != ChannelMember.Type.SELF) {
            peers.add(channelMember.getUserProfile());
        }
    }

    public Set<String> getChannelMembersUserProfileIds() {
        return channelMembers.stream()
                .map(ChannelMember::getUserProfile)
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
    }

    public Set<String> getPeersProfileIds() {
        return peers.stream()
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
    }

    abstract public ObservableSet<T> getChatMessages();

    abstract public void addChatMessage(T chatMessage);

    abstract public void removeChatMessage(T chatMessage);

    abstract public void removeChatMessages(Collection<T> messages);

    abstract public String getDisplayString();
}