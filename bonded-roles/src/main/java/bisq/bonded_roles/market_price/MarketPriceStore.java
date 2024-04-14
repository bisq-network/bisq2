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

package bisq.bonded_roles.market_price;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public final class MarketPriceStore implements PersistableStore<MarketPriceStore> {
    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private final Observable<Market> selectedMarket = new Observable<>(MarketRepository.getDefault());

    public MarketPriceStore() {
    }

    private MarketPriceStore(Map<Market, MarketPrice> marketPriceByCurrencyMap, Market selectedMarket) {
        this.marketPriceByCurrencyMap.putAll(marketPriceByCurrencyMap);
        this.selectedMarket.set(selectedMarket);
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPriceStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.bonded_roles.protobuf.MarketPriceStore.newBuilder()
                .putAllMarketPriceByCurrencyMap(marketPriceByCurrencyMap.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().getMarketCodes(),
                                e -> e.getValue().toProto(serializeForHash))))
                .setSelectedMarket(selectedMarket.get().toProto(serializeForHash));
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPriceStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MarketPriceStore fromProto(bisq.bonded_roles.protobuf.MarketPriceStore proto) {
        Map<Market, MarketPrice> map = proto.getMarketPriceByCurrencyMapMap().entrySet().stream()
                .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey()).isPresent())
                .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey()).orElseThrow(),
                        e -> MarketPrice.fromProto(e.getValue())));
        return new MarketPriceStore(map, Market.fromProto(proto.getSelectedMarket()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.MarketPriceStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MarketPriceStore getClone() {
        return new MarketPriceStore(new HashMap<>(marketPriceByCurrencyMap), selectedMarket.get());
    }

    @Override
    public void applyPersisted(MarketPriceStore persisted) {
        marketPriceByCurrencyMap.clear();
        Map<Market, MarketPrice> map = persisted.getMarketPriceByCurrencyMap().entrySet().stream()
                .peek(e -> e.getValue().setSource(MarketPrice.Source.PERSISTED))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        marketPriceByCurrencyMap.putAll(map);
        selectedMarket.set(persisted.getSelectedMarket().get());
    }

    ObservableHashMap<Market, MarketPrice> getMarketPriceByCurrencyMap() {
        return marketPriceByCurrencyMap;
    }

    Observable<Market> getSelectedMarket() {
        return selectedMarket;
    }
}