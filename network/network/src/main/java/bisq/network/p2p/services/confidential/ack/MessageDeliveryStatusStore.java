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

package bisq.network.p2p.services.confidential.ack;

import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class MessageDeliveryStatusStore implements PersistableStore<MessageDeliveryStatusStore> {
    private final ObservableHashMap<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = new ObservableHashMap<>();
    private final Map<String, Long> creationDateByMessageId = new ConcurrentHashMap<>();

    MessageDeliveryStatusStore() {
    }

    MessageDeliveryStatusStore(Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId,
                               Map<String, Long> creationDateByMessageId) {
        this.messageDeliveryStatusByMessageId.clear();
        this.messageDeliveryStatusByMessageId.putAll(messageDeliveryStatusByMessageId);
        this.creationDateByMessageId.clear();
        this.creationDateByMessageId.putAll(creationDateByMessageId);
    }

    @Override
    public bisq.network.protobuf.MessageDeliveryStatusStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.MessageDeliveryStatusStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.MessageDeliveryStatusStore.newBuilder()
                .putAllMessageDeliveryStatusByMessageId(messageDeliveryStatusByMessageId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get().toProtoEnum())))
                .putAllCreationDateByMessageId(creationDateByMessageId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.MessageDeliveryStatusStore proto) {
        return new MessageDeliveryStatusStore(proto.getMessageDeliveryStatusByMessageIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new Observable<>(MessageDeliveryStatus.fromProto(e.getValue())))),
                proto.getCreationDateByMessageIdMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.MessageDeliveryStatusStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(MessageDeliveryStatusStore persisted) {
        messageDeliveryStatusByMessageId.clear();
        messageDeliveryStatusByMessageId.putAll(persisted.getMessageDeliveryStatusByMessageId());
        creationDateByMessageId.clear();
        creationDateByMessageId.putAll(persisted.getCreationDateByMessageId());
    }

    @Override
    public MessageDeliveryStatusStore getClone() {
        return new MessageDeliveryStatusStore(new HashMap<>(messageDeliveryStatusByMessageId), new HashMap<>(creationDateByMessageId));
    }

    ObservableHashMap<String, Observable<MessageDeliveryStatus>> getMessageDeliveryStatusByMessageId() {
        return messageDeliveryStatusByMessageId;
    }

    Map<String, Long> getCreationDateByMessageId() {
        return creationDateByMessageId;
    }
}