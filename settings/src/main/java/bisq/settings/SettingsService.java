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

import bisq.common.application.Service;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.Market;
import bisq.common.data.ByteUnit;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.persistence.backup.BackupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SettingsService implements PersistenceClient<SettingsStore>, Service {
    @Deprecated(since = "2.1.1")
    public final static long DEFAULT_MIN_REQUIRED_REPUTATION_SCORE = 30_000;

    public final static double DEFAULT_MAX_TRADE_PRICE_DEVIATION = 0.05; // 5%
    public final static double MIN_TRADE_PRICE_DEVIATION = 0.01; // 1%
    public final static double MAX_TRADE_PRICE_DEVIATION = 0.1; // 10%

    public final static int DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA = 90;
    public final static int MIN_NUM_DAYS_AFTER_REDACTING_TRADE_DATA = 30;
    public final static int MAX_NUM_DAYS_AFTER_REDACTING_TRADE_DATA = 365;

    public final static double DEFAULT_TOTAL_MAX_BACKUP_SIZE_IN_MB = 100;
    public final static double MIN_TOTAL_MAX_BACKUP_SIZE_IN_MB = 1;
    public final static double MAX_TOTAL_MAX_BACKUP_SIZE_IN_MB = 1000;

    @Getter
    private static SettingsService instance;

    @Getter
    private final SettingsStore persistableStore = new SettingsStore();
    @Getter
    private final Persistence<SettingsStore> persistence;
    @Getter
    private final Observable<Boolean> cookieChanged = new Observable<>(false);
    private boolean isInitialized;

    public SettingsService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
        instance = this;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        // If used with FxBindings.bindBiDir we need to trigger persist call
        getIsTacAccepted().addObserver(value -> persist());
        getChatNotificationType().addObserver(value -> persist());
        getUseAnimations().addObserver(value -> persist());
        getPreventStandbyMode().addObserver(value -> persist());
        getCloseMyOfferWhenTaken().addObserver(value -> persist());
        getConsumedAlertIds().addObserver(this::persist);
        getSupportedLanguageCodes().addObserver(this::persist);
        getSelectedMarket().addObserver(value -> persist());
        getTradeRulesConfirmed().addObserver(value -> persist());
        getLanguageCode().addObserver(value -> persist());
        getDifficultyAdjustmentFactor().addObserver(value -> persist());
        getIgnoreDiffAdjustmentFromSecManager().addObserver(value -> persist());
        getFavouriteMarkets().addObserver(this::persist);
        getMaxTradePriceDeviation().addObserver(value -> persist());
        getShowBuyOffers().addObserver(value -> persist());
        getShowOfferListExpanded().addObserver(value -> persist());
        getShowMarketSelectionListCollapsed().addObserver(value -> persist());
        getBackupLocation().addObserver(value -> persist());
        getShowMyOffersOnly().addObserver(value -> persist());
        getTotalMaxBackupSizeInMB().addObserver(value -> {
            BackupService.setTotalMaxBackupSize(ByteUnit.MB.toBytes(value));
            persist();
        });
        getBisqEasyOfferbookMessageTypeFilter().addObserver(value -> persist());
        getNumDaysAfterRedactingTradeData().addObserver(value -> persist());

        isInitialized = true;

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        // We don't want to call persist from the addObserver calls at initialize
        if (isInitialized) {
            return PersistenceClient.super.persist();
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    @Override
    public void onPersistedApplied(SettingsStore persisted) {
        String languageCode = getLanguageCode().get();

        LanguageRepository.setDefaultLanguage(languageCode);
        Res.setLanguage(languageCode);
        Locale currentLocale = LocaleRepository.getDefaultLocale();
        Locale newLocale = new Locale(languageCode, currentLocale.getCountry(), currentLocale.getVariant());
        LocaleRepository.setDefaultLocale(newLocale);
        CountryRepository.applyDefaultLocale(newLocale);
        FiatCurrencyRepository.setLocale(newLocale);
    }


    /* --------------------------------------------------------------------- */
    // Getters for Observable
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<Market> getSelectedMarket() {
        return persistableStore.selectedMarket;
    }

    public ReadOnlyObservable<Boolean> getUseAnimations() {
        return persistableStore.useAnimations;
    }

    public ReadOnlyObservable<Boolean> getTradeRulesConfirmed() {
        return persistableStore.tradeRulesConfirmed;
    }

    public ReadOnlyObservable<Boolean> getPreventStandbyMode() {
        return persistableStore.preventStandbyMode;
    }

    public ReadOnlyObservable<Boolean> getIgnoreDiffAdjustmentFromSecManager() {
        return persistableStore.ignoreDiffAdjustmentFromSecManager;
    }

    public ReadOnlyObservable<Double> getDifficultyAdjustmentFactor() {
        return persistableStore.difficultyAdjustmentFactor;
    }

    public ReadOnlyObservable<Double> getMaxTradePriceDeviation() {
        return persistableStore.maxTradePriceDeviation;
    }

    public ReadOnlyObservable<ChatNotificationType> getChatNotificationType() {
        return persistableStore.chatNotificationType;
    }

    public ReadOnlyObservable<Boolean> getIsTacAccepted() {
        return persistableStore.isTacAccepted;
    }

    public ObservableSet<String> getConsumedAlertIds() {
        return persistableStore.consumedAlertIds;
    }

    public ObservableSet<String> getSupportedLanguageCodes() {
        return persistableStore.supportedLanguageCodes;
    }

    public ReadOnlyObservable<Boolean> getCloseMyOfferWhenTaken() {
        return persistableStore.closeMyOfferWhenTaken;
    }

    public ReadOnlyObservable<String> getLanguageCode() {
        return persistableStore.languageCode;
    }

    public ObservableSet<Market> getFavouriteMarkets() {
        return persistableStore.favouriteMarkets;
    }

    public ReadOnlyObservable<Boolean> getShowBuyOffers() {
        return persistableStore.showBuyOffers;
    }

    public ReadOnlyObservable<Boolean> getShowOfferListExpanded() {
        return persistableStore.showOfferListExpanded;
    }

    public ReadOnlyObservable<Boolean> getShowMarketSelectionListCollapsed() {
        return persistableStore.showMarketSelectionListCollapsed;
    }

    public ReadOnlyObservable<String> getBackupLocation() {
        return persistableStore.backupLocation;
    }

    public ReadOnlyObservable<Boolean> getShowMyOffersOnly() {
        return persistableStore.showMyOffersOnly;
    }

    public ReadOnlyObservable<Double> getTotalMaxBackupSizeInMB() {
        return persistableStore.totalMaxBackupSizeInMB;
    }

    public ReadOnlyObservable<ChatMessageType> getBisqEasyOfferbookMessageTypeFilter() {
        return persistableStore.bisqEasyOfferbookMessageTypeFilter;
    }

    public ReadOnlyObservable<Integer> getNumDaysAfterRedactingTradeData() {
        return persistableStore.numDaysAfterRedactingTradeData;
    }


    /* --------------------------------------------------------------------- */
    // Setters
    /* --------------------------------------------------------------------- */

    public void setSelectedMarket(Market market) {
        if (market != null) {
            persistableStore.selectedMarket.set(market);
        }
    }

    public void setUseAnimations(boolean useAnimations) {
        persistableStore.useAnimations.set(useAnimations);
    }

    public void setTradeRulesConfirmed(boolean tradeRulesConfirmed) {
        persistableStore.tradeRulesConfirmed.set(tradeRulesConfirmed);
    }

    public void setPreventStandbyMode(boolean preventStandbyMode) {
        persistableStore.preventStandbyMode.set(preventStandbyMode);
    }

    public void setIgnoreDiffAdjustmentFromSecManager(boolean ignoreDiffAdjustmentFromSecManager) {
        persistableStore.ignoreDiffAdjustmentFromSecManager.set(ignoreDiffAdjustmentFromSecManager);
    }

    public void setDifficultyAdjustmentFactor(double value) {
        if (value >= NetworkLoad.MIN_DIFFICULTY_ADJUSTMENT && value <= NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT) {
            persistableStore.difficultyAdjustmentFactor.set(value);
        }
    }

    public void setMaxTradePriceDeviation(double value) {
        if (value >= MIN_TRADE_PRICE_DEVIATION && value <= MAX_TRADE_PRICE_DEVIATION) {
            persistableStore.maxTradePriceDeviation.set(value);
        }
    }

    public void setChatNotificationType(ChatNotificationType chatNotificationType) {
        persistableStore.chatNotificationType.set(chatNotificationType);
    }

    public void setIsTacAccepted(boolean isTacAccepted) {
        persistableStore.isTacAccepted.set(isTacAccepted);
    }

    public void setCloseMyOfferWhenTaken(boolean closeMyOfferWhenTaken) {
        persistableStore.closeMyOfferWhenTaken.set(closeMyOfferWhenTaken);
    }

    public void setLanguageCode(String languageCode) {
        if (languageCode != null && LanguageRepository.CODES.contains(languageCode)) {
            persistableStore.languageCode.set(languageCode);
        }
    }

    public void setShowBuyOffers(boolean showBuyOffers) {
        persistableStore.showBuyOffers.set(showBuyOffers);
    }

    public void setShowOfferListExpanded(boolean showOfferListExpanded) {
        persistableStore.showOfferListExpanded.set(showOfferListExpanded);
    }

    public void setShowMarketSelectionListCollapsed(boolean showMarketSelectionListCollapsed) {
        persistableStore.showMarketSelectionListCollapsed.set(showMarketSelectionListCollapsed);
    }

    public void setBackupLocation(String backupLocation) {
        if (backupLocation != null) {
            persistableStore.backupLocation.set(backupLocation);
        }
    }

    public void setShowMyOffersOnly(boolean showMyOffersOnly) {
        persistableStore.showMyOffersOnly.set(showMyOffersOnly);
    }

    public void setTotalMaxBackupSizeInMB(double value) {
        if (value >= MIN_TOTAL_MAX_BACKUP_SIZE_IN_MB && value <= MAX_TOTAL_MAX_BACKUP_SIZE_IN_MB) {
            persistableStore.totalMaxBackupSizeInMB.set(value);
        }
    }

    public void setBisqEasyOfferbookMessageTypeFilter(ChatMessageType chatMessageType) {
        persistableStore.bisqEasyOfferbookMessageTypeFilter.set(chatMessageType);
    }

    public void setNumDaysAfterRedactingTradeData(int value) {
        if (value >= MIN_NUM_DAYS_AFTER_REDACTING_TRADE_DATA && value <= MAX_NUM_DAYS_AFTER_REDACTING_TRADE_DATA) {
            persistableStore.numDaysAfterRedactingTradeData.set(value);
        }
    }


    /* --------------------------------------------------------------------- */
    // DontShowAgainMap
    /* --------------------------------------------------------------------- */

    public Map<String, Boolean> getDontShowAgainMap() {
        return persistableStore.dontShowAgainMap;
    }


    /* --------------------------------------------------------------------- */
    // Cookie
    /* --------------------------------------------------------------------- */

    public Cookie getCookie() {
        return persistableStore.cookie;
    }

    public void setCookie(CookieKey key, boolean value) {
        setCookie(key, null, value);
    }

    public void setCookie(CookieKey key, String subKey, boolean value) {
        getCookie().putAsBoolean(key, subKey, value);
        persist();
        updateCookieChangedFlag();
    }

    public void setCookie(CookieKey key, double value) {
        setCookie(key, null, value);
    }

    public void setCookie(CookieKey key, String subKey, double value) {
        getCookie().putAsDouble(key, subKey, value);
        persist();
        updateCookieChangedFlag();
    }

    public void setCookie(CookieKey key, String value) {
        setCookie(key, null, value);
    }

    public void setCookie(CookieKey key, @Nullable String subKey, String value) {
        getCookie().putAsString(key, subKey, value);
        persist();
        updateCookieChangedFlag();
    }

    public void removeCookie(CookieKey key) {
        removeCookie(key, null);
    }

    public void removeCookie(CookieKey key, @Nullable String subKey) {
        getCookie().remove(key, subKey);
        persist();
        updateCookieChangedFlag();
    }

    private void updateCookieChangedFlag() {
        cookieChanged.set(!cookieChanged.get());
    }
}
