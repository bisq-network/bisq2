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

package bisq.network.tor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UseExternalTorResolverTest {

    @Test
    void whonixAlwaysUsesExternalTor() {
        boolean result = UseExternalTorResolver.evaluateUseExternalTorValue(
                true,
                null,
                false,
                "0");

        assertThat(result).isTrue();
    }

    @Test
    void torSkipLaunchEnvHasHighestPriority() {
        boolean result = UseExternalTorResolver.evaluateUseExternalTorValue(
                false,
                "true",
                false,
                "0");

        assertThat(result).isTrue();
    }

    @Test
    void torSkipLaunchEnvCanDisableExternalTor() {
        boolean result = UseExternalTorResolver.evaluateUseExternalTorValue(
                false,
                "0",
                true,
                "1");

        assertThat(result).isFalse();
    }

    @Test
    void jvmUseExternalTorOverridesExternalTorConfig() {
        boolean result = UseExternalTorResolver.evaluateUseExternalTorValue(
                false,
                null,
                true,
                "0");

        assertThat(result).isTrue();
    }

    @Test
    void externalTorConfigIsUsedWhenNoHigherPriorityInputIsSet() {
        boolean result = UseExternalTorResolver.evaluateUseExternalTorValue(
                false,
                null,
                false,
                "1");

        assertThat(result).isTrue();
    }
}
