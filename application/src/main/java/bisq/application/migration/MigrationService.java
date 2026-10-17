package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileReaderUtils;
import bisq.common.platform.InvalidVersionException;
import bisq.common.platform.Version;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MigrationService {
    // The stored version when the version file is missing or cannot be read. 2.1.1 is the last version before
    // MigrationService existed, so all migrations run.
    static final Version VERSION_WITHOUT_VERSION_FILE = new Version("2.1.1");
    // Each migration must be idempotent, as it can run more than once (see Migration.run())
    private static final List<Migration> ALL_MIGRATIONS = List.of();

    private final Path appDataDirPath;
    private final Path versionFilePath;
    private final List<Migration> allMigrations;

    public MigrationService(Path appDataDirPath) {
        this(appDataDirPath, ALL_MIGRATIONS);
    }

    @VisibleForTesting
    MigrationService(Path appDataDirPath, List<Migration> allMigrations) {
        this.appDataDirPath = appDataDirPath;
        this.versionFilePath = appDataDirPath.resolve("version");
        this.allMigrations = allMigrations;
    }

    /**
     * Runs the pending migrations. After a failed migration, the app starts only if startAfterFailedMigration is set or
     * the listener lets it start.
     *
     * @throws IncompleteMigrationException if a migration failed and the app must not start
     */
    public void migrate(MigrationListener listener, boolean startAfterFailedMigration) {
        Version storedVersion = getStoredVersion();
        Version appVersion = ApplicationVersion.getVersion();
        if (!storedVersion.below(appVersion)) {
            return;
        }

        Migrator migrator = new Migrator(storedVersion, appVersion, appDataDirPath, allMigrations);
        Optional<Version> failedVersion = migrator.migrate(listener);
        if (failedVersion.isEmpty()) {
            log.info("Migrated the data directory from version {} to {}", storedVersion, appVersion);
            return;
        }
        if (startAfterFailedMigration) {
            log.warn("Migration {} failed. The app starts anyway, because startAfterFailedMigration is set.",
                    failedVersion.get());
        } else if (listener.startAfterFailedMigration(failedVersion.get(), appDataDirPath)) {
            log.warn("Migration {} failed. The app starts anyway, because the user chose it.", failedVersion.get());
        } else {
            throw new IncompleteMigrationException("The migration " + failedVersion.get() + " of the data directory " +
                    appDataDirPath + " failed, so the app does not start. Starting on data which is not fully " +
                    "migrated can damage it. The cause is in the log file " + appDataDirPath.resolve("bisq.log") +
                    ". The migrations run again at the next start. To start anyway once, add the JVM option " +
                    "-Dapplication.startAfterFailedMigration=true, for example to JAVA_OPTS. Remove it after that " +
                    "start: while it is set, the app also starts after later failed migrations without asking.");
        }
    }

    // An unreadable version file is treated like a missing one, so all migrations run again. This is safe because the
    // migrations are idempotent. Throwing instead would stop every start of the app until the user deletes the file.
    Version getStoredVersion() {
        if (!Files.exists(versionFilePath)) {
            return VERSION_WITHOUT_VERSION_FILE;
        }

        try {
            String version = FileReaderUtils.readUTF8String(versionFilePath);
            return new Version(version);
        } catch (IOException | InvalidVersionException e) {
            log.warn("Could not read the version file {}. All migrations run again.", versionFilePath, e);
            return VERSION_WITHOUT_VERSION_FILE;
        }
    }
}
