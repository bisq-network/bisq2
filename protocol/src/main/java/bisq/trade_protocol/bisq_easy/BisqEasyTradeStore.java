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

package bisq.trade_protocol.bisq_easy;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import bisq.trade_protocol.TradeModel;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class BisqEasyTradeStore implements PersistableStore<BisqEasyTradeStore> {
    @Getter
    private final Map<String, BisqEasyTradeModel> tradeModelById = new ConcurrentHashMap<>();

    public BisqEasyTradeStore() {
    }

    private BisqEasyTradeStore(Map<String, BisqEasyTradeModel> tradeModelById) {
        this.tradeModelById.putAll(tradeModelById);
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyTradeStore toProto() {
        return bisq.protocol.protobuf.BisqEasyTradeStore.newBuilder()
                .putAllTradeModelById(tradeModelById.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto())))
                .build();
    }

    public static BisqEasyTradeStore fromProto(bisq.protocol.protobuf.BisqEasyTradeStore proto) {
        var tradeModelById = proto.getTradeModelByIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> TradeModel.protoToBisqEasyTradeModel(e.getValue())));
        return new BisqEasyTradeStore(tradeModelById);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.protocol.protobuf.BisqEasyTradeStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public BisqEasyTradeStore getClone() {
        return new BisqEasyTradeStore(tradeModelById);
    }

    @Override
    public void applyPersisted(BisqEasyTradeStore persisted) {
        tradeModelById.clear();
        tradeModelById.putAll(persisted.getTradeModelById());
    }

    public void add(BisqEasyTradeModel tradeModel) {
        String tradeId = tradeModel.getId();
        if (!tradeModelById.containsKey(tradeId)) {
            tradeModelById.put(tradeId, tradeModel);
        }
    }

    public Optional<BisqEasyTradeModel> findBisqEasyTrade(String tradeId) {
        return Optional.ofNullable(tradeModelById.get(tradeId));
    }
}