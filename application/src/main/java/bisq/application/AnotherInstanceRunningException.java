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
import java.util.Optional;

/**
 * Thrown at startup if another instance already uses the same application data directory.
 * The {@link Executable} handles it and terminates the application.
 */
@Getter
public class AnotherInstanceRunningException extends InstanceLockException {
    private final Optional<Long> ownerPid;

    public AnotherInstanceRunningException(String appName, Path appDataDirPath, Optional<Long> ownerPid) {
        super("Another instance of " + appName + " is already running" +
                        ownerPid.map(pid -> " (PID " + pid + ")").orElse("") +
                        " using the data directory " + appDataDirPath,
                null,
                appName,
                appDataDirPath);
        this.ownerPid = ownerPid;
    }
}
