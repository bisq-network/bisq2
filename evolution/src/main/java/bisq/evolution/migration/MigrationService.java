package bisq.evolution.migration;

import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.platform.Version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class MigrationService implements Service {
    static final Version VERSION_BEFORE_MIGRATION_SERVICE_INTRODUCED = new Version("2.1.1");
    private final Path dataDir;
    private final File dataDirVersionFile;

    public MigrationService(Path dataDir) {
        this.dataDir = dataDir;
        this.dataDirVersionFile = dataDir.resolve("version").toFile();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        Version dataDirVersion = getDataDirVersion();
        Version appVersion = ApplicationVersion.getVersion();

        if (dataDirVersion.below(appVersion)) {
            Migrator migrator = new Migrator(appVersion, dataDir);
            migrator.migrate();
        }

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    Version getDataDirVersion() {
        if (!dataDirVersionFile.exists()) {
            return VERSION_BEFORE_MIGRATION_SERVICE_INTRODUCED;
        }

        try {
            String version = Files.readString(dataDirVersionFile.toPath());
            return new Version(version);
        } catch (IOException e) {
            throw new RuntimeException("Can't identify data dir version. This shouldn't happen.", e);
        }
    }
}
