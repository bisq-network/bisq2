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

package bisq.chat.notifications;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ChatNotificationsStore implements PersistableStore<ChatNotificationsStore> {
    private final ObservableSet<ChatNotification> chatNotifications = new ObservableSet<>();

    public ChatNotificationsStore() {
    }

    ChatNotificationsStore(Collection<ChatNotification> chatNotifications) {
        this.chatNotifications.setAll(chatNotifications);
    }

    @Override
    public bisq.chat.protobuf.ChatNotificationsStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.ChatNotificationsStore.newBuilder()
                .addAllChatNotifications(chatNotifications.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.chat.protobuf.ChatNotificationsStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static PersistableStore<?> fromProto(bisq.chat.protobuf.ChatNotificationsStore proto) {
        return new ChatNotificationsStore(
                proto.getChatNotificationsList().stream()
                        .map(ChatNotification::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.ChatNotificationsStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ChatNotificationsStore getClone() {
        return new ChatNotificationsStore(new HashSet<>(chatNotifications));
    }

    @Override
    public void applyPersisted(ChatNotificationsStore persisted) {
        chatNotifications.setAll(persisted.chatNotifications);
    }

    ObservableSet<ChatNotification> getNotifications() {
        return chatNotifications;
    }

    Optional<ChatNotification> findNotification(String id) {
        return chatNotifications.stream()
                .filter(chatNotification -> chatNotification.getId().equals(id))
                .findAny();
    }

    Optional<ChatNotification> findNotification(ChatNotification notification) {
        return chatNotifications.stream()
                .filter(e -> e.equals(notification))
                .findAny();
    }

    Stream<ChatNotification> getNotConsumedNotifications() {
        return chatNotifications.stream().filter(ChatNotification::isNotConsumed);
    }
}