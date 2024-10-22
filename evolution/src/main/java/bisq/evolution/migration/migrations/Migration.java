package bisq.evolution.migration.migrations;

import bisq.common.platform.Version;

import java.nio.file.Path;

public interface Migration {
    void run(Path dataDir);

    Version getVersion();
}
