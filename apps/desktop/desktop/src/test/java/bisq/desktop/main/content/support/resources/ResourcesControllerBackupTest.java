/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.support.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The backup of the data directory copies the data of the user and leaves out what the node creates again.
 */
class ResourcesControllerBackupTest {
    @TempDir
    Path tempDirPath;

    private Path appDataDirPath;
    private Path backupDirPath;

    @BeforeEach
    void setUp() throws IOException {
        appDataDirPath = tempDirPath.resolve("Bisq2");
        backupDirPath = tempDirPath.resolve("backup");
        writeFile(appDataDirPath.resolve("db/private/key_bundle_store.protobuf"));
        writeFile(appDataDirPath.resolve("tor/torrc"));
        writeFile(appDataDirPath.resolve("tor/external_tor.config"));
        writeFile(appDataDirPath.resolve("tor/bin/version"));
        writeFile(appDataDirPath.resolve("tor/bin/tor"));
        writeFile(appDataDirPath.resolve("tor/bin/pluggable_transports/lyrebird"));
        writeFile(appDataDirPath.resolve("tor/sandy/sandy"));
        writeFile(appDataDirPath.resolve("tor/data/state"));
    }

    @Test
    void backupLeavesOutTheTorBundleAndSandy() throws IOException {
        ResourcesController.copyDataDirectory(appDataDirPath, backupDirPath);

        // The node installs both again at its next start of Tor, the bundle because its version file is missing too
        assertThat(backupDirPath.resolve("tor/bin")).doesNotExist();
        assertThat(backupDirPath.resolve("tor/sandy")).doesNotExist();
        assertThat(backupDirPath.resolve("tor/data")).doesNotExist();
    }

    @Test
    void backupKeepsTheDataOfTheUserAndTheExternalTorConfig() throws IOException {
        ResourcesController.copyDataDirectory(appDataDirPath, backupDirPath);

        assertThat(backupDirPath.resolve("db/private/key_bundle_store.protobuf")).exists();
        assertThat(backupDirPath.resolve("tor/torrc")).exists();
        // It can hold the password of an external Tor; docs/specifications/network/tor-startup.md says so
        assertThat(backupDirPath.resolve("tor/external_tor.config")).exists();
    }

    private static void writeFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, path.getFileName().toString());
    }
}
