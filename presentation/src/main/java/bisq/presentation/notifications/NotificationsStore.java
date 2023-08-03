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

package bisq.presentation.notifications;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class NotificationsStore implements PersistableStore<NotificationsStore> {
    private final Map<String, DateAndConsumedFlag> notificationIdMap = new ConcurrentHashMap<>();

    public NotificationsStore() {
    }

    private NotificationsStore(Map<String, DateAndConsumedFlag> notificationIdMap) {
        this.notificationIdMap.putAll(notificationIdMap);
    }

    @Override
    public bisq.presentation.protobuf.NotificationsStore toProto() {
        return bisq.presentation.protobuf.NotificationsStore.newBuilder()
                .putAllNotificationIdMap(notificationIdMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toProto())))
                .build();
    }

    public static PersistableStore<?> fromProto(bisq.presentation.protobuf.NotificationsStore proto) {
        return new NotificationsStore(proto.getNotificationIdMapMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> DateAndConsumedFlag.fromProto(entry.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.presentation.protobuf.NotificationsStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public NotificationsStore getClone() {
        return new NotificationsStore(notificationIdMap);
    }

    @Override
    public void applyPersisted(NotificationsStore persisted) {
        notificationIdMap.clear();
        notificationIdMap.putAll(persisted.notificationIdMap);
    }

    Map<String, DateAndConsumedFlag> getNotificationIdMap() {
        return notificationIdMap;
    }
}