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

package bisq.settings;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class SettingsStore implements PersistableStore<SettingsStore> {
    final Cookie cookie;
    final Map<String, Boolean> dontShowAgainMap = new ConcurrentHashMap<>();

    final Observable<Boolean> useAnimations = new Observable<>(true);
    final ObservableSet<Market> markets = new ObservableSet<>();
    final Observable<Market> selectedMarket = new Observable<>();
    final Observable<Long> requiredTotalReputationScore = new Observable<>(1000L);
    final Observable<Boolean> offersOnly = new Observable<>(true);
    final Observable<Boolean> tradeRulesConfirmed = new Observable<>(true);
    final Observable<ChatNotificationType> chatNotificationType = new Observable<>(ChatNotificationType.MENTION);
    final Set<String> consumedAlertIds;
    boolean isTacAccepted;

    public SettingsStore() {
        this(new Cookie(),
                new HashMap<>(),
                true,
                new HashSet<>(MarketRepository.getAllFiatMarkets()),
                MarketRepository.getDefault(),
                1000,
                true,
                false,
                ChatNotificationType.MENTION,
                false,
                new HashSet<>());
    }

    public SettingsStore(Cookie cookie,
                         Map<String, Boolean> dontShowAgainMap,
                         boolean useAnimations,
                         Set<Market> markets,
                         Market selectedMarket,
                         long requiredTotalReputationScore,
                         boolean offersOnly,
                         boolean tradeRulesConfirmed,
                         ChatNotificationType chatNotificationType,
                         boolean isTacAccepted,
                         Set<String> consumedAlertIds) {
        this.cookie = cookie;
        this.consumedAlertIds = consumedAlertIds;
        this.useAnimations.set(useAnimations);
        this.dontShowAgainMap.putAll(dontShowAgainMap);
        this.markets.setAll(markets);
        this.selectedMarket.set(selectedMarket);
        this.requiredTotalReputationScore.set(requiredTotalReputationScore);
        this.offersOnly.set(offersOnly);
        this.tradeRulesConfirmed.set(tradeRulesConfirmed);
        this.chatNotificationType.set(chatNotificationType);
        this.isTacAccepted = isTacAccepted;
    }

    @Override
    public bisq.settings.protobuf.SettingsStore toProto() {
        return bisq.settings.protobuf.SettingsStore.newBuilder()
                .setCookie(cookie.toProto())
                .putAllDontShowAgainMap(dontShowAgainMap)
                .setUseAnimations(useAnimations.get())
                .addAllMarkets(markets.stream().map(Market::toProto).collect(Collectors.toList()))
                .setSelectedMarket(selectedMarket.get().toProto())
                .setRequiredTotalReputationScore(requiredTotalReputationScore.get())
                .setOffersOnly(offersOnly.get())
                .setTradeRulesConfirmed(tradeRulesConfirmed.get())
                .setChatNotificationType(chatNotificationType.get().toProto())
                .setIsTacAccepted(isTacAccepted)
                .addAllConsumedAlertIds(consumedAlertIds)
                .build();
    }

    public static SettingsStore fromProto(bisq.settings.protobuf.SettingsStore proto) {
        return new SettingsStore(Cookie.fromProto(proto.getCookie()),
                proto.getDontShowAgainMapMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                proto.getUseAnimations(),
                new ObservableSet<>(proto.getMarketsList().stream().map(Market::fromProto).collect(Collectors.toList())),
                Market.fromProto(proto.getSelectedMarket()),
                proto.getRequiredTotalReputationScore(),
                proto.getOffersOnly(),
                proto.getTradeRulesConfirmed(),
                ChatNotificationType.fromProto(proto.getChatNotificationType()),
                proto.getIsTacAccepted(),
                new HashSet<>(proto.getConsumedAlertIdsList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.settings.protobuf.SettingsStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public SettingsStore getClone() {
        return new SettingsStore(cookie,
                dontShowAgainMap,
                useAnimations.get(),
                markets,
                selectedMarket.get(),
                requiredTotalReputationScore.get(),
                offersOnly.get(),
                tradeRulesConfirmed.get(),
                chatNotificationType.get(),
                isTacAccepted,
                consumedAlertIds);
    }

    @Override
    public void applyPersisted(SettingsStore persisted) {
        cookie.putAll(persisted.cookie.getMap());
        dontShowAgainMap.putAll(persisted.dontShowAgainMap);
        useAnimations.set(persisted.useAnimations.get());
        markets.clear();
        markets.addAll(persisted.markets);
        selectedMarket.set(persisted.selectedMarket.get());
        requiredTotalReputationScore.set(persisted.requiredTotalReputationScore.get());
        offersOnly.set(persisted.offersOnly.get());
        tradeRulesConfirmed.set(persisted.tradeRulesConfirmed.get());
        chatNotificationType.set(persisted.chatNotificationType.get());
        isTacAccepted = persisted.isTacAccepted;
        consumedAlertIds.clear();
        consumedAlertIds.addAll(persisted.consumedAlertIds);
    }
}