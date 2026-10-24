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

    @Override
    public void applyPersisted(SupportNotificationsStore persisted) {
        notifications.setAll(persisted.notifications);
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
