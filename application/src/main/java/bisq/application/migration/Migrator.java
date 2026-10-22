package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.common.file.FileMutatorUtils;
import bisq.common.platform.Version;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Runs the migrations above the stored version and up to the app version, in the order of their versions, and records
 * the app version if all of them succeed. It stops at the first failed migration. A migration can run more than once on
 * the same data directory, so every migration must be idempotent (see {@link Migration#run(Path)}).
 */
@Slf4j
class Migrator {
    private final Version storedVersion;
    private final Version appVersion;
    private final Path appDataDirPath;
    private final List<Migration> allMigrations;

    Migrator(Version storedVersion, Version appVersion, Path appDataDirPath, List<Migration> allMigrations) {
        this.storedVersion = storedVersion;
        this.appVersion = appVersion;
        this.appDataDirPath = appDataDirPath;
        this.allMigrations = allMigrations;
    }

    /**
     * @return The version of the failed migration, if one failed. The migrations after it did not run.
     */
    Optional<Version> migrate(MigrationListener listener) {
        // A migration at or below the stored version ran at an earlier start
        List<Migration> pendingMigrations = allMigrations.stream()
                .filter(migration -> storedVersion.below(migration.getVersion()) &&
                        migration.getVersion().belowOrEqual(appVersion))
                .sorted(Comparator.comparing(Migration::getVersion))
                .toList();
        if (!pendingMigrations.isEmpty()) {
            listener.onMigrationsStarted();
        }

        for (Migration migration : pendingMigrations) {
            Version migrationVersion = migration.getVersion();
            log.info("Running {} migrations.", migrationVersion);

            try {
                migration.run(appDataDirPath);
            } catch (RuntimeException e) {
                // An unexpected exception is a failed migration as well. The later migrations can depend on this one,
                // so they do not run.
                log.error("Migration {} failed.", migrationVersion, e);
                return Optional.of(migrationVersion);
            }
        }

        // A version which is not recorded makes the migrations run again at the next start, which is safe as they are
        // idempotent. So a failed write does not stop the start of the app.
        try {
            // MigrationService treats an unreadable version file as a missing one, which runs all migrations again.
            // So a crash or a full disk must not leave a partial file.
            FileMutatorUtils.writeToPathAtomically(appVersion.toString(), appDataDirPath.resolve("version"));
        } catch (IOException e) {
            log.error("Could not write the version file. The migrations run again at the next start.", e);
        }
        if (!pendingMigrations.isEmpty()) {
            listener.onMigrationsCompleted();
        }
        return Optional.empty();
    }
}
