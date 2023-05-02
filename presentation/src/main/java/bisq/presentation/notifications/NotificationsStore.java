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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class NotificationsStore implements PersistableStore<NotificationsStore> {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(30);

    private final Map<String, Long> dateByMessageId = new ConcurrentHashMap<>();

    public NotificationsStore() {
    }

    private NotificationsStore(Map<String, Long> dateByMessageId) {
        this.dateByMessageId.putAll(dateByMessageId);
    }

    @Override
    public bisq.presentation.protobuf.NotificationsStore toProto() {
        return bisq.presentation.protobuf.NotificationsStore.newBuilder()
                .putAllDateByMessageId(dateByMessageId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))

                .build();
    }

    public static PersistableStore<?> fromProto(bisq.presentation.protobuf.NotificationsStore proto) {
        return new NotificationsStore(proto.getDateByMessageIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
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
        return new NotificationsStore(dateByMessageId);
    }

    @Override
    public void applyPersisted(NotificationsStore persisted) {
        dateByMessageId.clear();
        dateByMessageId.putAll(prune(persisted.dateByMessageId));
    }

    Map<String, Long> getDateByMessageId() {
        return dateByMessageId;
    }

    private Map<String, Long> prune(Map<String, Long> dateByMessageId) {
        long pruneDate = System.currentTimeMillis() - MAX_AGE;
        return dateByMessageId.entrySet().stream()
                .filter(e -> e.getValue() > pruneDate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}