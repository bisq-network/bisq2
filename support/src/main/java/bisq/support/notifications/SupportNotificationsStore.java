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

package bisq.support.notifications;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
public final class SupportNotificationsStore implements PersistableStore<SupportNotificationsStore> {
    private final ObservableSet<SupportNotification> notifications = new ObservableSet<>();

    SupportNotificationsStore() {}

    SupportNotificationsStore(Collection<SupportNotification> notifications) {
        this.notifications.addAll(notifications);
    }

    ObservableSet<SupportNotification> getNotifications() {
        return notifications;
    }

    @Override
    public void applyPersisted(SupportNotificationsStore persisted) {
        notifications.setAll(persisted.notifications);
    }

    @Override
    public bisq.support.protobuf.SupportNotificationsStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.SupportNotificationsStore.newBuilder()
                .addAllSupportNotifications(notifications.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.support.protobuf.SupportNotificationsStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static PersistableStore<?> fromProto(bisq.support.protobuf.SupportNotificationsStore proto) {
        return new bisq.support.notifications.SupportNotificationsStore(
                proto.getSupportNotificationsList().stream()
                        .map(SupportNotification::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.support.protobuf.SupportNotificationsStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public SupportNotificationsStore getClone() {
        return new SupportNotificationsStore(Set.copyOf(notifications));
    }
}
