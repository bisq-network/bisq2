package bisq.evolution.migration.migrations;

import bisq.common.platform.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationsForV2_1_2Tests {
    private final MigrationsForV2_1_2 migrationsForV212 = new MigrationsForV2_1_2();

    @Test
    void migrateEmptyDataDir(@TempDir Path dataDir) {
        Version version = migrationsForV212.getVersion();
        Version expectedVersion = new Version("2.1.2");
        assertThat(version).isEqualTo(expectedVersion);

        migrationsForV212.run(dataDir);
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.WINDOWS})
    void torFilesRemovalTestOnMacAndWindows(@TempDir Path dataDir) {
        Path torDataDir = dataDir.resolve("tor");
        createFakeTorDataDir(torDataDir);

        migrationsForV212.run(dataDir);

        assertThat(torDataDir).exists();
        boolean fileExists = torDataDir.resolve("libevent-2.1.so.7").toFile().exists();
        assertThat(fileExists).isTrue();

        Path pluggableTransportDir = torDataDir.resolve("pluggable_transports");
        assertThat(pluggableTransportDir).exists();

        fileExists = pluggableTransportDir.resolve("README.SNOWFLAKE.md").toFile().exists();
        assertThat(fileExists).isTrue();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void torFilesRemovalTestOnLinux(@TempDir Path dataDir) {
        Path torDataDir = dataDir.resolve("tor");
        createFakeTorDataDir(torDataDir);

        migrationsForV212.run(dataDir);

        assertThat(torDataDir).doesNotExist();
        boolean fileExists = torDataDir.resolve("libevent-2.1.so.7").toFile().exists();
        assertThat(fileExists).isFalse();

        Path pluggableTransportDir = torDataDir.resolve("pluggable_transports");
        assertThat(pluggableTransportDir).doesNotExist();

        fileExists = pluggableTransportDir.resolve("README.SNOWFLAKE.md").toFile().exists();
        assertThat(fileExists).isFalse();
    }

    private void createFakeTorDataDir(Path torDataDir) {
        boolean isSuccess = torDataDir.toFile().mkdirs();
        assertThat(isSuccess).isTrue();

        List<String> torDirFiles = List.of("lock",
                "libssl.so.1.1",
                "libstdc++.so.6",
                "cached-certs",
                "cached-microdesc-consensus",
                "state",
                "debug.log",
                "libevent-2.1.so.7",
                "torrc",
                "tor",
                "cached-microdescs.new",
                "geoip",
                "version",
                "libcrypto.so.1.1",
                "keys",
                "geoip6");
        createFiles(torDataDir, torDirFiles);

        Path pluggableTransportDir = torDataDir.resolve("pluggable_transports");
        isSuccess = pluggableTransportDir.toFile().mkdirs();
        assertThat(isSuccess).isTrue();

        List<String> pluggableTransportFiles = List.of("snowflake-client",
                "bridges_list.snowflake.txt",
                "bridges_list.obfs4.txt",
                "README.SNOWFLAKE.md",
                "obfs4proxy",
                "pt_config.json",
                "bridges_list.meek-azure.txt");
        createFiles(pluggableTransportDir, pluggableTransportFiles);

        boolean fileExists = torDataDir.resolve("libevent-2.1.so.7").toFile().exists();
        assertThat(fileExists).isTrue();

        fileExists = pluggableTransportDir.resolve("README.SNOWFLAKE.md").toFile().exists();
        assertThat(fileExists).isTrue();
    }

    private void createFiles(Path basePath, List<String> fileNames) {
        fileNames.forEach(filename -> {
            try {
                Path filePath = basePath.resolve(filename);
                Files.writeString(filePath, "ABC");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
