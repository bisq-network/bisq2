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

import java.util.Objects;

public record DesktopAutomationConfig(String bindHost,
                                      int bindPort,
                                      long fxTimeoutMs,
                                      double defaultWidth,
                                      double defaultHeight,
                                      String token,
                                      String artifactsDir) {
    public DesktopAutomationConfig {
        bindHost = requireNonBlank(bindHost, "bindHost");
        token = requireNonBlank(token, "token");
        artifactsDir = requireNonBlank(artifactsDir, "artifactsDir");

        if (bindPort < 1 || bindPort > 65535) {
            throw new IllegalArgumentException("bindPort must be in range 1..65535");
        }
        if (fxTimeoutMs <= 0) {
            throw new IllegalArgumentException("fxTimeoutMs must be > 0");
        }
        if (defaultWidth <= 0) {
            throw new IllegalArgumentException("defaultWidth must be > 0");
        }
        if (defaultHeight <= 0) {
            throw new IllegalArgumentException("defaultHeight must be > 0");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        String trimmedValue = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmedValue;
    }
}
