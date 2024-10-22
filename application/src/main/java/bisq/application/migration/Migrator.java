package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.application.migration.migrations.MigrationsForV2_1_2;
import bisq.common.platform.Version;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class Migrator {
    private final Version appVersion;
    private final Path dataDir;
    private final List<Migration> allMigrations;

    public Migrator(Version appVersion, Path dataDir) {
        this.appVersion = appVersion;
        this.dataDir = dataDir;
        this.allMigrations = List.of(new MigrationsForV2_1_2());
    }

    public void migrate() {
        for (Migration migration : allMigrations) {
            Version migrationVersion = migration.getVersion();
            if (migrationVersion.belowOrEqual(appVersion)) {
                log.info("Running {} migrations.", migrationVersion);
                migration.run(dataDir);
            }
        }
    }
}
