package bisq.application.migration;

import bisq.common.platform.InvalidVersionException;
import bisq.common.platform.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MigrationServiceTests {
    @Test
    void getStoredVersionBeforeMigrationServiceIntroduced(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath);
        Version storedVersion = migrationService.getStoredVersion();
        assertThat(storedVersion)
                .isEqualTo(MigrationService.VERSION_BEFORE_MIGRATION_SERVICE_INTRODUCED);
    }

    @Test
    void getInvalidStoredVersion(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Files.writeString(versionFilePath, "2.1-alpha");

        MigrationService migrationService = new MigrationService(appDataDirPath);
        assertThrows(InvalidVersionException.class, migrationService::getStoredVersion);
    }

    @Test
    void getStoredVersion(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Files.writeString(versionFilePath, "2.1.34");

        MigrationService migrationService = new MigrationService(appDataDirPath);
        Version storedVersion = migrationService.getStoredVersion();
        assertThat(storedVersion)
                .isEqualTo(new Version("2.1.34"));
    }
}
