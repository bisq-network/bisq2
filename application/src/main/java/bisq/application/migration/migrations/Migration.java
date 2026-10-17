package bisq.application.migration.migrations;

import bisq.common.platform.Version;

import java.nio.file.Path;

public interface Migration {
    /**
     * Migrates the data directory.
     * <p>
     * A migration throws only if the app must not run without it, for example when it converts data which the new
     * version needs in the new form. It reports that with a {@link MigrationFailedException}; any other runtime
     * exception counts as such a failure as well. Then the later migrations do not run, the version is not recorded,
     * and the app does not start, unless the user or the option startAfterFailedMigration lets it start anyway. A step
     * which the app can do without, such as removing old files, catches and logs its own errors instead of throwing.
     * <p>
     * A migration MUST be idempotent, and it MUST reach the migrated state from every state which an earlier run or the
     * app can leave:
     * <ul>
     *     <li>A data directory which it already migrated. It runs again at the next start if another migration of the
     *     same start failed or the version file could not be written, and at any start at which the version file cannot
     *     be read or parsed.</li>
     *     <li>A data directory which it migrated only partly, because the app stopped or crashed during the migration,
     *     for example on a power failure. Write new files through a temporary file and a rename, and delete old data
     *     last.</li>
     *     <li>A new, empty data directory. Without a version file, all migrations run, also at the first start of a new
     *     installation.</li>
     *     <li>Data which the new version wrote, because the user started the app anyway after a failed migration.</li>
     * </ul>
     * See docs/specifications/persistence/data-directory-migrations.md.
     */
    void run(Path appDataDirPath);

    /**
     * The version of the first release which contains this migration. The migration runs on a data directory with a
     * lower stored version. A migration with the version of a published release never runs for users who already
     * started that release, so a new migration gets the version of the next release.
     */
    Version getVersion();
}
