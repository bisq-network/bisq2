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

import bisq.common.observable.Observable;
import lombok.Getter;

/**
 * Static convenience class for handling "don't show again" flags
 */
public class DontShowAgainService {
    private static SettingsService settingsService;
    private static SettingsStore persistableStore;
    @Getter
    private static final Observable<Integer> updateFlag = new Observable<>(0);

    static void setSettingsService(SettingsService settingsService) {
        DontShowAgainService.settingsService = settingsService;
        persistableStore = settingsService.getPersistableStore();
    }

    public static boolean showAgain(DontShowAgainKey key) {
        return showAgain(key.name());
    }

    public static boolean showAgain(String key) {
        return !persistableStore.getDontShowAgainMap().containsKey(key) ||
                !persistableStore.getDontShowAgainMap().get(key);
    }

    public static void dontShowAgain(DontShowAgainKey key) {
        putDontShowAgain(key.name(), true);
    }

    public static void putDontShowAgain(String key, boolean dontShowAgain) {
        persistableStore.getDontShowAgainMap().put(key, dontShowAgain);
        persist();
    }

    public static void resetDontShowAgain() {
        persistableStore.getDontShowAgainMap().clear();
        persist();
    }

    private static void persist() {
        updateFlag.set(updateFlag.get() + 1);
        settingsService.persist();
    }
}
