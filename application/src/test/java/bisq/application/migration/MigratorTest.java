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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MigratorTest {
    @Test
    void migrationSuccess(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve("version");
        Version storedVersion = new Version("2.1.0");
        FileMutatorUtils.writeToPath(storedVersion.toString(), versionFilePath);

        Version appVersion = ApplicationVersion.getVersion();
        Migrator migrator = new Migrator(appVersion, appDataDirPath, Collections.emptyList());

        migrator.migrate();

        String readVersion = Files.readString(appDataDirPath.resolve("version"));
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

        Migrator migrator = new Migrator(appVersion, appDataDirPath, List.of(migration, migration));
        migrator.migrate();

        String readVersion = Files.readString(appDataDirPath.resolve("version"));
        assertThat(readVersion).isEqualTo(storedVersion.toString());
    }
}
