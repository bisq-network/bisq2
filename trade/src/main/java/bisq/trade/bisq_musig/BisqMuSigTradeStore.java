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

package bisq.trade.bisq_musig;

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
public final class BisqMuSigTradeStore implements PersistableStore<BisqMuSigTradeStore> {
    @Getter
    private final Map<String, BisqMuSigTrade> tradeById = new ConcurrentHashMap<>();

    public BisqMuSigTradeStore() {
    }

    private BisqMuSigTradeStore(Map<String, BisqMuSigTrade> tradeById) {
        this.tradeById.putAll(tradeById);
    }


    @Override
    public BisqMuSigTradeStore getClone() {
        return new BisqMuSigTradeStore(new HashMap<>(tradeById));
    }

    @Override
    public void applyPersisted(BisqMuSigTradeStore persisted) {
        tradeById.clear();
        tradeById.putAll(persisted.getTradeById());
    }

    @Override
    public bisq.trade.protobuf.BisqMuSigTradeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqMuSigTradeStore.newBuilder()
                .putAllTradeById(tradeById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))));
    }

    @Override
    public bisq.trade.protobuf.BisqMuSigTradeStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BisqMuSigTradeStore fromProto(bisq.trade.protobuf.BisqMuSigTradeStore proto) {
        var tradeById = proto.getTradeByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> BisqMuSigTrade.fromProto(e.getValue())));
        return new BisqMuSigTradeStore(tradeById);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.trade.protobuf.BisqMuSigTradeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }


    public void add(BisqMuSigTrade trade) {
        String tradeId = trade.getId();
        if (!tradeById.containsKey(tradeId)) {
            tradeById.put(tradeId, trade);
        }
    }

    public Optional<BisqMuSigTrade> findTrade(String tradeId) {
        return Optional.ofNullable(tradeById.get(tradeId));
    }
}