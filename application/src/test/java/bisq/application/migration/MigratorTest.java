package bisq.application.migration;

import bisq.application.migration.migrations.Migration;
import bisq.application.migration.migrations.MigrationFailedException;
import bisq.common.application.ApplicationVersion;
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
    void migrationSuccess(@TempDir Path dataDir) throws IOException {
        Path versionFilePath = dataDir.resolve("version");
        Version dataDirVersion = new Version("2.1.0");
        Files.writeString(versionFilePath, dataDirVersion.toString());

        Version appVersion = ApplicationVersion.getVersion();
        Migrator migrator = new Migrator(appVersion, dataDir, Collections.emptyList());

        migrator.migrate();

        String readVersion = Files.readString(dataDir.resolve("version"));
        assertThat(readVersion).isEqualTo(appVersion.toString());
    }

    @Test
    void migrationFailure(@TempDir Path dataDir) throws IOException {
        Path versionFilePath = dataDir.resolve("version");
        Version dataDirVersion = new Version("2.1.0");
        Files.writeString(versionFilePath, dataDirVersion.toString());

        Version appVersion = ApplicationVersion.getVersion();
        var migration = new Migration() {
            @Override
            public void run(Path dataDir) {
                throw new MigrationFailedException("Migration failed.");
            }

            @Override
            public Version getVersion() {
                return new Version("2.1.1");
            }
        };

        Migrator migrator = new Migrator(appVersion, dataDir, List.of(migration, migration));
        migrator.migrate();

        String readVersion = Files.readString(dataDir.resolve("version"));
        assertThat(readVersion).isEqualTo(dataDirVersion.toString());
    }
}
