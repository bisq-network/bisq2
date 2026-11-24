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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeRestrictingAppTypeTest {

    @Test
    void mobileClientMapsToTheAppTypeOfTheNode() {
        assertThat(TradeRestrictingAppType.toEnforcedAppType(AppType.MOBILE_CLIENT, AppType.MOBILE_NODE)).isEqualTo(AppType.MOBILE_NODE);
        assertThat(TradeRestrictingAppType.toEnforcedAppType(AppType.MOBILE_CLIENT, AppType.DESKTOP)).isEqualTo(AppType.DESKTOP);
    }

    @Test
    void otherAppTypesAreKept() {
        assertThat(TradeRestrictingAppType.toEnforcedAppType(AppType.DESKTOP, AppType.MOBILE_NODE)).isEqualTo(AppType.DESKTOP);
        assertThat(TradeRestrictingAppType.toEnforcedAppType(AppType.MOBILE_NODE, AppType.DESKTOP)).isEqualTo(AppType.MOBILE_NODE);
        assertThat(TradeRestrictingAppType.toEnforcedAppType(AppType.UNSPECIFIED, AppType.MOBILE_NODE)).isEqualTo(AppType.UNSPECIFIED);
    }
}
