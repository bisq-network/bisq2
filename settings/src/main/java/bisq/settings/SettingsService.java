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
import bisq.common.application.Service;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.data.ByteUnit;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.market.Market;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.persistence.backup.BackupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SettingsService extends RateLimitedPersistenceClient<SettingsStore> implements Service {
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

    private final Set<Pin> pins = new HashSet<>();

    public SettingsService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
        instance = this;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (isInitialized) {
            log.info("SettingsService already initialized, skipping initialization");
            return CompletableFuture.completedFuture(true);
        }

        // If used with FxBindings.bindBiDir we need to trigger persist call
        pins.add(getIsTacAccepted().addObserver(value -> persist()));
        pins.add(getChatNotificationType().addObserver(value -> persist()));
        pins.add(getUseAnimations().addObserver(value -> persist()));
        pins.add(getPreventStandbyMode().addObserver(value -> persist()));
        pins.add(getCloseMyOfferWhenTaken().addObserver(value -> persist()));
        pins.add(getConsumedAlertIds().addObserver(this::persist));
        pins.add(getSupportedLanguageTags().addObserver(this::persist));
        pins.add(getSelectedMuSigMarket().addObserver(value -> persist()));
        pins.add(getTradeRulesConfirmed().addObserver(value -> persist()));
        pins.add(getLanguageTag().addObserver(value -> persist()));
        pins.add(getDifficultyAdjustmentFactor().addObserver(value -> persist()));
        pins.add(getIgnoreDiffAdjustmentFromSecManager().addObserver(value -> persist()));
        pins.add(getFavouriteMarkets().addObserver(this::persist));
        pins.add(getMaxTradePriceDeviation().addObserver(value -> persist()));
        pins.add(getShowBuyOffers().addObserver(value -> persist()));
        pins.add(getShowOfferListExpanded().addObserver(value -> persist()));
        pins.add(getShowMarketSelectionListCollapsed().addObserver(value -> persist()));
        pins.add(getBackupLocation().addObserver(value -> persist()));
        pins.add(getShowMyOffersOnly().addObserver(value -> persist()));
        pins.add(getTotalMaxBackupSizeInMB().addObserver(value -> {
            BackupService.setTotalMaxBackupSize(ByteUnit.MB.toBytes(value));
            persist();
        }));
        pins.add(getBisqEasyOfferbookMessageTypeFilter().addObserver(value -> persist()));
        pins.add(getNumDaysAfterRedactingTradeData().addObserver(value -> persist()));
        pins.add(getMuSigActivated().addObserver(value -> persist()));
        pins.add(getAutoAddToContactsList().addObserver(value -> persist()));
        pins.add(getSelectedWalletMarket().addObserver(value -> persist()));

        isInitialized = true;

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (!isInitialized) {
            return CompletableFuture.completedFuture(true);
        }

        pins.forEach(Pin::unbind);
        pins.clear();

        isInitialized = false;
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        // We don't want to call persist from the addObserver calls at initialize
        if (isInitialized) {
            return super.persist();
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    @Override
    public void onPersistedApplied(SettingsStore persisted) {
        String languageTag = getLanguageTag().get();
        LanguageRepository.setDefaultLanguageTag(languageTag);
        Res.setAndApplyLanguageTag(languageTag);
        Locale locale = Locale.forLanguageTag(languageTag);
        Locale newLocale = LocaleRepository.ensureValidLocale(locale);
        LocaleRepository.setDefaultLocale(newLocale);
        CountryRepository.applyDefaultLocale(newLocale);
        FiatCurrencyRepository.setLocale(newLocale);
    }


    /* --------------------------------------------------------------------- */
    // Getters for Observable
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<Market> getSelectedMuSigMarket() {
        return persistableStore.selectedMuSigMarket;
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

    public ObservableSet<String> getSupportedLanguageTags() {
        return persistableStore.supportedLanguageTags;
    }

    public ReadOnlyObservable<Boolean> getCloseMyOfferWhenTaken() {
        return persistableStore.closeMyOfferWhenTaken;
    }

    public ReadOnlyObservable<String> getLanguageTag() {
        return persistableStore.languageTag;
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

    public ReadOnlyObservable<Boolean> getMuSigActivated() {
        return persistableStore.muSigActivated;
    }

    public ReadOnlyObservable<Boolean> getAutoAddToContactsList() {
        return persistableStore.autoAddToContactsList;
    }

    public boolean getDoAutoAddToContactList() {
        return getAutoAddToContactsList().get();
    }

    public Map<String, Market> getMuSigLastSelectedMarketByBaseCurrencyMap() {
        return Collections.unmodifiableMap(persistableStore.muSigLastSelectedMarketByBaseCurrencyMap);
    }

    public ReadOnlyObservable<Market> getSelectedWalletMarket() {
        return persistableStore.selectedWalletMarket;
    }


    /* --------------------------------------------------------------------- */
    // Setters
    /* --------------------------------------------------------------------- */

    public void setSelectedMuSigMarket(Market market) {
        if (market != null) {
            persistableStore.selectedMuSigMarket.set(market);
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

    public void setLanguageTag(String languageTag) {
        if (languageTag != null && LanguageRepository.LANGUAGE_TAGS.contains(languageTag)) {
            persistableStore.languageTag.set(languageTag);
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

    public void setMuSigActivated(boolean muSigActivated) {
        persistableStore.muSigActivated.set(DevMode.isDevMode() && muSigActivated);
    }

    public void setAutoAddToContactsList(boolean value) {
        persistableStore.autoAddToContactsList.set(value);
    }

    public void setMuSigLastSelectedMarketByBaseCurrencyMap(Market market) {
        if (market != null) {
            persistableStore.muSigLastSelectedMarketByBaseCurrencyMap.put(market.getBaseCurrencyCode(), market);
            persist();
        }
    }

    public void setSelectedWalletMarket(Market market) {
        if (market != null) {
            persistableStore.selectedWalletMarket.set(market);
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
