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

package bisq.protocol.poc;

import bisq.common.proto.ProtoResolver;
import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class PocProtocolStore implements PersistableStore<PocProtocolStore> {
    @Getter
    private final Map<String, PocProtocolModel> protocolModelByOfferId = new ConcurrentHashMap<>();

    public PocProtocolStore() {
    }

    private PocProtocolStore(Map<String, PocProtocolModel> protocolModelByOfferId) {
        this.protocolModelByOfferId.putAll(protocolModelByOfferId);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return null;
    }

    @Override
    public Message toProto() {
        return null;
    }

 /*   @Override
    public bisq.protocol.protobuf.ProtocolStore toProto() {
        return bisq.protocol.protobuf.ProtocolStore.newBuilder()
                .putAllProtocolModelByOfferId(protocolModelByOfferId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toProto())))
                .build();
    }

    public static PocProtocolStore fromProto(bisq.protocol.protobuf.ProtocolStore proto) {
        return new PocProtocolStore(proto.getProtocolModelByOfferIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> PocProtocolModel.fromProto(e.getValue()))));
    }*/

  /*  @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.protocol.protobuf.ProtocolStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }*/

    @Override
    public PocProtocolStore getClone() {
        return new PocProtocolStore(protocolModelByOfferId);
    }

    @Override
    public void applyPersisted(PocProtocolStore persisted) {
        protocolModelByOfferId.clear();
        protocolModelByOfferId.putAll(persisted.getProtocolModelByOfferId());
    }

    public void add(PocProtocolModel protocolModel) {
        String protocolId = protocolModel.getId();
        if (!protocolModelByOfferId.containsKey(protocolId)) {
            protocolModelByOfferId.put(protocolId, protocolModel);
        }
    }

}