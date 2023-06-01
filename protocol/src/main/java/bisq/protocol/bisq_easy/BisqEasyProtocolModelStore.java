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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public class BisqEasyProtocolModelStore implements PersistableStore<BisqEasyProtocolModelStore> {

    private final Map<String, BisqEasyProtocolModel> protocolModelById = new ConcurrentHashMap<>();

    public BisqEasyProtocolModelStore() {
    }

    private BisqEasyProtocolModelStore(Map<String, BisqEasyProtocolModel> protocolModelById) {
        setAll(protocolModelById);
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyProtocolModelStore toProto() {
        bisq.protocol.protobuf.BisqEasyProtocolModelStore.Builder builder = bisq.protocol.protobuf.BisqEasyProtocolModelStore.newBuilder()
                .putAllProtocolModelById(protocolModelById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> entry.getValue().toProto())));
        return builder.build();
    }

    public static BisqEasyProtocolModelStore fromProto(bisq.protocol.protobuf.BisqEasyProtocolModelStore proto) {
        return new BisqEasyProtocolModelStore(proto.getProtocolModelByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> BisqEasyProtocolModel.fromProto(entry.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.protocol.protobuf.BisqEasyProtocolModelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(BisqEasyProtocolModelStore chatStore) {
        setAll(chatStore.getProtocolModelById());
    }

    @Override
    public BisqEasyProtocolModelStore getClone() {
        return new BisqEasyProtocolModelStore(protocolModelById);
    }

    private void setAll(Map<String, BisqEasyProtocolModel> protocolModelById) {
        protocolModelById.clear();
        this.protocolModelById.putAll(protocolModelById);
    }

    Map<String, BisqEasyProtocolModel> getProtocolModelById() {
        return protocolModelById;
    }
}