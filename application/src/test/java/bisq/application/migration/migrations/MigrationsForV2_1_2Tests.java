package bisq.application.migration.migrations;

import bisq.common.application.ApplicationVersion;
import bisq.common.platform.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationsForV2_1_2Tests {
    private final MigrationsForV2_1_2 migrationsForV212 = new MigrationsForV2_1_2();

    @Test
    void migrationTest(@TempDir Path dataDir) throws IOException {
        Version version = migrationsForV212.getVersion();
        Version expectedVersion = new Version("2.1.2");
        assertThat(version).isEqualTo(expectedVersion);

        migrationsForV212.run(dataDir);
        String readVersion = Files.readString(dataDir.resolve("version"));
        assertThat(readVersion)
                .isEqualTo(ApplicationVersion.getVersion().toString());
    }
}
