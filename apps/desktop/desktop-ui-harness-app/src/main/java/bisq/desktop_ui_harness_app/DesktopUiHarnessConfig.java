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

import bisq.desktop_automation.DesktopAutomationConfig;

import java.nio.file.Path;
import java.util.function.Function;

record DesktopUiHarnessConfig(String bindHost,
                              int bindPort,
                              long fxTimeoutMs,
                              double windowWidth,
                              double windowHeight,
                              String token,
                              Path artifactsDir,
                              long stageTimeoutMs) {
    private static final String PREFIX = "bisq.desktopUiHarness.";

    static DesktopUiHarnessConfig fromSystemProperties() {
        return from(System::getProperty);
    }

    static DesktopUiHarnessConfig from(Function<String, String> propertyReader) {
        String artifactsDir = propertyReader.apply(PREFIX + "artifacts.dir");
        if (artifactsDir == null || artifactsDir.isBlank()) {
            artifactsDir = Path.of(System.getProperty("java.io.tmpdir"), "bisq2-ui-harness", "artifacts").toString();
        } else {
            artifactsDir = artifactsDir.trim();
        }

        String token = requireNonBlank(propertyReader.apply(PREFIX + "token"), PREFIX + "token");
        int bindPort = getInt(propertyReader, PREFIX + "bind.port", 18180);
        long fxTimeoutMs = getLong(propertyReader, PREFIX + "fx.timeoutMs", 5000L);
        double windowWidth = getDouble(propertyReader, PREFIX + "window.width", 1440d);
        double windowHeight = getDouble(propertyReader, PREFIX + "window.height", 900d);
        long stageTimeoutMs = getLong(propertyReader, PREFIX + "stage.timeoutMs", 40000L);
        return new DesktopUiHarnessConfig(
                getString(propertyReader, PREFIX + "bind.host", "127.0.0.1"),
                bindPort,
                fxTimeoutMs,
                windowWidth,
                windowHeight,
                token,
                Path.of(artifactsDir),
                stageTimeoutMs
        );
    }

    DesktopUiHarnessConfig {
        bindHost = requireNonBlank(bindHost, PREFIX + "bind.host");
        token = requireNonBlank(token, PREFIX + "token");

        if (bindPort < 1 || bindPort > 65535) {
            throw new IllegalStateException("Invalid system property: " + PREFIX + "bind.port");
        }
        if (fxTimeoutMs <= 0) {
            throw new IllegalStateException("Invalid system property: " + PREFIX + "fx.timeoutMs");
        }
        if (windowWidth <= 0) {
            throw new IllegalStateException("Invalid system property: " + PREFIX + "window.width");
        }
        if (windowHeight <= 0) {
            throw new IllegalStateException("Invalid system property: " + PREFIX + "window.height");
        }
        if (stageTimeoutMs <= 0) {
            throw new IllegalStateException("Invalid system property: " + PREFIX + "stage.timeoutMs");
        }
    }

    DesktopAutomationConfig toAutomationConfig() {
        return new DesktopAutomationConfig(bindHost, bindPort, fxTimeoutMs, windowWidth, windowHeight, token,
                artifactsDir.toString());
    }

    private static String getString(Function<String, String> propertyReader, String key, String defaultValue) {
        String value = propertyReader.apply(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static long getLong(Function<String, String> propertyReader, String key, long defaultValue) {
        String value = propertyReader.apply(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }

    private static int getInt(Function<String, String> propertyReader, String key, int defaultValue) {
        String value = propertyReader.apply(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private static double getDouble(Function<String, String> propertyReader, String key, double defaultValue) {
        String value = propertyReader.apply(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Double.parseDouble(value.trim());
    }

    private static String requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + key);
        }
        return value.trim();
    }
}
