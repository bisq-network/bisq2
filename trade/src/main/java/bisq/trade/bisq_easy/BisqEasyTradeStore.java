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
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.profile.UserProfile;
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
final class BisqEasyTradeStore implements PersistableStore<BisqEasyTradeStore> {
    private final ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();

    // We keep track of all trades by storing the trade IDs to avoid that the same trade can be taken again.
    private final ObservableSet<String> tradeIds = new ObservableSet<>();
    private final ObservableSet<BisqEasyClosedTrade> closedTrades = new ObservableSet<>();

    private BisqEasyTradeStore(Set<BisqEasyTrade> trades, Set<String> tradeIds, Set<BisqEasyClosedTrade> closedTrades) {
        this.trades.setAll(trades);
        this.tradeIds.setAll(tradeIds);
        this.closedTrades.setAll(closedTrades);
    }

    @Override
    public bisq.trade.protobuf.BisqEasyTradeStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeStore.newBuilder()
                .addAllTrades(trades.stream()
                        .map(trade -> {
                            try {
                                return trade.toProto(serializeForHash);
                            } catch (Exception e) {
                                log.error("Could not create proto from BisqEasyTrade {}", trade, e);
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
                                log.error("Could not create proto from BisqEasyTrade {}", closedTrade, e);
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

    public static BisqEasyTradeStore fromProto(bisq.trade.protobuf.BisqEasyTradeStore proto) {
        var trades = proto.getTradesList().stream()
                .map(tradeProto -> {
                    try {
                        return BisqEasyTrade.fromProto(tradeProto);
                    } catch (Exception e) {
                        log.error("Could not create BisqEasyClosedTrade from proto {}", tradeProto, e);
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var closedTrades = proto.getBisqEasyClosedTradesList().stream()
                .map(closedTradeProto -> {
                    try {
                        return BisqEasyClosedTrade.fromProto(closedTradeProto);
                    } catch (Exception e) {
                        log.error("Could not create BisqEasyClosedTrade from proto {}", closedTradeProto, e);
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new BisqEasyTradeStore(trades, new HashSet<>(proto.getTradeIdsList()), closedTrades);
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

    void removeTrade(BisqEasyTrade trade, UserProfile myUserProfile, UserProfile peerUserProfile) {
        trades.remove(trade);
        closedTrades.add(new BisqEasyClosedTrade(trade, myUserProfile, peerUserProfile));
    }

    Optional<BisqEasyTrade> findTrade(String tradeId) {
        return trades.stream().filter(trade -> trade.getId().equals(tradeId)).findAny();
    }

    boolean tradeExists(String tradeId) {
        return tradeIds.contains(tradeId);
    }
}