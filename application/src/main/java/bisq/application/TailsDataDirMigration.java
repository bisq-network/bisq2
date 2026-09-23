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
import bisq.common.platform.OS;
import bisq.common.platform.PlatformUtils;
import bisq.common.platform.TailsPersistenceGuard;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
@Slf4j
final class TailsDataDirMigration {
    private static final Path DOTFILES_DIR_PATH = Paths.get(TailsPersistenceGuard.PERSISTENCE_MOUNT, "dotfiles");
    private static final String MIGRATION_DIR_SUFFIX = ".migrating";
    // A stale lock file is not data. An external_tor.config from a version without Tails support points
    // at the wrong control port, and the Tails defaults are only written when the file is absent.
    private static final Set<Path> RELATIVE_PATHS_TO_SKIP = Set.of(
            Paths.get(InstanceLock.LOCK_FILE_NAME),
            Paths.get("tor", "external_tor.config"));

    private TailsDataDirMigration() {
    }

    /**
     * @return the directory the data was copied from, if a migration happened.
     * @throws TailsDataDirMigrationException if the data could not be copied.
     */
    static Optional<Path> migrateIfNeeded(Path appDataDirPath) {
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

    static boolean migrate(Path legacyDataDirPath, Path appDataDirPath) {
        Path migrationDirPath = appDataDirPath.resolveSibling(appDataDirPath.getFileName() + MIGRATION_DIR_SUFFIX);
        try {
            // An existing data directory means the user already runs from it, so never overwrite it.
            if (legacyDataDirPath.equals(appDataDirPath) ||
                    Files.exists(appDataDirPath) ||
                    isEmptyOrMissing(legacyDataDirPath)) {
                return false;
            }

            // Copy into a sibling first, so an interrupted copy never leaves a partial data directory behind.
            FileMutatorUtils.deleteFileOrDirectory(migrationDirPath);
            copyFollowingLinks(legacyDataDirPath, migrationDirPath);
            // Tails users may pull the USB stick to shut down, so the copy must be on disk before the
            // data directory exists, as an existing one is never migrated again.
            syncTree(migrationDirPath);
            Files.move(migrationDirPath, appDataDirPath, StandardCopyOption.ATOMIC_MOVE);
            syncDirectory(appDataDirPath.getParent());
            return true;
        } catch (IOException | RuntimeException e) {
            try {
                FileMutatorUtils.deleteFileOrDirectory(migrationDirPath);
            } catch (IOException | RuntimeException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw new TailsDataDirMigrationException(legacyDataDirPath, appDataDirPath, e);
        }
    }

    // Dotfiles links single files, but users may also have linked the whole directory or subdirectories,
    // so every link is resolved to copy real content.
    private static void copyFollowingLinks(Path sourceDirPath, Path destinationDirPath) throws IOException {
        try (Stream<Path> sourcePaths = Files.walk(sourceDirPath, FileVisitOption.FOLLOW_LINKS)) {
            for (Path sourcePath : (Iterable<Path>) sourcePaths::iterator) {
                Path relativePath = sourceDirPath.relativize(sourcePath);
                Path destinationPath = destinationDirPath.resolve(relativePath.toString());
                if (Files.isDirectory(sourcePath)) {
                    FileMutatorUtils.createDirectories(destinationPath);
                } else if (!Files.exists(sourcePath)) {
                    // A dangling link has no data to copy.
                    log.warn("Skipped the broken link {} while copying the data directory", sourcePath);
                } else if (!RELATIVE_PATHS_TO_SKIP.contains(relativePath)) {
                    FileMutatorUtils.copyFile(sourcePath, destinationPath);
                    Files.setLastModifiedTime(destinationPath, Files.getLastModifiedTime(sourcePath));
                }
            }
        }
    }

    private static void syncTree(Path dirPath) throws IOException {
        try (Stream<Path> paths = Files.walk(dirPath)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (Files.isDirectory(path)) {
                    syncDirectory(path);
                } else {
                    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                        channel.force(true);
                    }
                }
            }
        }
    }

    // Syncing a directory persists its entries. Windows cannot open directories, but only Tails runs
    // this in production.
    private static void syncDirectory(Path dirPath) throws IOException {
        if (OS.isWindows()) {
            return;
        }
        try (FileChannel channel = FileChannel.open(dirPath, StandardOpenOption.READ)) {
            channel.force(true);
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
