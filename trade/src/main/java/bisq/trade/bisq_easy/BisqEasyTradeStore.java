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
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ExceptionUtil;
import bisq.persistence.PersistableStore;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public final class BisqEasyTradeStore implements PersistableStore<BisqEasyTradeStore> {
    private final ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();

    // We keep track of all trades by storing the trade IDs to avoid that the same trade can be taken again.
    private final ObservableSet<String> tradeIds = new ObservableSet<>();
    private final ObservableSet<BisqEasyClosedTrade> closedTrades = new ObservableSet<>();

    BisqEasyTradeStore(Set<BisqEasyTrade> trades, Set<String> tradeIds, Set<BisqEasyClosedTrade> closedTrades) {
        this.trades.setAll(trades);
        this.tradeIds.setAll(tradeIds);
        this.closedTrades.setAll(closedTrades);
    }

    // A trade holds private data of the traders, for example the payment account data, and the message of an exception
    // can contain values of the trade. So a failure is logged with the trade ID and the stack trace only
    // (docs/specifications/persistence/store-write-results.md).
    @Override
    public bisq.trade.protobuf.BisqEasyTradeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeStore.newBuilder()
                .addAllTrades(trades.stream()
                        .map(trade -> {
                            try {
                                return trade.toProto(serializeForHash);
                            } catch (Exception e) {
                                log.error("Could not create proto from BisqEasyTrade {}. Cause:\n{}",
                                        trade.getId(), ExceptionUtil.getSafeStackTraceAsString(e));
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .addAllTradeIds(tradeIds)
                .addAllBisqEasyClosedTrades(closedTrades.stream()
                        .map(closedTrade -> {
                            try {
                                return closedTrade.toProto(serializeForHash);
                            } catch (Exception e) {
                                log.error("Could not create proto from BisqEasyClosedTrade {}. Cause:\n{}",
                                        closedTrade.trade().getId(), ExceptionUtil.getSafeStackTraceAsString(e));
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                );
    }

    @Override
    public bisq.trade.protobuf.BisqEasyTradeStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static BisqEasyTradeStore fromProto(bisq.trade.protobuf.BisqEasyTradeStore proto)
            throws UnresolvableProtobufException {
        try {
            var trades = ProtobufUtils.fromProtoStreamSkippingInvalid(proto.getTradesList(), BisqEasyTrade::fromProto)
                    .collect(Collectors.toSet());
            var closedTrades = ProtobufUtils.fromProtoStreamSkippingInvalid(proto.getBisqEasyClosedTradesList(),
                            BisqEasyClosedTrade::fromProto)
                    .collect(Collectors.toSet());
            return new BisqEasyTradeStore(trades, new HashSet<>(proto.getTradeIdsList()), closedTrades);
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
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
        return new BisqEasyTradeStore(Set.copyOf(trades), Set.copyOf(tradeIds), Set.copyOf(closedTrades));
    }

    @Override
    public void applyPersisted(BisqEasyTradeStore persisted) {
        trades.setAll(persisted.getTrades());
        tradeIds.setAll(persisted.getTradeIds());
        closedTrades.setAll(persisted.getClosedTrades());
    }

    // An earlier version could not remove a closed trade from the open trades, so a stored trade can be both open and
    // closed. Only the closing of a trade adds a closed trade, and a trade ID is never used again, so such an open trade
    // is a copy of the closed trade.
    BisqEasyTradeStore withoutOpenCopiesOfClosedTrades() {
        Set<String> closedTradeIds = closedTrades.stream()
                .map(closedTrade -> closedTrade.trade().getId())
                .collect(Collectors.toSet());
        Set<BisqEasyTrade> openTrades = trades.stream()
                .filter(trade -> !closedTradeIds.contains(trade.getId()))
                .collect(Collectors.toSet());
        int numCopies = trades.size() - openTrades.size();
        if (numCopies == 0) {
            return this;
        }
        log.info("We remove {} open trades which are copies of closed trades", numCopies);
        return new BisqEasyTradeStore(openTrades, Set.copyOf(tradeIds), Set.copyOf(closedTrades));
    }

    ObservableSet<BisqEasyTrade> getAllTrades() {
        ObservableSet<BisqEasyTrade> allTrades = new ObservableSet<>();
        allTrades.addAll(trades);
        allTrades.addAll(closedTrades.stream().map(BisqEasyClosedTrade::trade).collect(Collectors.toSet()));
        return allTrades;
    }

    void addTrade(BisqEasyTrade trade) {
        trades.add(trade);
        tradeIds.add(trade.getId());
    }

    // Searches only the open trades in trades. Closing a trade moves it from trades to closedTrades.
    Optional<BisqEasyTrade> findOpenTrade(String tradeId) {
        return trades.stream().filter(trade -> trade.getId().equals(tradeId)).findAny();
    }

    // Also true for a trade which was closed or deleted since: tradeIds keeps the ID of every trade which we added.
    boolean wasTradeIdUsed(String tradeId) {
        return tradeIds.contains(tradeId);
    }
}