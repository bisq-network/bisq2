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

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Thrown at startup if we cannot establish that we are the only instance using the application data
 * directory. The {@link Executable} handles it and terminates the application. We fail closed as
 * two instances on the same data directory corrupt the persisted state and the wallet.
 */
@Getter
public abstract class InstanceLockException extends RuntimeException {
    private final String appName;
    private final Path appDataDirPath;

    protected InstanceLockException(String message,
                                    @Nullable Throwable cause,
                                    String appName,
                                    Path appDataDirPath) {
        super(message, cause);
        this.appName = appName;
        this.appDataDirPath = appDataDirPath;
    }
}
