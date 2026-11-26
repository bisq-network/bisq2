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

package bisq.application;

import lombok.Getter;

import java.nio.file.Path;

/**
 * Thrown if bisq.conf exists but cannot be read, parsed or checked. The start fails closed: bisq.conf can hold settings
 * which protect the user, such as Tor bridges or the torrc override ExcludeNodes, and without them the application
 * would connect in a way which the user did not choose.
 */
@Getter
public class CustomConfigException extends RuntimeException {
    private final Path customConfigFilePath;
    private final String reason;

    public CustomConfigException(Path customConfigFilePath, Throwable cause) {
        super("Cannot read the configuration file " + customConfigFilePath + ": " + cause.getMessage() +
                ". The application does not start without the settings of this file. Correct the file or move it " +
                "away.", cause);
        this.customConfigFilePath = customConfigFilePath;
        this.reason = cause.getMessage();
    }
}
