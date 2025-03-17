package bisq.application.migration;

import bisq.common.application.ApplicationVersion;
import bisq.common.platform.Version;
import bisq.application.migration.migrations.Migration;
import bisq.application.migration.migrations.MigrationFailedException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class Migrator {
    private final Version appVersion;
    private final Path dataDir;
    private final List<Migration> allMigrations;

    public Migrator(Version appVersion, Path dataDir, List<Migration> allMigrations) {
        this.appVersion = appVersion;
        this.dataDir = dataDir;
        this.allMigrations = allMigrations;
    }

    public void migrate() {
        boolean allMigrationsSucceeded = true;

        for (Migration migration : allMigrations) {
            Version migrationVersion = migration.getVersion();
            if (migrationVersion.belowOrEqual(appVersion)) {
                log.info("Running {} migrations.", migrationVersion);

                try {
                    migration.run(dataDir);
                } catch (MigrationFailedException e) {
                    log.error("Migration failed.", e);
                    allMigrationsSucceeded = false;
                }
            }
        }

        if (allMigrationsSucceeded) {
            try {
                Path versionFilePath = dataDir.resolve("version");
                Files.writeString(versionFilePath, ApplicationVersion.getVersion().toString());
            } catch (IOException e) {
                throw new MigrationFailedException(e);
            }
        }
    }
}
