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
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class SettingsStore implements PersistableStore<SettingsStore> {
    final Cookie cookie;
    final Map<String, Boolean> dontShowAgainMap = new ConcurrentHashMap<>();
    final Observable<Boolean> useAnimations = new Observable<>();
    final Observable<Market> selectedMarket = new Observable<>();
    final Observable<Long> minRequiredReputationScore = new Observable<>();
    final Observable<Boolean> offersOnly = new Observable<>();
    final Observable<Boolean> tradeRulesConfirmed = new Observable<>();
    final Observable<ChatNotificationType> chatNotificationType = new Observable<>(ChatNotificationType.MENTION);
    final Set<String> consumedAlertIds;
    boolean isTacAccepted;
    final Observable<Boolean> closeMyOfferWhenTaken = new Observable<>();
    final Observable<Boolean> preventStandbyMode = new Observable<>();
    String languageCode;
    final ObservableSet<String> supportedLanguageCodes = new ObservableSet<>();
    final Observable<Double> difficultyAdjustmentFactor = new Observable<>();

    public SettingsStore() {
        this(new Cookie(),
                new HashMap<>(),
                true,
                MarketRepository.getDefault(),
                SettingsService.DEFAULT_MIN_REQUIRED_REPUTATION_SCORE,
                false,
                false,
                ChatNotificationType.MENTION,
                false,
                new HashSet<>(),
                true,
                LanguageRepository.getDefaultLanguage(),
                true,
                Set.of(LanguageRepository.getDefaultLanguage()),
                NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT);
    }

    public SettingsStore(Cookie cookie,
                         Map<String, Boolean> dontShowAgainMap,
                         boolean useAnimations,
                         Market selectedMarket,
                         long requiredTotalReputationScore,
                         boolean offersOnly,
                         boolean tradeRulesConfirmed,
                         ChatNotificationType chatNotificationType,
                         boolean isTacAccepted,
                         Set<String> consumedAlertIds,
                         boolean closeMyOfferWhenTaken,
                         String languageCode,
                         boolean preventStandbyMode,
                         Set<String> supportedLanguageCodes,
                         double difficultyAdjustmentFactor) {
        this.cookie = cookie;
        this.dontShowAgainMap.putAll(dontShowAgainMap);
        this.useAnimations.set(useAnimations);
        this.selectedMarket.set(selectedMarket);
        this.minRequiredReputationScore.set(requiredTotalReputationScore);
        this.offersOnly.set(offersOnly);
        this.tradeRulesConfirmed.set(tradeRulesConfirmed);
        this.chatNotificationType.set(chatNotificationType);
        this.isTacAccepted = isTacAccepted;
        this.consumedAlertIds = consumedAlertIds;
        this.closeMyOfferWhenTaken.set(closeMyOfferWhenTaken);
        this.languageCode = languageCode;
        this.preventStandbyMode.set(preventStandbyMode);
        this.supportedLanguageCodes.setAll(supportedLanguageCodes);
        this.difficultyAdjustmentFactor.set(difficultyAdjustmentFactor);
    }

    @Override
    public bisq.settings.protobuf.SettingsStore toProto() {
        return bisq.settings.protobuf.SettingsStore.newBuilder()
                .setCookie(cookie.toProto())
                .putAllDontShowAgainMap(dontShowAgainMap)
                .setUseAnimations(useAnimations.get())
                .setSelectedMarket(selectedMarket.get().toProto())
                .setMinRequiredReputationScore(minRequiredReputationScore.get())
                .setOffersOnly(offersOnly.get())
                .setTradeRulesConfirmed(tradeRulesConfirmed.get())
                .setChatNotificationType(chatNotificationType.get().toProto())
                .setIsTacAccepted(isTacAccepted)
                .addAllConsumedAlertIds(consumedAlertIds)
                .setCloseMyOfferWhenTaken(closeMyOfferWhenTaken.get())
                .setLanguageCode(languageCode)
                .setPreventStandbyMode(preventStandbyMode.get())
                .addAllSupportedLanguageCodes(new ArrayList<>(supportedLanguageCodes))
                .setDifficultyAdjustmentFactor(difficultyAdjustmentFactor.get())
                .build();
    }

    public static SettingsStore fromProto(bisq.settings.protobuf.SettingsStore proto) {
        return new SettingsStore(Cookie.fromProto(proto.getCookie()),
                proto.getDontShowAgainMapMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                proto.getUseAnimations(),
                Market.fromProto(proto.getSelectedMarket()),
                proto.getMinRequiredReputationScore(),
                proto.getOffersOnly(),
                proto.getTradeRulesConfirmed(),
                ChatNotificationType.fromProto(proto.getChatNotificationType()),
                proto.getIsTacAccepted(),
                new HashSet<>(proto.getConsumedAlertIdsList()),
                proto.getCloseMyOfferWhenTaken(),
                proto.getLanguageCode(),
                proto.getPreventStandbyMode(),
                new HashSet<>(proto.getSupportedLanguageCodesList()),
                proto.getDifficultyAdjustmentFactor());
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
                selectedMarket.get(),
                minRequiredReputationScore.get(),
                offersOnly.get(),
                tradeRulesConfirmed.get(),
                chatNotificationType.get(),
                isTacAccepted,
                consumedAlertIds,
                closeMyOfferWhenTaken.get(),
                languageCode,
                preventStandbyMode.get(),
                new HashSet<>(supportedLanguageCodes),
                difficultyAdjustmentFactor.get());
    }

    @Override
    public void applyPersisted(SettingsStore persisted) {
        try {
            cookie.putAll(persisted.cookie.getMap());
            dontShowAgainMap.putAll(persisted.dontShowAgainMap);
            useAnimations.set(persisted.useAnimations.get());
            selectedMarket.set(persisted.selectedMarket.get());
            minRequiredReputationScore.set(persisted.minRequiredReputationScore.get());
            offersOnly.set(persisted.offersOnly.get());
            tradeRulesConfirmed.set(persisted.tradeRulesConfirmed.get());
            chatNotificationType.set(persisted.chatNotificationType.get());
            isTacAccepted = persisted.isTacAccepted;
            consumedAlertIds.clear();
            consumedAlertIds.addAll(persisted.consumedAlertIds);
            closeMyOfferWhenTaken.set(persisted.closeMyOfferWhenTaken.get());
            languageCode = persisted.languageCode;
            preventStandbyMode.set(persisted.preventStandbyMode.get());
            supportedLanguageCodes.setAll(persisted.supportedLanguageCodes);
            difficultyAdjustmentFactor.set(persisted.difficultyAdjustmentFactor.get());
        } catch (Exception e) {
            log.error("Exception at applyPersisted", e);
        }
    }
}