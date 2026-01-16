package bisq.application.migration.migrations;

import bisq.common.file.FileMutatorUtils;
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
    void migrateEmptyDataDir(@TempDir Path appDataDirPath) {
        Version version = migrationsForV212.getVersion();
        Version expectedVersion = new Version("2.1.2");
        assertThat(version).isEqualTo(expectedVersion);

        migrationsForV212.run(appDataDirPath);
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.WINDOWS})
    void torFilesRemovalTestOnMacAndWindows(@TempDir Path appDataDirPath) throws IOException {
        Path torPath = appDataDirPath.resolve("tor");
        createFakeTorPath(torPath);

        migrationsForV212.run(appDataDirPath);

        assertThat(torPath).exists();
        assertThat(torPath.resolve("libevent-2.1.so.7")).exists();

        Path pluggableTransportDirPath = torPath.resolve("pluggable_transports");
        assertThat(pluggableTransportDirPath).exists();

        assertThat(pluggableTransportDirPath.resolve("README.SNOWFLAKE.md")).exists();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void torFilesRemovalTestOnLinux(@TempDir Path appDataDirPath) throws IOException {
        Path torPath = appDataDirPath.resolve("tor");
        createFakeTorPath(torPath);

        migrationsForV212.run(appDataDirPath);

        assertThat(torPath).doesNotExist();
        assertThat(torPath.resolve("libevent-2.1.so.7")).doesNotExist();

        Path pluggableTransportDirPath = torPath.resolve("pluggable_transports");
        assertThat(pluggableTransportDirPath).doesNotExist();

        assertThat(pluggableTransportDirPath.resolve("README.SNOWFLAKE.md")).doesNotExist();
    }

    private void createFakeTorPath(Path torPath) throws IOException {
        Files.createDirectories(torPath);

        List<String> torPathFiles = List.of("lock",
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
        createFiles(torPath, torPathFiles);

        Path pluggableTransportDirPath = torPath.resolve("pluggable_transports");
        FileMutatorUtils.createDirectories(pluggableTransportDirPath);

        List<String> pluggableTransportFiles = List.of("snowflake-client",
                "bridges_list.snowflake.txt",
                "bridges_list.obfs4.txt",
                "README.SNOWFLAKE.md",
                "obfs4proxy",
                "pt_config.json",
                "bridges_list.meek-azure.txt");
        createFiles(pluggableTransportDirPath, pluggableTransportFiles);

        assertThat(torPath.resolve("libevent-2.1.so.7")).exists();

        assertThat(pluggableTransportDirPath.resolve("README.SNOWFLAKE.md")).exists();
    }

    private void createFiles(Path basePath, List<String> fileNames) {
        fileNames.forEach(filename -> {
            try {
                Path filePath = basePath.resolve(filename);
                FileMutatorUtils.writeToPath("ABC", filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
