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
import lombok.EqualsAndHashCode;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Channel<T extends ChatMessage> implements Proto {
    @EqualsAndHashCode.Include
    protected final String id;
    protected final Observable<NotificationSetting> notificationSetting = new Observable<>();
    protected final ObservableSet<T> chatMessages = new ObservableSet<>();

    public Channel(String id, NotificationSetting notificationSetting, Set<T> chatMessages) {
        this.id = id;
        this.notificationSetting.set(notificationSetting);
        this.chatMessages.addAll(chatMessages);
    }

    public bisq.social.protobuf.Channel.Builder getChannelBuilder() {
        return bisq.social.protobuf.Channel.newBuilder()
                .setId(id)
                .setNotificationSetting(notificationSetting.get().toProto())
                .addAllChatMessages(chatMessages.stream().map(this::getChatMessageProto).collect(Collectors.toList()));
    }

    // As protobuf classes do not support inheritance we need to delegate it to our subclasses to provide the
    // concrete implementation for the ChatMessage.
    protected abstract bisq.social.protobuf.ChatMessage getChatMessageProto(T e);

    abstract public bisq.social.protobuf.Channel toProto();

    public static Channel<? extends ChatMessage> fromProto(bisq.social.protobuf.Channel proto) {
        switch (proto.getMessageCase()) {
            case PRIVATECHANNEL -> {
                return PrivateChannel.fromProto(proto, proto.getPrivateChannel());
            }
            case PUBLICCHANNEL -> {
                return PublicChannel.fromProto(proto, proto.getPublicChannel());
            }
            case MESSAGE_NOT_SET -> {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public void addChatMessage(T chatMessage) {
        chatMessages.add(chatMessage);
    }

    public void removeChatMessage(T chatMessage) {
        chatMessages.remove(chatMessage);
    }

    public abstract String getChannelName();
}