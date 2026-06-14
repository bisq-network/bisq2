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

package bisq.desktop_ui_harness_app;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesktopUiHarnessConfigTest {
    @Test
    void fromUsesDefaultsForOptionalProperties() {
        DesktopUiHarnessConfig config = DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token"
        )::get);

        assertThat(config.bindHost()).isEqualTo("127.0.0.1");
        assertThat(config.bindPort()).isEqualTo(18180);
        assertThat(config.fxTimeoutMs()).isEqualTo(5000L);
        assertThat(config.windowWidth()).isEqualTo(1440d);
        assertThat(config.windowHeight()).isEqualTo(900d);
        assertThat(config.stageTimeoutMs()).isEqualTo(40000L);
        assertThat(config.token()).isEqualTo("secret-token");
    }

    @Test
    void fromRequiresToken() {
        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.<String, String>of()::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.token");

        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "   "
        )::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.token");
    }

    @Test
    void toAutomationConfigPreservesResolvedValues() {
        DesktopUiHarnessConfig config = DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.bind.host", "localhost",
                "bisq.desktopUiHarness.bind.port", "19090",
                "bisq.desktopUiHarness.fx.timeoutMs", "7777",
                "bisq.desktopUiHarness.window.width", "1600",
                "bisq.desktopUiHarness.window.height", "1000",
                "bisq.desktopUiHarness.token", "abc123",
                "bisq.desktopUiHarness.artifacts.dir", "/tmp/harness-artifacts",
                "bisq.desktopUiHarness.stage.timeoutMs", "12345"
        )::get);

        assertThat(config.toAutomationConfig().bindHost()).isEqualTo("localhost");
        assertThat(config.toAutomationConfig().bindPort()).isEqualTo(19090);
        assertThat(config.toAutomationConfig().fxTimeoutMs()).isEqualTo(7777L);
        assertThat(config.toAutomationConfig().defaultWidth()).isEqualTo(1600d);
        assertThat(config.toAutomationConfig().defaultHeight()).isEqualTo(1000d);
        assertThat(config.toAutomationConfig().token()).isEqualTo("abc123");
        assertThat(config.toAutomationConfig().artifactsDir()).isEqualTo("/tmp/harness-artifacts");
        assertThat(config.stageTimeoutMs()).isEqualTo(12345L);
    }

    @Test
    void fromRejectsInvalidNumericProperties() {
        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token",
                "bisq.desktopUiHarness.bind.port", "0"
        )::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.bind.port");

        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token",
                "bisq.desktopUiHarness.fx.timeoutMs", "0"
        )::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.fx.timeoutMs");

        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token",
                "bisq.desktopUiHarness.window.width", "-1"
        )::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.window.width");

        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token",
                "bisq.desktopUiHarness.window.height", "0"
        )::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.window.height");

        assertThatThrownBy(() -> DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token",
                "bisq.desktopUiHarness.stage.timeoutMs", "-5"
        )::get))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bisq.desktopUiHarness.stage.timeoutMs");
    }

    @Test
    void fromTrimsArtifactsDir() {
        DesktopUiHarnessConfig config = DesktopUiHarnessConfig.from(Map.of(
                "bisq.desktopUiHarness.token", "secret-token",
                "bisq.desktopUiHarness.artifacts.dir", " /tmp/harness-artifacts "
        )::get);

        assertThat(config.artifactsDir()).hasToString("/tmp/harness-artifacts");
    }
}
