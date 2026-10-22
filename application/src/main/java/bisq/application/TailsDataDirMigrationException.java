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
 * Thrown at startup if the data directory from an earlier version could not be copied to Persistent
 * Storage on Tails. We must not start, as a new data directory would get a new identity and would
 * prevent the migration from being retried. The {@link Executable} handles it and terminates the application.
 */
@Getter
public class TailsDataDirMigrationException extends RuntimeException {
    private final Path legacyDataDirPath;
    private final Path appDataDirPath;

    public TailsDataDirMigrationException(Path legacyDataDirPath, Path appDataDirPath, Throwable cause) {
        super("Could not copy the data directory " + legacyDataDirPath + " to " + appDataDirPath + ": " +
                        cause.getMessage() + ". The original was left in place. Fix the cause and start again.",
                cause);
        this.legacyDataDirPath = legacyDataDirPath;
        this.appDataDirPath = appDataDirPath;
    }
}
