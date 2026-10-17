package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.application.migration.migrations.MigrationFailedException;
import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.common.platform.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MigratorTest {
    @Test
    void migrationSuccess(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Version storedVersion = new Version("2.1.0");
        FileMutatorUtils.writeToPath(storedVersion.toString(), versionFilePath);

        Version appVersion = ApplicationVersion.getVersion();
        Migrator migrator = new Migrator(storedVersion, appVersion, appDataDirPath, Collections.emptyList());

        assertThat(migrator.migrate(MigrationListener.NONE)).isEmpty();

        String readVersion = FileReaderUtils.readUTF8String(appDataDirPath.resolve("version"));
        assertThat(readVersion).isEqualTo(appVersion.toString());
    }

    @Test
    void migrationFailure(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Version storedVersion = new Version("2.1.0");
        FileMutatorUtils.writeToPath(storedVersion.toString(), versionFilePath);

        Version appVersion = ApplicationVersion.getVersion();
        var migration = new Migration() {
            @Override
            public void run(Path appDataDirPath) {
                throw new MigrationFailedException("Migration failed.");
            }

            @Override
            public Version getVersion() {
                return new Version("2.1.1");
            }
        };

        Migrator migrator = new Migrator(storedVersion, appVersion, appDataDirPath, List.of(migration));
        assertThat(migrator.migrate(MigrationListener.NONE)).contains(new Version("2.1.1"));

        String readVersion = FileReaderUtils.readUTF8String(appDataDirPath.resolve("version"));
        assertThat(readVersion).isEqualTo(storedVersion.toString());
    }

    @Test
    void failedWriteKeepsStoredVersionWithoutThrowing(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Version storedVersion = new Version("2.1.0");
        FileMutatorUtils.writeToPath(storedVersion.toString(), versionFilePath);
        // A directory at the path of the temporary file of FileMutatorUtils.writeToPathAtomically makes the write fail
        Files.createDirectory(appDataDirPath.resolve("version.tmp"));

        Version appVersion = ApplicationVersion.getVersion();
        Migrator migrator = new Migrator(storedVersion, appVersion, appDataDirPath, Collections.emptyList());
        assertDoesNotThrow(() -> migrator.migrate(MigrationListener.NONE));

        String readVersion = FileReaderUtils.readUTF8String(versionFilePath);
        assertThat(readVersion).isEqualTo(storedVersion.toString());
    }

    @Test
    void unexpectedExceptionIsAFailedMigration(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Version storedVersion = new Version("2.1.0");
        FileMutatorUtils.writeToPath(storedVersion.toString(), versionFilePath);

        Version appVersion = ApplicationVersion.getVersion();
        var migration = new Migration() {
            @Override
            public void run(Path appDataDirPath) {
                throw new IllegalStateException("A bug in the migration");
            }

            @Override
            public Version getVersion() {
                return new Version("2.1.1");
            }
        };

        Migrator migrator = new Migrator(storedVersion, appVersion, appDataDirPath, List.of(migration));
        assertThat(migrator.migrate(MigrationListener.NONE)).contains(new Version("2.1.1"));

        String readVersion = FileReaderUtils.readUTF8String(versionFilePath);
        assertThat(readVersion).isEqualTo(storedVersion.toString());
    }

    @Test
    void onlyMigrationsAboveStoredVersionAndUpToAppVersionRun(@TempDir Path appDataDirPath) throws IOException {
        Version storedVersion = new Version("2.1.5");
        Version appVersion = new Version("2.1.8");
        List<String> ranMigrations = new ArrayList<>();
        List<Migration> allMigrations = Stream.of("2.1.4", "2.1.5", "2.1.6", "2.1.8", "2.1.9")
                .map(version -> recordingMigration(version, ranMigrations))
                .toList();

        Migrator migrator = new Migrator(storedVersion, appVersion, appDataDirPath, allMigrations);
        assertThat(migrator.migrate(MigrationListener.NONE)).isEmpty();

        assertThat(ranMigrations).containsExactly("2.1.6", "2.1.8");
        String readVersion = FileReaderUtils.readUTF8String(appDataDirPath.resolve("version"));
        assertThat(readVersion).isEqualTo(appVersion.toString());
    }

    @Test
    void pendingMigrationsRunInTheOrderOfTheirVersions(@TempDir Path appDataDirPath) {
        List<String> ranMigrations = new ArrayList<>();
        List<Migration> allMigrations = Stream.of("2.1.8", "2.1.6", "2.1.7")
                .map(version -> recordingMigration(version, ranMigrations))
                .toList();

        Migrator migrator = new Migrator(new Version("2.1.5"), new Version("2.1.8"), appDataDirPath, allMigrations);
        migrator.migrate(MigrationListener.NONE);

        assertThat(ranMigrations).containsExactly("2.1.6", "2.1.7", "2.1.8");
    }

    @Test
    void laterMigrationsDoNotRunAfterAFailedOne(@TempDir Path appDataDirPath) {
        List<String> events = new ArrayList<>();
        Migration failingMigration = new Migration() {
            @Override
            public void run(Path appDataDirPath) {
                events.add("2.1.6");
                throw new MigrationFailedException("Migration failed.");
            }

            @Override
            public Version getVersion() {
                return new Version("2.1.6");
            }
        };
        Migrator migrator = new Migrator(new Version("2.1.5"), new Version("2.1.8"), appDataDirPath,
                List.of(failingMigration, recordingMigration("2.1.7", events)));

        assertThat(migrator.migrate(recordingListener(events))).contains(new Version("2.1.6"));
        // No "completed", as a migration failed
        assertThat(events).containsExactly("started", "2.1.6");
        assertThat(appDataDirPath.resolve("version")).doesNotExist();
    }

    @Test
    void listenerIsCalledAroundSuccessfulMigrations(@TempDir Path appDataDirPath) {
        List<String> events = new ArrayList<>();
        Migrator migrator = new Migrator(new Version("2.1.5"), new Version("2.1.8"), appDataDirPath,
                List.of(recordingMigration("2.1.6", events), recordingMigration("2.1.7", events)));

        assertThat(migrator.migrate(recordingListener(events))).isEmpty();

        assertThat(events).containsExactly("started", "2.1.6", "2.1.7", "completed");
    }

    @Test
    void listenerIsNotCalledWithoutPendingMigrations(@TempDir Path appDataDirPath) {
        List<String> events = new ArrayList<>();
        // An upgrade from 2.1.5 to 2.1.8 with migrations only outside of that range
        Migrator migrator = new Migrator(new Version("2.1.5"), new Version("2.1.8"), appDataDirPath,
                List.of(recordingMigration("2.1.4", events), recordingMigration("2.1.9", events)));

        assertThat(migrator.migrate(recordingListener(events))).isEmpty();

        assertThat(events).isEmpty();
        assertThat(appDataDirPath.resolve("version")).exists();
    }

    private static MigrationListener recordingListener(List<String> events) {
        return new MigrationListener() {
            @Override
            public void onMigrationsStarted() {
                events.add("started");
            }

            @Override
            public void onMigrationsCompleted() {
                events.add("completed");
            }
        };
    }

    private static Migration recordingMigration(String version, List<String> ranMigrations) {
        return new Migration() {
            @Override
            public void run(Path appDataDirPath) {
                ranMigrations.add(version);
            }

            @Override
            public Version getVersion() {
                return new Version(version);
            }
        };
    }
}
