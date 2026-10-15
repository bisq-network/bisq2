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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Thrown if the locking mechanism itself is unusable, e.g. the lock file cannot be created or the
 * file system does not support locks. We cannot tell whether another instance is running, thus we
 * abort instead of risking two instances on the same data directory.
 */
public class InstanceLockUnavailableException extends InstanceLockException {
    public static final String DISABLE_CHECK_OPTION = "--application.checkInstanceLock=false";

    public InstanceLockUnavailableException(String appName, Path appDataDirPath, IOException cause) {
        super("Could not lock the data directory " + appDataDirPath + " of " + appName +
                        ", thus we cannot detect if another instance is running: " + cause.getMessage() +
                        ". Start with " + DISABLE_CHECK_OPTION + " to run without the single instance protection, " +
                        "but only if you are sure that no other instance uses that data directory.",
                cause,
                appName,
                appDataDirPath);
    }
}
