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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class BisqEasyTradeStore implements PersistableStore<BisqEasyTradeStore> {
    @Getter
    private final ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();

    public BisqEasyTradeStore() {
    }

    private BisqEasyTradeStore(Set<BisqEasyTrade> trades) {
        this.trades.setAll(trades);
    }

    @Override
    public bisq.trade.protobuf.BisqEasyTradeStore toProto() {
        return bisq.trade.protobuf.BisqEasyTradeStore.newBuilder()
                .addAllTrades(trades.stream()
                        .map(BisqEasyTrade::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static BisqEasyTradeStore fromProto(bisq.trade.protobuf.BisqEasyTradeStore proto) {
        var trades = proto.getTradesList().stream()
                .map(BisqEasyTrade::fromProto)
                .collect(Collectors.toSet());
        return new BisqEasyTradeStore(trades);
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
        return new BisqEasyTradeStore(trades);
    }

    @Override
    public void applyPersisted(BisqEasyTradeStore persisted) {
        trades.setAll(persisted.getTrades());
    }

    public void add(BisqEasyTrade trade) {
        trades.add(trade);
    }

    public Optional<BisqEasyTrade> findTrade(String tradeId) {
        return trades.stream().filter(e -> e.getOffer().getId().equals(tradeId)).findAny();
    }
}