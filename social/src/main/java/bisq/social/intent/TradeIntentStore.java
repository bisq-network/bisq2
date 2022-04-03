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

package bisq.social.intent;

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TradeIntentStore implements PersistableStore<TradeIntentStore> {
    @Getter
    private final ObservableSet<String> tradeTags = new ObservableSet<>();
    @Getter
    private final ObservableSet<String> currencyTags = new ObservableSet<>();
    @Getter
    private final ObservableSet<String> paymentMethodTags = new ObservableSet<>();

    //todo
    public TradeIntentStore() {
        tradeTags.addAll(List.of("want", "buy", "sell"));

        Set<String> fiatCurrencyNames = FiatCurrencyRepository.getAllCurrencies().stream().map(TradeCurrency::getName).collect(Collectors.toSet());
        currencyTags.addAll(fiatCurrencyNames);
        Set<String> fiatCurrencyCodes = FiatCurrencyRepository.getAllCurrencies().stream().map(TradeCurrency::getCode).collect(Collectors.toSet());
        currencyTags.addAll(fiatCurrencyCodes);
        paymentMethodTags.addAll(List.of("sepa", "bank-transfer", "zelle", "revolut"));
    }

    private TradeIntentStore(Set<String> tradeKeyWords) {
        // setAll(tradeKeyWords);
        this();
    }

    @Override
    public bisq.social.protobuf.TradeIntentStore toProto() {
        return bisq.social.protobuf.TradeIntentStore.newBuilder()
                .addAllTradeKeyWords(tradeTags)
                .build();
    }

    public static TradeIntentStore fromProto(bisq.social.protobuf.TradeIntentStore proto) {
        return new TradeIntentStore(new HashSet<>(proto.getTradeKeyWordsList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.TradeIntentStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(TradeIntentStore chatStore) {
        setAll(chatStore.tradeTags);
    }

    @Override
    public TradeIntentStore getClone() {
        return new TradeIntentStore(tradeTags);
    }

    public void setAll(Set<String> tradeKeyWords) {
        this.tradeTags.clear();
        this.tradeTags.addAll(tradeKeyWords);
    }
}