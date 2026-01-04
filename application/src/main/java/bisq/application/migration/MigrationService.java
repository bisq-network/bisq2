package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.application.migration.migrations.MigrationsForV2_1_2;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.file.FileReaderUtils;
import bisq.common.platform.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MigrationService implements Service {
    static final Version VERSION_BEFORE_MIGRATION_SERVICE_INTRODUCED = new Version("2.1.1");
    private final Path appDataDirPath;
    private final Path versionFilePath;

    public MigrationService(Path appDataDirPath) {
        this.appDataDirPath = appDataDirPath;
        this.versionFilePath = appDataDirPath.resolve("version");
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        Version storedVersion = getStoredVersion();
        Version appVersion = ApplicationVersion.getVersion();

        if (storedVersion.below(appVersion)) {
            List<Migration> allMigrations = List.of(new MigrationsForV2_1_2());
            Migrator migrator = new Migrator(appVersion, appDataDirPath, allMigrations);
            migrator.migrate();
        }

        return CompletableFuture.completedFuture(true);
    }

    Version getStoredVersion() {
        if (!Files.exists(versionFilePath)) {
            return VERSION_BEFORE_MIGRATION_SERVICE_INTRODUCED;
        }

        try {
            String version = FileReaderUtils.readUTF8String(versionFilePath);
            return new Version(version);
        } catch (IOException e) {
            throw new RuntimeException("Can't identify stored version. This shouldn't happen.", e);
        }
    }
}
