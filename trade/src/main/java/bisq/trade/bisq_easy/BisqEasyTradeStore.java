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

package bisq.trade.bisq_easy;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
final class BisqEasyTradeStore implements PersistableStore<BisqEasyTradeStore> {
    @Getter
    private final ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();

    // We keep track of all trades by storing the trade IDs to avoid that the same trade can be taken again.
    @Getter
    private final ObservableSet<String> tradeIds = new ObservableSet<>();

    BisqEasyTradeStore() {
    }

    private BisqEasyTradeStore(Set<BisqEasyTrade> trades, Set<String> tradeIds) {
        this.trades.setAll(trades);
        this.tradeIds.setAll(tradeIds);
    }

    @Override
    public bisq.trade.protobuf.BisqEasyTradeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeStore.newBuilder()
                .addAllTrades(trades.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllTradeIds(tradeIds);
    }

    @Override
    public bisq.trade.protobuf.BisqEasyTradeStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BisqEasyTradeStore fromProto(bisq.trade.protobuf.BisqEasyTradeStore proto) {
        var trades = proto.getTradesList().stream()
                .map(BisqEasyTrade::fromProto)
                .collect(Collectors.toSet());
        return new BisqEasyTradeStore(trades, new HashSet<>(proto.getTradeIdsList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.trade.protobuf.BisqEasyTradeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public BisqEasyTradeStore getClone() {
        return new BisqEasyTradeStore(new HashSet<>(trades), new HashSet<>(tradeIds));
    }

    @Override
    public void applyPersisted(BisqEasyTradeStore persisted) {
        trades.setAll(persisted.getTrades());
        tradeIds.setAll(persisted.getTradeIds());
    }

    void addTrade(BisqEasyTrade trade) {
        trades.add(trade);
        tradeIds.add(trade.getId());
    }

    void removeTrade(BisqEasyTrade trade) {
        trades.remove(trade);
    }

    Optional<BisqEasyTrade> findTrade(String tradeId) {
        return trades.stream().filter(trade -> trade.getId().equals(tradeId)).findAny();
    }

    boolean tradeExists(String tradeId) {
        return tradeIds.contains(tradeId);
    }
}