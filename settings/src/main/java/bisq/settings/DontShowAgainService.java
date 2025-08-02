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

import java.util.concurrent.CompletableFuture;

/**
 * Service for managing "don't show again" preferences for popups and warnings.
 * 
 * This service supports two types of keys:
 * 1. Enum-based keys (preferred): For static, well-known popups that are part of the core UI.
 *    These are defined in {@link DontShowAgainKey} and should be used whenever possible.
 * 
 * 2. String-based keys (for dynamic cases): For dynamically generated popups where the key
 *    needs to incorporate runtime information (e.g., a trade ID). These should only be used
 *    when the key cannot be known at compile time.
 *    Example: "errorMessage_" + tradeId
 */
public class DontShowAgainService implements Service {
    private final SettingsService settingsService;

    public DontShowAgainService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public boolean showAgain(DontShowAgainKey key) {
        return showAgain(key.getKey());
    }

    public boolean showAgain(String key) {
        return !settingsService.getDontShowAgainMap().containsKey(key) ||
                !settingsService.getDontShowAgainMap().get(key);
    }

    public void dontShowAgain(DontShowAgainKey key) {
        putDontShowAgain(key.getKey(), true);
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
        settingsService.persist();
    }
}
