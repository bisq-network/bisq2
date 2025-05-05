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

package bisq.trade.mu_sig;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
final class MuSigTradeStore implements PersistableStore<MuSigTradeStore> {
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, MuSigTrade> tradeById = new ConcurrentHashMap<>();

    private MuSigTradeStore(Map<String, MuSigTrade> tradeById) {
        this.tradeById.putAll(tradeById);
    }

    @Override
    public MuSigTradeStore getClone() {
        return new MuSigTradeStore(new HashMap<>(tradeById));
    }

    @Override
    public void applyPersisted(MuSigTradeStore persisted) {
        tradeById.clear();
        tradeById.putAll(persisted.getTradeById());
    }

    @Override
    public bisq.trade.protobuf.MuSigTradeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeStore.newBuilder()
                .putAllTradeById(tradeById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))));
    }

    @Override
    public bisq.trade.protobuf.MuSigTradeStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MuSigTradeStore fromProto(bisq.trade.protobuf.MuSigTradeStore proto) {
        var tradeById = proto.getTradeByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> MuSigTrade.fromProto(e.getValue())));
        return new MuSigTradeStore(tradeById);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.trade.protobuf.MuSigTradeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    void addTrade(MuSigTrade trade) {
        String tradeId = trade.getId();
        if (!tradeById.containsKey(tradeId)) {
            tradeById.put(tradeId, trade);
        }
    }

    void removeTrade(String tradeId) {
        tradeById.remove(tradeId);
    }

    boolean tradeExists(String tradeId) {
        return tradeById.containsKey(tradeId);
    }

    Optional<MuSigTrade> findTrade(String tradeId) {
        return Optional.ofNullable(tradeById.get(tradeId));
    }

    public Collection<MuSigTrade> getTrades() {
        return tradeById.values();
    }
}