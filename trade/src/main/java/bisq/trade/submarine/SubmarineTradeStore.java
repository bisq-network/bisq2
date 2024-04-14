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

package bisq.trade.submarine;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class SubmarineTradeStore implements PersistableStore<SubmarineTradeStore> {
    @Getter
    private final Map<String, SubmarineTrade> tradeById = new ConcurrentHashMap<>();

    public SubmarineTradeStore() {
    }

    private SubmarineTradeStore(Map<String, SubmarineTrade> tradeById) {
        this.tradeById.putAll(tradeById);
    }


    @Override
    public SubmarineTradeStore getClone() {
        return new SubmarineTradeStore(new HashMap<>(tradeById));
    }

    @Override
    public void applyPersisted(SubmarineTradeStore persisted) {
        tradeById.clear();
        tradeById.putAll(persisted.getTradeById());
    }

    @Override
    public bisq.trade.protobuf.SubmarineTradeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.SubmarineTradeStore.newBuilder()
                .putAllTradeById(tradeById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))));
    }

    @Override
    public bisq.trade.protobuf.SubmarineTradeStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static SubmarineTradeStore fromProto(bisq.trade.protobuf.SubmarineTradeStore proto) {
        var tradeById = proto.getTradeByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> SubmarineTrade.fromProto(e.getValue())));
        return new SubmarineTradeStore(tradeById);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.trade.protobuf.SubmarineTradeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }


    public void add(SubmarineTrade trade) {
        String tradeId = trade.getId();
        if (!tradeById.containsKey(tradeId)) {
            tradeById.put(tradeId, trade);
        }
    }

    public Optional<SubmarineTrade> findTrade(String tradeId) {
        return Optional.ofNullable(tradeById.get(tradeId));
    }
}