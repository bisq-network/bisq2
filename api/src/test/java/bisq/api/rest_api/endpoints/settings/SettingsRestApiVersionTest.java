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

package bisq.api.rest_api.endpoints.settings;

import bisq.common.application.ApplicationVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsRestApiVersionTest {
    @Test
    void reportsCoreVersionWhenNoImageVersionIsSet() {
        assertThat(SettingsRestApi.resolveVersion(null))
                .isEqualTo(ApplicationVersion.getVersion().toString());
        assertThat(SettingsRestApi.resolveVersion(""))
                .isEqualTo(ApplicationVersion.getVersion().toString());
        assertThat(SettingsRestApi.resolveVersion("   "))
                .isEqualTo(ApplicationVersion.getVersion().toString());
    }

    @Test
    void reportsGranularImageVersionWhenSet() {
        assertThat(SettingsRestApi.resolveVersion("2.1.11.2")).isEqualTo("2.1.11.2");
        assertThat(SettingsRestApi.resolveVersion(" 2.1.11.3-rc1 ")).isEqualTo("2.1.11.3-rc1");
    }
}
