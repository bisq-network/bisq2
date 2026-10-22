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


package bisq.application.migration;

import bisq.common.platform.Version;

import java.nio.file.Path;

/**
 * Lets an app show the migrations of the data directory to the user, and lets the user decide whether the app starts
 * after a failed migration. The ApplicationService constructor runs the migrations before the app has a UI of its own.
 * It calls the listener on the thread which constructs the ApplicationService, and waits until each call returns.
 */
public interface MigrationListener {
    MigrationListener NONE = new MigrationListener() {
    };

    /**
     * Called before the first migration runs. Not called at a start at which no migration runs.
     */
    default void onMigrationsStarted() {
    }

    /**
     * Called after all pending migrations succeeded, if {@link #onMigrationsStarted()} was called. The app continues its
     * start when this method returns.
     */
    default void onMigrationsCompleted() {
    }

    /**
     * Called when a migration failed, unless the configuration option startAfterFailedMigration lets the app start
     * anyway. The migrations after the failed one did not run, and the version is not recorded, so the migrations run
     * again at the next start.
     * <p>
     * The default stops the start, because the app can misbehave or damage data on a data directory which is not fully
     * migrated.
     *
     * @return true to start the app anyway, false to stop the start
     */
    default boolean startAfterFailedMigration(Version failedVersion, Path appDataDirPath) {
        return false;
    }
}
