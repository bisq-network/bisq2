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

package bisq.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TailsDataDirMigrationTest {

    @Test
    void copiesDataAndSkipsLockAndExternalTorConfig(@TempDir Path tempDirPath) throws IOException {
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Files.createDirectories(legacyPath.resolve("db").resolve("private"));
        Files.writeString(legacyPath.resolve("db").resolve("private").resolve("KeyBundleStore"), "keys");
        Files.createDirectories(legacyPath.resolve("tor"));
        Files.writeString(legacyPath.resolve("tor").resolve("external_tor.config"), "ControlPort 127.0.0.1:9051");
        Files.writeString(legacyPath.resolve("instance.lock"), "123");
        Path targetPath = tempDirPath.resolve("Persistent").resolve("Bisq2");
        Files.createDirectories(targetPath.getParent());

        assertTrue(TailsDataDirMigration.migrate(legacyPath, targetPath));

        assertEquals("keys", Files.readString(targetPath.resolve("db").resolve("private").resolve("KeyBundleStore")));
        assertTrue(Files.isDirectory(targetPath.resolve("tor")));
        assertFalse(Files.exists(targetPath.resolve("tor").resolve("external_tor.config")));
        assertFalse(Files.exists(targetPath.resolve("instance.lock")));
        assertFalse(Files.exists(targetPath.resolveSibling("Bisq2.migrating")));
        assertTrue(Files.exists(legacyPath.resolve("db").resolve("private").resolve("KeyBundleStore")));
    }

    @Test
    void copiesDotfilesSymlinksAsRegularFiles(@TempDir Path tempDirPath) throws IOException {
        Path dotfilesFilePath = tempDirPath.resolve("dotfiles").resolve("settings");
        Files.createDirectories(dotfilesFilePath.getParent());
        Files.writeString(dotfilesFilePath, "settings");
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Files.createDirectories(legacyPath);
        Files.createSymbolicLink(legacyPath.resolve("settings"), dotfilesFilePath);
        Path targetPath = tempDirPath.resolve("Bisq2");

        assertTrue(TailsDataDirMigration.migrate(legacyPath, targetPath));

        Path migratedFilePath = targetPath.resolve("settings");
        assertFalse(Files.isSymbolicLink(migratedFilePath));
        assertEquals("settings", Files.readString(migratedFilePath));
    }

    @Test
    void followsSymlinkedDataDir(@TempDir Path tempDirPath) throws IOException {
        Path realDataDirPath = tempDirPath.resolve("elsewhere").resolve("Bisq2");
        Files.createDirectories(realDataDirPath);
        Files.writeString(realDataDirPath.resolve("settings"), "settings");
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Files.createDirectories(legacyPath.getParent());
        Files.createSymbolicLink(legacyPath, realDataDirPath);
        Path targetPath = tempDirPath.resolve("Bisq2");

        assertTrue(TailsDataDirMigration.migrate(legacyPath, targetPath));

        assertEquals("settings", Files.readString(targetPath.resolve("settings")));
    }

    @Test
    void followsSymlinkedSubdirectory(@TempDir Path tempDirPath) throws IOException {
        Path realDbPath = tempDirPath.resolve("elsewhere").resolve("db");
        Files.createDirectories(realDbPath);
        Files.writeString(realDbPath.resolve("KeyBundleStore"), "keys");
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Files.createDirectories(legacyPath);
        Files.createSymbolicLink(legacyPath.resolve("db"), realDbPath);
        Path targetPath = tempDirPath.resolve("Bisq2");

        assertTrue(TailsDataDirMigration.migrate(legacyPath, targetPath));

        assertFalse(Files.isSymbolicLink(targetPath.resolve("db")));
        assertEquals("keys", Files.readString(targetPath.resolve("db").resolve("KeyBundleStore")));
    }

    @Test
    void doesNotTouchExistingDataDir(@TempDir Path tempDirPath) throws IOException {
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Files.createDirectories(legacyPath);
        Files.writeString(legacyPath.resolve("settings"), "legacy");
        Path targetPath = tempDirPath.resolve("Bisq2");
        Files.createDirectories(targetPath);
        Files.writeString(targetPath.resolve("settings"), "current");

        assertFalse(TailsDataDirMigration.migrate(legacyPath, targetPath));

        assertEquals("current", Files.readString(targetPath.resolve("settings")));
    }

    @Test
    void skipsMissingOrEmptyLegacyDir(@TempDir Path tempDirPath) throws IOException {
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Path targetPath = tempDirPath.resolve("Bisq2");

        assertFalse(TailsDataDirMigration.migrate(legacyPath, targetPath));

        Files.createDirectories(legacyPath);
        assertFalse(TailsDataDirMigration.migrate(legacyPath, targetPath));
        assertFalse(Files.exists(targetPath));
    }

    @Test
    void skipsWhenLegacyDirIsTheDataDir(@TempDir Path tempDirPath) throws IOException {
        Path dataDirPath = tempDirPath.resolve("Bisq2");
        Files.createDirectories(dataDirPath);
        Files.writeString(dataDirPath.resolve("settings"), "settings");

        assertFalse(TailsDataDirMigration.migrate(dataDirPath, dataDirPath));
    }

    @Test
    void replacesLeftoverFromInterruptedMigration(@TempDir Path tempDirPath) throws IOException {
        Path legacyPath = tempDirPath.resolve("legacy").resolve("Bisq2");
        Files.createDirectories(legacyPath);
        Files.writeString(legacyPath.resolve("settings"), "settings");
        Path leftoverPath = tempDirPath.resolve("Bisq2.migrating");
        Files.createDirectories(leftoverPath);
        Files.writeString(leftoverPath.resolve("partial"), "partial");
        Path targetPath = tempDirPath.resolve("Bisq2");

        assertTrue(TailsDataDirMigration.migrate(legacyPath, targetPath));

        assertEquals("settings", Files.readString(targetPath.resolve("settings")));
        assertFalse(Files.exists(targetPath.resolve("partial")));
        assertFalse(Files.exists(leftoverPath));
    }
}
