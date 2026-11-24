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

public final class TradeRestrictingAppType {
    private TradeRestrictingAppType() {
    }

    /**
     * The app type whose trade restricting alerts apply to the trades of the requested app type on this node. A mobile
     * client trades through this node, so the alerts which this node enforces apply to its trades.
     */
    public static AppType toEnforcedAppType(AppType requestedAppType, AppType nodeAppType) {
        return requestedAppType == AppType.MOBILE_CLIENT ? nodeAppType : requestedAppType;
    }
}
