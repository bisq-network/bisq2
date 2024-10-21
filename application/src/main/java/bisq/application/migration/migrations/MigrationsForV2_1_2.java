package bisq.application.migration.migrations;

import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileUtils;
import bisq.common.platform.OS;
import bisq.common.platform.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MigrationsForV2_1_2 implements Migration {

    @Override
    public void run(Path dataDir) {
        try {
            if (OS.isLinux()) {
                Path torDataDir = dataDir.resolve("tor");
                FileUtils.deleteFileOrDirectory(torDataDir);
            }

            Path versionFilePath = dataDir.resolve("version");
            Files.writeString(versionFilePath, ApplicationVersion.getVersion().toString());
        } catch (IOException e) {
            throw new MigrationFailedException(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.1.2");
    }
}
