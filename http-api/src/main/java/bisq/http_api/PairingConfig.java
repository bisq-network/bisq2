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

package bisq.http_api;

import lombok.Getter;

@Getter
public class PairingConfig {
    private final boolean enabled;
    private final boolean writePairingQrCodeToDisk;
    private final int sessionTtlInMinutes;

    public PairingConfig(boolean enabled, boolean writePairingQrCodeToDisk, int sessionTtlInMinutes) {
        this.enabled = enabled;
        this.writePairingQrCodeToDisk = writePairingQrCodeToDisk;
        this.sessionTtlInMinutes = sessionTtlInMinutes;
    }

    public static PairingConfig from(com.typesafe.config.Config config) {
        int sessionTtlInMinutes = config.hasPath("sessionTtlInMinutes") ? config.getInt("sessionTtlInMinutes") : 60;
        if (sessionTtlInMinutes <= 0) {
            throw new IllegalArgumentException("sessionTtlInMinutes must be > 0, got: " + sessionTtlInMinutes);
        }
        return new PairingConfig(
                config.getBoolean("enabled"),
                config.getBoolean("writePairingQrCodeToDisk"),
                sessionTtlInMinutes
        );
    }
}

