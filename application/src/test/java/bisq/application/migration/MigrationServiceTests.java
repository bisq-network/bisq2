package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.application.migration.migrations.MigrationFailedException;
import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileMutatorUtils;
import bisq.common.platform.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MigrationServiceTests {
    @Test
    void getStoredVersionBeforeMigrationServiceIntroduced(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath);
        Version storedVersion = migrationService.getStoredVersion();
        assertThat(storedVersion)
                .isEqualTo(MigrationService.VERSION_WITHOUT_VERSION_FILE);
    }

    @Test
    void getInvalidStoredVersion(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        FileMutatorUtils.writeToPath("2.1-alpha", versionFilePath);

        MigrationService migrationService = new MigrationService(appDataDirPath);
        Version storedVersion = migrationService.getStoredVersion();
        assertThat(storedVersion)
                .isEqualTo(MigrationService.VERSION_WITHOUT_VERSION_FILE);
    }

    @Test
    void getUnreadableStoredVersion(@TempDir Path appDataDirPath) throws IOException {
        // A directory at the path of the version file cannot be read as a file
        Files.createDirectory(appDataDirPath.resolve("version"));

        MigrationService migrationService = new MigrationService(appDataDirPath);
        Version storedVersion = migrationService.getStoredVersion();
        assertThat(storedVersion)
                .isEqualTo(MigrationService.VERSION_WITHOUT_VERSION_FILE);
    }

    @Test
    void getStoredVersion(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        FileMutatorUtils.writeToPath("2.1.34", versionFilePath);

        MigrationService migrationService = new MigrationService(appDataDirPath);
        Version storedVersion = migrationService.getStoredVersion();
        assertThat(storedVersion)
                .isEqualTo(new Version("2.1.34"));
    }

    @Test
    void migrateWritesAppVersionWithoutVersionFile(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath);
        migrationService.migrate(MigrationListener.NONE, false);

        assertThat(migrationService.getStoredVersion())
                .isEqualTo(ApplicationVersion.getVersion());
    }

    @Test
    void migrateReplacesInvalidVersionFile(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        FileMutatorUtils.writeToPath("garbage", versionFilePath);

        MigrationService migrationService = new MigrationService(appDataDirPath);
        migrationService.migrate(MigrationListener.NONE, false);

        assertThat(migrationService.getStoredVersion())
                .isEqualTo(ApplicationVersion.getVersion());
    }

    @Test
    void failedMigrationStopsTheStart(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath, List.of(failingMigration()));

        IncompleteMigrationException exception = assertThrows(IncompleteMigrationException.class,
                () -> migrationService.migrate(MigrationListener.NONE, false));
        assertThat(exception.getMessage())
                .contains("2.1.2")
                .contains("-Dapplication.startAfterFailedMigration=true");
        assertThat(appDataDirPath.resolve("version")).doesNotExist();
    }

    @Test
    void noMigrationRunsIfStoredVersionIsNotBelowAppVersion(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        FileMutatorUtils.writeToPath(ApplicationVersion.getVersion().toString(), versionFilePath);
        MigrationService migrationService = new MigrationService(appDataDirPath, List.of(failingMigration()));
        List<Version> askedVersions = new ArrayList<>();

        assertDoesNotThrow(() -> migrationService.migrate(answeringListener(false, askedVersions), false));
        assertThat(askedVersions).isEmpty();
        assertThat(migrationService.getStoredVersion())
                .isEqualTo(ApplicationVersion.getVersion());
    }

    @Test
    void startAfterFailedMigrationOptionStartsWithoutAskingTheListener(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath, List.of(failingMigration()));
        List<Version> askedVersions = new ArrayList<>();

        assertDoesNotThrow(() -> migrationService.migrate(answeringListener(false, askedVersions), true));
        assertThat(askedVersions).isEmpty();
        assertThat(appDataDirPath.resolve("version")).doesNotExist();
    }

    @Test
    void listenerCanStartAfterFailedMigration(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath, List.of(failingMigration()));
        List<Version> askedVersions = new ArrayList<>();

        assertDoesNotThrow(() -> migrationService.migrate(answeringListener(true, askedVersions), false));
        assertThat(askedVersions).containsExactly(new Version("2.1.2"));
        assertThat(appDataDirPath.resolve("version")).doesNotExist();
    }

    @Test
    void listenerCanStopAfterFailedMigration(@TempDir Path appDataDirPath) {
        MigrationService migrationService = new MigrationService(appDataDirPath, List.of(failingMigration()));
        List<Version> askedVersions = new ArrayList<>();

        assertThrows(IncompleteMigrationException.class,
                () -> migrationService.migrate(answeringListener(false, askedVersions), false));
        assertThat(askedVersions).containsExactly(new Version("2.1.2"));
    }

    // Above the version which counts as stored without a version file, and below the app version
    private static Migration failingMigration() {
        return new Migration() {
            @Override
            public void run(Path appDataDirPath) {
                throw new MigrationFailedException("Migration failed.");
            }

            @Override
            public Version getVersion() {
                return new Version("2.1.2");
            }
        };
    }

    private static MigrationListener answeringListener(boolean startAnyway, List<Version> askedVersions) {
        return new MigrationListener() {
            @Override
            public boolean startAfterFailedMigration(Version failedVersion, Path appDataDirPath) {
                askedVersions.add(failedVersion);
                return startAnyway;
            }
        };
    }
}
