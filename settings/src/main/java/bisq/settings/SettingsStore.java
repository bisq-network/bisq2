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

import bisq.common.application.DevMode;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.platform.PlatformUtils;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.StringUtils;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static bisq.settings.SettingsService.DEFAULT_MAX_TRADE_PRICE_DEVIATION;
import static bisq.settings.SettingsService.DEFAULT_MIN_REQUIRED_REPUTATION_SCORE;
import static bisq.settings.SettingsService.DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA;
import static bisq.settings.SettingsService.DEFAULT_TOTAL_MAX_BACKUP_SIZE_IN_MB;
import static bisq.settings.SettingsService.MAX_NUM_DAYS_AFTER_REDACTING_TRADE_DATA;
import static bisq.settings.SettingsService.MAX_TRADE_PRICE_DEVIATION;
import static bisq.settings.SettingsService.MIN_NUM_DAYS_AFTER_REDACTING_TRADE_DATA;
import static bisq.settings.SettingsService.MIN_TRADE_PRICE_DEVIATION;

@Slf4j
public final class SettingsStore implements PersistableStore<SettingsStore> {
    final Cookie cookie;
    final Map<String, Boolean> dontShowAgainMap = new ConcurrentHashMap<>();
    final Observable<Boolean> useAnimations = new Observable<>();
    final Observable<Market> selectedMuSigMarket = new Observable<>();
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2.1.1")
    private final Observable<Long> minRequiredReputationScore = new Observable<>();
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2.1.2")
    final Observable<Boolean> offersOnly = new Observable<>();
    final Observable<Boolean> bisqEasyTradeRulesConfirmed = new Observable<>();
    final Observable<Boolean> muSigTradeRulesConfirmed = new Observable<>();
    final Observable<ChatNotificationType> chatNotificationType = new Observable<>();
    final ObservableSet<String> consumedAlertIds = new ObservableSet<>();
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2.1.12")
    final Observable<Boolean> isTacAccepted = new Observable<>();
    final List<TacAcceptance> tacAcceptances = new CopyOnWriteArrayList<>();
    final Observable<Boolean> closeMyOfferWhenTaken = new Observable<>();
    final Observable<Boolean> preventStandbyMode = new Observable<>();
    final Observable<String> languageTag = new Observable<>();
    final Observable<String> countryCode = new Observable<>();
    final Observable<String> currencyCode = new Observable<>();
    final ObservableSet<String> supportedLanguageTags = new ObservableSet<>();
    final Observable<Double> difficultyAdjustmentFactor = new Observable<>();
    final Observable<Boolean> ignoreDiffAdjustmentFromSecManager = new Observable<>();
    final ObservableSet<Market> favouriteMarkets = new ObservableSet<>();
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "2.1.1")
    final Observable<Boolean> ignoreMinRequiredReputationScoreFromSecManager = new Observable<>();
    final Observable<Double> maxTradePriceDeviation = new Observable<>();
    final Observable<Boolean> showBuyOffers = new Observable<>();
    final Observable<Boolean> showOfferListExpanded = new Observable<>();
    final Observable<Boolean> showMarketSelectionListCollapsed = new Observable<>();
    final Observable<String> backupLocation = new Observable<>();
    final Observable<Boolean> showMyOffersOnly = new Observable<>();
    final Observable<Double> totalMaxBackupSizeInMB = new Observable<>();
    final Observable<ChatMessageType> bisqEasyOfferbookMessageTypeFilter = new Observable<>();
    final Observable<Integer> numDaysAfterRedactingTradeData = new Observable<>();
    final Observable<Boolean> muSigActivated = new Observable<>();
    final Observable<Boolean> autoAddToContactsList = new Observable<>();
    final Observable<Market> muSigLastSelectedFiatMarket = new Observable<>();
    final Observable<Market> muSigLastSelectedOtherMarket = new Observable<>();
    final Observable<Market> selectedWalletMarket = new Observable<>();

    SettingsStore() {
        this(new Cookie(),
                new HashMap<>(),
                true,
                MarketRepository.getDefaultBtcFiatMarket(),
                DEFAULT_MIN_REQUIRED_REPUTATION_SCORE,
                false,
                false,
                false,
                ChatNotificationType.ALL,
                false,
                new ArrayList<>(),
                new HashSet<>(),
                true,
                LanguageRepository.getDefaultLanguageTag(),
                CountryRepository.getDefaultCountry().getCode(),
                FiatCurrencyRepository.getDefaultCurrency().getCode(),
                true,
                Set.of(LanguageRepository.getDefaultLanguageTag()),
                NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT,
                false,
                Set.of(MarketRepository.getDefaultCryptoBtcMarket()),
                false,
                DEFAULT_MAX_TRADE_PRICE_DEVIATION,
                false,
                false,
                false,
                PlatformUtils.getHomeDirectoryPath().toAbsolutePath().toString(),
                false,
                DEFAULT_TOTAL_MAX_BACKUP_SIZE_IN_MB,
                ChatMessageType.ALL,
                DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA,
                DevMode.isDevMode(),
                true,
                MarketRepository.getDefaultBtcFiatMarket(),
                MarketRepository.getDefaultCryptoBtcMarket(),
                MarketRepository.getDefaultBtcFiatMarket());
    }

    SettingsStore(Cookie cookie,
                  Map<String, Boolean> dontShowAgainMap,
                  boolean useAnimations,
                  Market selectedMuSigMarket,
                  long requiredTotalReputationScore,
                  boolean offersOnly,
                  boolean bisqEasyTradeRulesConfirmed,
                  boolean muSigTradeRulesConfirmed,
                  ChatNotificationType chatNotificationType,
                  boolean isTacAccepted,
                  List<TacAcceptance> tacAcceptances,
                  Set<String> consumedAlertIds,
                  boolean closeMyOfferWhenTaken,
                  String languageTag,
                  String countryCode,
                  String currencyCode,
                  boolean preventStandbyMode,
                  Set<String> supportedLanguageTags,
                  double difficultyAdjustmentFactor,
                  boolean ignoreDiffAdjustmentFromSecManager,
                  Set<Market> favouriteMarkets,
                  boolean ignoreMinRequiredReputationScoreFromSecManager,
                  double maxTradePriceDeviation,
                  boolean showBuyOffers,
                  boolean showOfferListExpanded,
                  boolean showMarketSelectionListCollapsed,
                  String backupLocation,
                  boolean showMyOffersOnly,
                  double totalMaxBackupSizeInMB,
                  ChatMessageType bisqEasyOfferbookMessageTypeFilter,
                  int numDaysAfterRedactingTradeData,
                  boolean muSigActivated,
                  boolean autoAddToContactsList,
                  Market muSigLastSelectedFiatMarket,
                  Market muSigLastSelectedOtherMarket,
                  Market selectedWalletMarket) {
        this.cookie = cookie;
        this.dontShowAgainMap.putAll(dontShowAgainMap);
        this.useAnimations.set(useAnimations);
        this.selectedMuSigMarket.set(selectedMuSigMarket);
        this.minRequiredReputationScore.set(requiredTotalReputationScore);
        this.offersOnly.set(offersOnly);
        this.bisqEasyTradeRulesConfirmed.set(bisqEasyTradeRulesConfirmed);
        this.muSigTradeRulesConfirmed.set(muSigTradeRulesConfirmed);
        this.chatNotificationType.set(chatNotificationType);
        this.isTacAccepted.set(isTacAccepted);
        this.tacAcceptances.addAll(tacAcceptances);
        this.consumedAlertIds.setAll(consumedAlertIds);
        this.closeMyOfferWhenTaken.set(closeMyOfferWhenTaken);
        this.languageTag.set(languageTag);
        this.countryCode.set(countryCode);
        this.currencyCode.set(currencyCode);
        this.preventStandbyMode.set(preventStandbyMode);
        this.supportedLanguageTags.setAll(supportedLanguageTags);
        this.difficultyAdjustmentFactor.set(difficultyAdjustmentFactor);
        this.ignoreDiffAdjustmentFromSecManager.set(ignoreDiffAdjustmentFromSecManager);
        this.favouriteMarkets.setAll(favouriteMarkets);
        this.ignoreMinRequiredReputationScoreFromSecManager.set(ignoreMinRequiredReputationScoreFromSecManager);
        this.maxTradePriceDeviation.set(maxTradePriceDeviation);
        this.showBuyOffers.set(showBuyOffers);
        this.showOfferListExpanded.set(showOfferListExpanded);
        this.showMarketSelectionListCollapsed.set(showMarketSelectionListCollapsed);
        this.backupLocation.set(backupLocation);
        this.showMyOffersOnly.set(showMyOffersOnly);
        this.totalMaxBackupSizeInMB.set(totalMaxBackupSizeInMB);
        this.bisqEasyOfferbookMessageTypeFilter.set(bisqEasyOfferbookMessageTypeFilter);
        this.numDaysAfterRedactingTradeData.set(numDaysAfterRedactingTradeData);
        this.muSigActivated.set(muSigActivated);
        this.autoAddToContactsList.set(autoAddToContactsList);
        this.muSigLastSelectedFiatMarket.set(muSigLastSelectedFiatMarket);
        this.muSigLastSelectedOtherMarket.set(muSigLastSelectedOtherMarket);
        this.selectedWalletMarket.set(selectedWalletMarket);
    }

    @SuppressWarnings("deprecation")
    @Override
    public bisq.settings.protobuf.SettingsStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.settings.protobuf.SettingsStore.newBuilder()
                .setCookie(cookie.toProto(serializeForHash))
                .putAllDontShowAgainMap(dontShowAgainMap)
                .setUseAnimations(useAnimations.get())
                .setSelectedMuSigMarket(selectedMuSigMarket.get().toProto(serializeForHash))
                .setMinRequiredReputationScore(minRequiredReputationScore.get())
                .setOffersOnly(offersOnly.get())
                .setBisqEasyTradeRulesConfirmed(bisqEasyTradeRulesConfirmed.get())
                .setMuSigTradeRulesConfirmed(muSigTradeRulesConfirmed.get())
                .setChatNotificationType(chatNotificationType.get().toProtoEnum())
                .setIsTacAccepted(isTacAccepted.get())
                .addAllTacAcceptances(tacAcceptances.stream()
                        .map(tacAcceptance -> tacAcceptance.toProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllConsumedAlertIds(new ArrayList<>(consumedAlertIds))
                .setCloseMyOfferWhenTaken(closeMyOfferWhenTaken.get())
                .setLanguageTag(languageTag.get())
                .setCountryCode(countryCode.get())
                .setCurrencyCode(currencyCode.get())
                .setPreventStandbyMode(preventStandbyMode.get())
                .addAllSupportedLanguageTags(new ArrayList<>(supportedLanguageTags))
                .setDifficultyAdjustmentFactor(difficultyAdjustmentFactor.get())
                .setIgnoreDiffAdjustmentFromSecManager(ignoreDiffAdjustmentFromSecManager.get())
                .addAllFavouriteMarkets(favouriteMarkets.stream().map(market -> market.toProto(serializeForHash)).collect(Collectors.toList()))
                .setIgnoreMinRequiredReputationScoreFromSecManager(ignoreMinRequiredReputationScoreFromSecManager.get())
                .setMaxTradePriceDeviation(maxTradePriceDeviation.get())
                .setShowBuyOffers(showBuyOffers.get())
                .setShowOfferListExpanded(showOfferListExpanded.get())
                .setShowMarketSelectionListCollapsed(showMarketSelectionListCollapsed.get())
                .setBackupLocation(backupLocation.get())
                .setShowMyOffersOnly(showMyOffersOnly.get())
                .setTotalMaxBackupSizeInMB(totalMaxBackupSizeInMB.get())
                .setBisqEasyOfferbookMessageTypeFilter(bisqEasyOfferbookMessageTypeFilter.get().toProtoEnum())
                .setNumDaysAfterRedactingTradeData(numDaysAfterRedactingTradeData.get())
                .setMuSigActivated(muSigActivated.get())
                .setAutoAddToContactsList(autoAddToContactsList.get())
                .setMuSigLastSelectedFiatMarket(muSigLastSelectedFiatMarket.get().toProto(serializeForHash))
                .setMuSigLastSelectedOtherMarket(muSigLastSelectedOtherMarket.get().toProto(serializeForHash))
                .setSelectedWalletMarket(selectedWalletMarket.get().toProto(serializeForHash));
    }

    @Override
    public bisq.settings.protobuf.SettingsStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @SuppressWarnings("deprecation")
    public static SettingsStore fromProto(bisq.settings.protobuf.SettingsStore proto) {
        double maxTradePriceDeviation = proto.getMaxTradePriceDeviation();
        if (maxTradePriceDeviation < MIN_TRADE_PRICE_DEVIATION ||
                maxTradePriceDeviation > MAX_TRADE_PRICE_DEVIATION) {
            maxTradePriceDeviation = DEFAULT_MAX_TRADE_PRICE_DEVIATION;
        }

        double totalMaxBackupSizeInMB = proto.getTotalMaxBackupSizeInMB();
        if (totalMaxBackupSizeInMB == 0) {
            totalMaxBackupSizeInMB = DEFAULT_TOTAL_MAX_BACKUP_SIZE_IN_MB;
        }

        int numDaysAfterRedactingTradeData = proto.getNumDaysAfterRedactingTradeData();
        if (numDaysAfterRedactingTradeData < MIN_NUM_DAYS_AFTER_REDACTING_TRADE_DATA ||
                numDaysAfterRedactingTradeData > MAX_NUM_DAYS_AFTER_REDACTING_TRADE_DATA) {
            numDaysAfterRedactingTradeData = DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA;
        }

        String countryCode = proto.getCountryCode();
        if (StringUtils.isEmpty(countryCode)) {
            countryCode = CountryRepository.getDefaultCountry().getCode();
        }

        String currencyCode = proto.getCurrencyCode();
        if (StringUtils.isEmpty(currencyCode)) {
            currencyCode = FiatCurrencyRepository.getDefaultCurrency().getCode();
        }
// ,
        Market muSigLastSelectedFiatMarket = proto.hasMuSigLastSelectedFiatMarket()
                ? Market.fromProto(proto.getMuSigLastSelectedFiatMarket())
                : MarketRepository.getDefaultBtcFiatMarket();
        Market muSigLastSelectedOtherMarket = proto.hasMuSigLastSelectedOtherMarket()
                ? Market.fromProto(proto.getMuSigLastSelectedOtherMarket())
                : MarketRepository.getDefaultCryptoBtcMarket();
        return new SettingsStore(Cookie.fromProto(proto.getCookie()),
                proto.getDontShowAgainMapMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                proto.getUseAnimations(),
                Market.fromProto(proto.getSelectedMuSigMarket()),
                proto.getMinRequiredReputationScore(),
                proto.getOffersOnly(),
                proto.getBisqEasyTradeRulesConfirmed(),
                proto.getMuSigTradeRulesConfirmed(),
                ChatNotificationType.fromProto(proto.getChatNotificationType()),
                proto.getIsTacAccepted(),
                proto.getTacAcceptancesList().stream()
                        .map(TacAcceptance::fromProto)
                        .collect(Collectors.toList()),
                new HashSet<>(proto.getConsumedAlertIdsList()),
                proto.getCloseMyOfferWhenTaken(),
                proto.getLanguageTag(),
                countryCode,
                currencyCode,
                proto.getPreventStandbyMode(),
                new HashSet<>(proto.getSupportedLanguageTagsList()),
                proto.getDifficultyAdjustmentFactor(),
                proto.getIgnoreDiffAdjustmentFromSecManager(),
                new HashSet<>(proto.getFavouriteMarketsList().stream()
                        .map(Market::fromProto).collect(Collectors.toSet())),
                proto.getIgnoreMinRequiredReputationScoreFromSecManager(),
                maxTradePriceDeviation,
                proto.getShowBuyOffers(),
                proto.getShowOfferListExpanded(),
                proto.getShowMarketSelectionListCollapsed(),
                proto.getBackupLocation(),
                proto.getShowMyOffersOnly(),
                totalMaxBackupSizeInMB,
                ChatMessageType.fromProto(proto.getBisqEasyOfferbookMessageTypeFilter()),
                numDaysAfterRedactingTradeData,
                proto.getMuSigActivated(),
                proto.getAutoAddToContactsList(),
                muSigLastSelectedFiatMarket,
                muSigLastSelectedOtherMarket,
                proto.hasSelectedWalletMarket() ? Market.fromProto(proto.getSelectedWalletMarket()) : MarketRepository.getDefaultBtcFiatMarket());
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
                Map.copyOf(dontShowAgainMap),
                useAnimations.get(),
                selectedMuSigMarket.get(),
                minRequiredReputationScore.get(),
                offersOnly.get(),
                bisqEasyTradeRulesConfirmed.get(),
                muSigTradeRulesConfirmed.get(),
                chatNotificationType.get(),
                isTacAccepted.get(),
                List.copyOf(tacAcceptances),
                Set.copyOf(consumedAlertIds),
                closeMyOfferWhenTaken.get(),
                languageTag.get(),
                countryCode.get(),
                currencyCode.get(),
                preventStandbyMode.get(),
                Set.copyOf(supportedLanguageTags),
                difficultyAdjustmentFactor.get(),
                ignoreDiffAdjustmentFromSecManager.get(),
                Set.copyOf(favouriteMarkets),
                ignoreMinRequiredReputationScoreFromSecManager.get(),
                maxTradePriceDeviation.get(),
                showBuyOffers.get(),
                showOfferListExpanded.get(),
                showMarketSelectionListCollapsed.get(),
                backupLocation.get(),
                showMyOffersOnly.get(),
                totalMaxBackupSizeInMB.get(),
                bisqEasyOfferbookMessageTypeFilter.get(),
                numDaysAfterRedactingTradeData.get(),
                muSigActivated.get(),
                autoAddToContactsList.get(),
                muSigLastSelectedFiatMarket.get(),
                muSigLastSelectedOtherMarket.get(),
                selectedWalletMarket.get());
    }

    @Override
    public void applyPersisted(SettingsStore persisted) {
        try {
            cookie.putAll(persisted.cookie.getMap());
            dontShowAgainMap.putAll(persisted.dontShowAgainMap);
            useAnimations.set(persisted.useAnimations.get());
            selectedMuSigMarket.set(persisted.selectedMuSigMarket.get());
            minRequiredReputationScore.set(persisted.minRequiredReputationScore.get());
            offersOnly.set(persisted.offersOnly.get());
            bisqEasyTradeRulesConfirmed.set(persisted.bisqEasyTradeRulesConfirmed.get());
            muSigTradeRulesConfirmed.set(persisted.muSigTradeRulesConfirmed.get());
            chatNotificationType.set(persisted.chatNotificationType.get());
            isTacAccepted.set(persisted.isTacAccepted.get());
            tacAcceptances.clear();
            tacAcceptances.addAll(persisted.tacAcceptances);
            consumedAlertIds.setAll(persisted.consumedAlertIds);
            closeMyOfferWhenTaken.set(persisted.closeMyOfferWhenTaken.get());
            languageTag.set(persisted.languageTag.get());
            countryCode.set(persisted.countryCode.get());
            currencyCode.set(persisted.currencyCode.get());
            preventStandbyMode.set(persisted.preventStandbyMode.get());
            supportedLanguageTags.setAll(persisted.supportedLanguageTags);
            difficultyAdjustmentFactor.set(persisted.difficultyAdjustmentFactor.get());
            ignoreDiffAdjustmentFromSecManager.set(persisted.ignoreDiffAdjustmentFromSecManager.get());
            favouriteMarkets.setAll(persisted.favouriteMarkets);
            ignoreMinRequiredReputationScoreFromSecManager.set(persisted.ignoreMinRequiredReputationScoreFromSecManager.get());
            maxTradePriceDeviation.set(persisted.maxTradePriceDeviation.get());
            showBuyOffers.set(persisted.showBuyOffers.get());
            showOfferListExpanded.set(persisted.showOfferListExpanded.get());
            showMarketSelectionListCollapsed.set(persisted.showMarketSelectionListCollapsed.get());
            backupLocation.set(persisted.backupLocation.get());
            showMyOffersOnly.set(persisted.showMyOffersOnly.get());
            totalMaxBackupSizeInMB.set(persisted.totalMaxBackupSizeInMB.get());
            bisqEasyOfferbookMessageTypeFilter.set(persisted.bisqEasyOfferbookMessageTypeFilter.get());
            numDaysAfterRedactingTradeData.set(persisted.numDaysAfterRedactingTradeData.get());
            muSigActivated.set(persisted.muSigActivated.get());
            autoAddToContactsList.set(persisted.autoAddToContactsList.get());
            muSigLastSelectedFiatMarket.set(persisted.muSigLastSelectedFiatMarket.get());
            muSigLastSelectedOtherMarket.set(persisted.muSigLastSelectedOtherMarket.get());
            selectedWalletMarket.set(persisted.selectedWalletMarket.get());
        } catch (Exception e) {
            log.error("Exception at applyPersisted", e);
        }
    }
}