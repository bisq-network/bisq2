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

import bisq.common.file.FileMutatorUtils;
import bisq.common.platform.LinuxDistribution;
import bisq.common.platform.PlatformUtils;
import bisq.common.platform.TailsPersistenceGuard;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * On Tails, earlier versions kept the data directory in ~/.local/share, which is lost on shutdown
 * unless the user persisted it, usually with the Tails Dotfiles feature. Dotfiles symlinks each file
 * into the home directory, and Bisq replaces its files by renaming a temp file over them, which breaks
 * those links. The persisted copy is therefore a snapshot restored on every boot. Now that the data
 * directory is on Persistent Storage, we copy that data over once so the user keeps their identity.
 */
final class TailsDataDirMigration {
    private static final Path DOTFILES_DIR_PATH = Paths.get(TailsPersistenceGuard.PERSISTENCE_MOUNT, "dotfiles");
    private static final String MIGRATION_DIR_SUFFIX = ".migrating";
    // A stale lock file is not data. An external_tor.config from a version without Tails support points
    // at the wrong control port, and the Tails defaults are only written when the file is absent.
    private static final Set<String> FILE_NAMES_TO_SKIP = Set.of(InstanceLock.LOCK_FILE_NAME, "external_tor.config");

    private TailsDataDirMigration() {
    }

    /**
     * @return the directory the data was copied from, if a migration happened.
     */
    static Optional<Path> migrateIfNeeded(Path appDataDirPath) throws IOException {
        if (!LinuxDistribution.isTails()) {
            return Optional.empty();
        }
        Path legacyDataDirPath = PlatformUtils.getHomeDirectoryPath().resolve(".local").resolve("share")
                .resolve(appDataDirPath.getFileName());
        return migrate(legacyDataDirPath, appDataDirPath)
                ? Optional.of(legacyDataDirPath)
                : Optional.empty();
    }

    /**
     * @return the copy of the data directory kept with the Tails Dotfiles feature, if there is one.
     */
    static Optional<Path> findDotfilesDataDir(Path appDataDirPath) {
        Path dotfilesDataDirPath = DOTFILES_DIR_PATH.resolve(".local").resolve("share")
                .resolve(appDataDirPath.getFileName());
        return Files.isDirectory(dotfilesDataDirPath) ? Optional.of(dotfilesDataDirPath) : Optional.empty();
    }

    static boolean migrate(Path legacyDataDirPath, Path appDataDirPath) throws IOException {
        // An existing data directory means the user already runs from it, so never overwrite it.
        if (legacyDataDirPath.equals(appDataDirPath) || Files.exists(appDataDirPath) || isEmptyOrMissing(legacyDataDirPath)) {
            return false;
        }

        // Copy into a sibling first, so an interrupted copy never leaves a partial data directory behind.
        Path migrationDirPath = appDataDirPath.resolveSibling(appDataDirPath.getFileName() + MIGRATION_DIR_SUFFIX);
        FileMutatorUtils.deleteFileOrDirectory(migrationDirPath);
        try {
            copyFollowingLinks(legacyDataDirPath, migrationDirPath);
            Files.move(migrationDirPath, appDataDirPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            FileMutatorUtils.deleteFileOrDirectory(migrationDirPath);
            throw e;
        }
        return true;
    }

    // Dotfiles links single files, but users may also have linked the whole directory or subdirectories,
    // so every link is resolved to copy real content.
    private static void copyFollowingLinks(Path sourceDirPath, Path destinationDirPath) throws IOException {
        try (Stream<Path> sourcePaths = Files.walk(sourceDirPath, FileVisitOption.FOLLOW_LINKS)) {
            for (Path sourcePath : (Iterable<Path>) sourcePaths::iterator) {
                Path destinationPath = destinationDirPath.resolve(sourceDirPath.relativize(sourcePath).toString());
                if (Files.isDirectory(sourcePath)) {
                    FileMutatorUtils.createDirectories(destinationPath);
                } else if (!FILE_NAMES_TO_SKIP.contains(sourcePath.getFileName().toString())) {
                    FileMutatorUtils.copyFile(sourcePath, destinationPath);
                }
            }
        }
    }

    private static boolean isEmptyOrMissing(Path dirPath) throws IOException {
        if (!Files.isDirectory(dirPath)) {
            return true;
        }
        try (Stream<Path> entries = Files.list(dirPath)) {
            return entries.findAny().isEmpty();
        }
    }
}
