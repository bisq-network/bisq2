package bisq.evolution.migration;

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
    void getDataDirVersionBeforeMigrationServiceIntroduced(@TempDir Path dataDir) {
        MigrationService migrationService = new MigrationService(dataDir);
        Version dataDirVersion = migrationService.getDataDirVersion();
        assertThat(dataDirVersion)
                .isEqualTo(MigrationService.VERSION_BEFORE_MIGRATION_SERVICE_INTRODUCED);
    }

    @Test
    void getDataDirInvalidVersion(@TempDir Path dataDir) throws IOException {
        Path versionFilePath = dataDir.resolve("version");
        Files.writeString(versionFilePath, "2.1-alpha");

        MigrationService migrationService = new MigrationService(dataDir);
        assertThrows(InvalidVersionException.class, migrationService::getDataDirVersion);
    }

    @Test
    void getDataDirVersion(@TempDir Path dataDir) throws IOException {
        Path versionFilePath = dataDir.resolve("version");
        Files.writeString(versionFilePath, "2.1.34");

        MigrationService migrationService = new MigrationService(dataDir);
        Version dataDirVersion = migrationService.getDataDirVersion();
        assertThat(dataDirVersion)
                .isEqualTo(new Version("2.1.34"));
    }
}
