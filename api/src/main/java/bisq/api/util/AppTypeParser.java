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

package bisq.api.util;

import bisq.bonded_roles.release.AppType;

import java.util.Locale;
import java.util.Optional;

public final class AppTypeParser {
    private AppTypeParser() {
    }

    public static AppType parse(String appTypeParam) {
        if (appTypeParam == null || appTypeParam.isBlank()) {
            throw new IllegalArgumentException("appType parameter is required");
        }
        try {
            return AppType.valueOf(appTypeParam.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid appType: " + appTypeParam);
        }
    }

    public static AppType parse(Optional<String> appTypeParam) {
        return parse(appTypeParam.orElse(null));
    }
}