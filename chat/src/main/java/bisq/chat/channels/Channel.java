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

import bisq.chat.ChannelNotificationType;
import bisq.chat.messages.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Channel<T extends ChatMessage> implements Proto {
    @EqualsAndHashCode.Include
    protected final String id;
    protected final Observable<ChannelNotificationType> channelNotificationType = new Observable<>();

    public Channel(String id, ChannelNotificationType channelNotificationType) {
        this.id = id;
        this.channelNotificationType.set(channelNotificationType);
    }

    public bisq.chat.protobuf.Channel.Builder getChannelBuilder() {
        return bisq.chat.protobuf.Channel.newBuilder()
                .setId(id)
                .setChannelNotificationType(channelNotificationType.get().toProto());
    }

    // As protobuf classes do not support inheritance we need to delegate it to our subclasses to provide the
    // concrete implementation for the ChatMessage.
    protected abstract bisq.chat.protobuf.ChatMessage getChatMessageProto(T e);

    abstract public bisq.chat.protobuf.Channel toProto();

    public static Channel<? extends ChatMessage> fromProto(bisq.chat.protobuf.Channel proto) {
        switch (proto.getMessageCase()) {
            case PRIVATETRADECHANNEL: {
                return PrivateTradeChannel.fromProto(proto, proto.getPrivateTradeChannel());
            }
            case PRIVATEDISCUSSIONCHANNEL: {
                return PrivateDiscussionChannel.fromProto(proto, proto.getPrivateDiscussionChannel());
            }
            case PUBLICTRADECHANNEL: {
                return PublicTradeChannel.fromProto(proto, proto.getPublicTradeChannel());
            }
            case PUBLICDISCUSSIONCHANNEL: {
                return PublicDiscussionChannel.fromProto(proto, proto.getPublicDiscussionChannel());
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    abstract public ObservableSet<T> getChatMessages();

    abstract public void addChatMessage(T chatMessage);

    abstract public void removeChatMessage(T chatMessage);

    abstract public void removeChatMessages(Collection<T> removeMessages);

    public String getDisplayString() {
        return id;
    }
}