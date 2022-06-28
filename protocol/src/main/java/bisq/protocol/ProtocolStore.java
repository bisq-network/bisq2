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

package bisq.protocol;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class ProtocolStore implements PersistableStore<ProtocolStore> {
    @Getter
    private final Map<String, ProtocolModel> protocolModelByOfferId = new ConcurrentHashMap<>();

    public ProtocolStore() {
    }

    private ProtocolStore(Map<String, ProtocolModel> protocolModelByOfferId) {
        this.protocolModelByOfferId.putAll(protocolModelByOfferId);
    }

    @Override
    public bisq.protocol.protobuf.ProtocolStore toProto() {
        return bisq.protocol.protobuf.ProtocolStore.newBuilder()
                .putAllProtocolModelByOfferId(protocolModelByOfferId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toProto())))
                .build();
    }

    public static ProtocolStore fromProto(bisq.protocol.protobuf.ProtocolStore proto) {
        return new ProtocolStore(proto.getProtocolModelByOfferIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ProtocolModel.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.protocol.protobuf.ProtocolStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ProtocolStore getClone() {
        return new ProtocolStore(protocolModelByOfferId);
    }

    @Override
    public void applyPersisted(ProtocolStore persisted) {
        protocolModelByOfferId.clear();
        protocolModelByOfferId.putAll(persisted.getProtocolModelByOfferId());
    }

    public void add(ProtocolModel protocolModel) {
        String protocolId = protocolModel.getId();
        if (!protocolModelByOfferId.containsKey(protocolId)) {
            protocolModelByOfferId.put(protocolId, protocolModel);
        }
    }
}