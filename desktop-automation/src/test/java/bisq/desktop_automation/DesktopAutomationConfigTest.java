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

package bisq.desktop_automation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesktopAutomationConfigTest {
    @Test
    void constructorTrimsStringInputs() {
        DesktopAutomationConfig config = new DesktopAutomationConfig(
                " 127.0.0.1 ",
                18180,
                5000L,
                1440d,
                900d,
                " secret-token ",
                " /tmp/bisq2-ui-harness/artifacts "
        );

        assertThat(config.bindHost()).isEqualTo("127.0.0.1");
        assertThat(config.token()).isEqualTo("secret-token");
        assertThat(config.artifactsDir()).isEqualTo("/tmp/bisq2-ui-harness/artifacts");
    }

    @Test
    void constructorRejectsInvalidValues() {
        assertThatThrownBy(() -> new DesktopAutomationConfig(
                "127.0.0.1", 0, 5000L, 1440d, 900d, "secret-token", "/tmp/artifacts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindPort");

        assertThatThrownBy(() -> new DesktopAutomationConfig(
                "127.0.0.1", 18180, 0L, 1440d, 900d, "secret-token", "/tmp/artifacts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fxTimeoutMs");

        assertThatThrownBy(() -> new DesktopAutomationConfig(
                "127.0.0.1", 18180, 5000L, 0d, 900d, "secret-token", "/tmp/artifacts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultWidth");

        assertThatThrownBy(() -> new DesktopAutomationConfig(
                "127.0.0.1", 18180, 5000L, 1440d, 0d, "secret-token", "/tmp/artifacts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultHeight");

        assertThatThrownBy(() -> new DesktopAutomationConfig(
                " ", 18180, 5000L, 1440d, 900d, "secret-token", "/tmp/artifacts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bindHost");

        assertThatThrownBy(() -> new DesktopAutomationConfig(
                "127.0.0.1", 18180, 5000L, 1440d, 900d, " ", "/tmp/artifacts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token");

        assertThatThrownBy(() -> new DesktopAutomationConfig(
                "127.0.0.1", 18180, 5000L, 1440d, 900d, "secret-token", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifactsDir");
    }
}
