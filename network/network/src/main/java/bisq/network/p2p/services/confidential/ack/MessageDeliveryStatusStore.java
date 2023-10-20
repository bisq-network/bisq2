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

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
final class MessageDeliveryStatusStore implements PersistableStore<MessageDeliveryStatusStore> {
    private final ObservableHashMap<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = new ObservableHashMap<>();

    MessageDeliveryStatusStore() {
    }

    MessageDeliveryStatusStore(Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId) {
        this.messageDeliveryStatusByMessageId.clear();
        this.messageDeliveryStatusByMessageId.putAll(messageDeliveryStatusByMessageId);
    }

    @Override
    public bisq.network.protobuf.MessageDeliveryStatusStore toProto() {
        return bisq.network.protobuf.MessageDeliveryStatusStore.newBuilder()
                .putAllMessageDeliveryStatusByMessageId(messageDeliveryStatusByMessageId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get().toProto())))
                .build();
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.MessageDeliveryStatusStore proto) {
        return new MessageDeliveryStatusStore(proto.getMessageDeliveryStatusByMessageIdMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
                e -> new Observable<>(MessageDeliveryStatus.fromProto(e.getValue())))));
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
    }

    @Override
    public MessageDeliveryStatusStore getClone() {
        return new MessageDeliveryStatusStore(messageDeliveryStatusByMessageId);
    }

    ObservableHashMap<String, Observable<MessageDeliveryStatus>> getMessageDeliveryStatusByMessageId() {
        return messageDeliveryStatusByMessageId;
    }
}