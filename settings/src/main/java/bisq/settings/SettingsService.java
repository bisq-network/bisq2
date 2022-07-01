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

import bisq.common.application.ModuleService;
import bisq.common.currency.Market;
import bisq.common.observable.ObservableSet;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class SettingsService implements PersistenceClient<SettingsStore>, ModuleService {
    @Getter
    private final SettingsStore persistableStore = new SettingsStore();
    @Getter
    private final Persistence<SettingsStore> persistence;

    public SettingsService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        DontShowAgainService.setSettingsService(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ModuleService
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }

    public DisplaySettings getDisplaySettings() {
        return persistableStore.getDisplaySettings();
    }

    public ObservableSet<Market> getMarkets() {
        return persistableStore.getMarkets();
    }

    public Market getSelectedMarket() {
        return persistableStore.getSelectedMarket();
    }

    public Cookie getCookie() {
        return persistableStore.getCookie();
    }

    public long getRequiredTotalReputationScore() {
        return persistableStore.getRequiredTotalReputationScore();
    }

    public void setCookie(CookieKey key, boolean value) {
        getCookie().putAsBoolean(key, value);
        persist();
    }

    public void setCookie(CookieKey key, double value) {
        getCookie().putAsDouble(key, value);
        persist();
    }

    public void setCookie(CookieKey key, String value) {
        getCookie().put(key, value);
        persist();
    }

    public void setRequiredTotalReputationScore(long value) {
        persistableStore.setRequiredTotalReputationScore(value);
        persist();
    }
}