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
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
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
        SettingsService.instance = this;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        // If used with FxBindings.bindBiDir we need to trigger persist call
        getIsTacAccepted().addObserver(value -> persist());
        getOffersOnly().addObserver(value -> persist());
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
            return getPersistence().persistAsync(getPersistableStore().getClone())
                    .handle((r, t) -> true);
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters for Observable
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<Market> getSelectedMarket() {
        return persistableStore.selectedMarket;
    }

    public Observable<Boolean> getUseAnimations() {
        return persistableStore.useAnimations;
    }

    public Observable<Boolean> getOffersOnly() {
        return persistableStore.offersOnly;
    }

    public Observable<Boolean> getTradeRulesConfirmed() {
        return persistableStore.tradeRulesConfirmed;
    }

    public Observable<Boolean> getPreventStandbyMode() {
        return persistableStore.preventStandbyMode;
    }

    public Observable<Boolean> getIgnoreDiffAdjustmentFromSecManager() {
        return persistableStore.ignoreDiffAdjustmentFromSecManager;
    }

    public Observable<Double> getDifficultyAdjustmentFactor() {
        return persistableStore.difficultyAdjustmentFactor;
    }

    public Observable<Double> getMaxTradePriceDeviation() {
        return persistableStore.maxTradePriceDeviation;
    }

    public Observable<ChatNotificationType> getChatNotificationType() {
        return persistableStore.chatNotificationType;
    }

    public Observable<Boolean> getIsTacAccepted() {
        return persistableStore.isTacAccepted;
    }

    public ObservableSet<String> getConsumedAlertIds() {
        return persistableStore.consumedAlertIds;
    }

    public ObservableSet<String> getSupportedLanguageCodes() {
        return persistableStore.supportedLanguageCodes;
    }

    public Observable<Boolean> getCloseMyOfferWhenTaken() {
        return persistableStore.closeMyOfferWhenTaken;
    }

    public Observable<String> getLanguageCode() {
        return persistableStore.languageCode;
    }

    public ObservableSet<Market> getFavouriteMarkets() {
        return persistableStore.favouriteMarkets;
    }

    public Observable<Boolean> getShowBuyOffers() {
        return persistableStore.showBuyOffers;
    }

    public Observable<Boolean> getShowOfferListExpanded() {
        return persistableStore.showOfferListExpanded;
    }

    public Observable<Boolean> getShowMarketSelectionListCollapsed() {
        return persistableStore.showMarketSelectionListCollapsed;
    }

    public Observable<String> getBackupLocation() {
        return persistableStore.backupLocation;
    }

    public Observable<Boolean> getShowMyOffersOnly() {
        return persistableStore.showMyOffersOnly;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DontShowAgainMap
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, Boolean> getDontShowAgainMap() {
        return persistableStore.dontShowAgainMap;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Cookie
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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
