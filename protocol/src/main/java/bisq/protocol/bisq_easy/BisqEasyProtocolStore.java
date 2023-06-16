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

package bisq.protocol.bisq_easy;

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
public final class BisqEasyProtocolStore implements PersistableStore<BisqEasyProtocolStore> {
    @Getter
    private final Map<String, BisqEasyProtocolModel> protocolModelById = new ConcurrentHashMap<>();

    public BisqEasyProtocolStore() {
    }

    private BisqEasyProtocolStore(Map<String, BisqEasyProtocolModel> protocolModelById) {
        this.protocolModelById.putAll(protocolModelById);
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyProtocolStore toProto() {
        return bisq.protocol.protobuf.BisqEasyProtocolStore.newBuilder()
                .putAllModelById(protocolModelById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toProto())))
                .build();
    }

    public static BisqEasyProtocolStore fromProto(bisq.protocol.protobuf.BisqEasyProtocolStore proto) {
        return new BisqEasyProtocolStore(proto.getModelByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> BisqEasyProtocolModel.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.protocol.protobuf.BisqEasyProtocolStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public BisqEasyProtocolStore getClone() {
        return new BisqEasyProtocolStore(protocolModelById);
    }

    @Override
    public void applyPersisted(BisqEasyProtocolStore persisted) {
        protocolModelById.clear();
        //  protocolModelById.putAll(persisted.getProtocolModelById());
    }

    public void add(BisqEasyProtocolModel protocolModel) {
       /* String protocolId = protocolModel.getProtocolId();
        if (!protocolModelById.containsKey(protocolId)) {
            protocolModelById.put(protocolId, protocolModel);
        }*/
    }

}