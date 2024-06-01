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
import bisq.common.observable.Observable;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

public class DontShowAgainService implements Service {
    @Getter
    private static final Observable<Integer> updateFlag = new Observable<>(0);

    private final SettingsService settingsService;

    public DontShowAgainService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public boolean showAgain(DontShowAgainKey key) {
        return showAgain(key.name());
    }

    public boolean showAgain(String key) {
        return !settingsService.getDontShowAgainMap().containsKey(key) ||
                !settingsService.getDontShowAgainMap().get(key);
    }

    public void dontShowAgain(DontShowAgainKey key) {
        putDontShowAgain(key.name(), true);
    }

    public void putDontShowAgain(String key, boolean dontShowAgain) {
        settingsService.getDontShowAgainMap().put(key, dontShowAgain);
        persist();
    }

    public void resetDontShowAgain() {
        settingsService.getDontShowAgainMap().clear();
        persist();
    }

    private void persist() {
        updateFlag.set(updateFlag.get() + 1);
        settingsService.persist();
    }

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }
}
