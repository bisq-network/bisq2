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

package bisq.persistence;

import bisq.common.data.ByteUnit;
import bisq.common.encoding.Hex;
import bisq.common.file.FileMutatorUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps a copy of a private or settings store file which this version could not read completely. The store deletes the
 * entries which it could not read at its next write, so the copy is the only place where they stay recoverable.
 * The rules are in docs/specifications/persistence/unreadable-store-files.md.
 */
@Slf4j
public class UnreadableStoreArchive {
    public static final Duration RETENTION = Duration.ofDays(90);
    public static final long MAX_TOTAL_SIZE = (long) ByteUnit.MB.toBytes(100);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final int HASH_LENGTH_IN_FILE_NAME = 16;
    private static final String TEMP_FILE_PREFIX = "temp_";
    private static final Pattern ARCHIVED_FILE_NAME =
            Pattern.compile("^.+_(\\d{4}-\\d{2}-\\d{2}_\\d{6})_[0-9a-f]{" + HASH_LENGTH_IN_FILE_NAME + "}$");

    public enum Reason {
        /** The store loaded, but it skipped entries which it could not parse. */
        SKIPPED_ENTRIES,
        /** The store file could not be loaded at all. */
        NOT_READABLE
    }

    public enum Outcome {
        ARCHIVED,
        /** A copy would exceed {@link #MAX_TOTAL_SIZE}, so no copy was kept. A move is not limited. */
        SIZE_LIMIT_REACHED,
        FAILED
    }

    /**
     * @param storeFileName the file name of the store, for example {@code bisq_easy_trade_store.protobuf}
     * @param archivedFile  the copy, present only for {@link Outcome#ARCHIVED}
     */
    public record UnreadableStoreFile(String storeFileName, Reason reason, Outcome outcome,
                                      Optional<Path> archivedFile) {
        /** The name of the store for the user, for example {@code bisq_easy_trade_store}. */
        public String storeName() {
            return storeFileName.endsWith(Persistence.EXTENSION)
                    ? storeFileName.substring(0, storeFileName.length() - Persistence.EXTENSION.length())
                    : storeFileName;
        }
    }

    @Getter
    private final Path directory;
    private final Clock clock;
    private final long maxTotalSize;
    private final List<UnreadableStoreFile> unreadableStoreFiles = new CopyOnWriteArrayList<>();

    public UnreadableStoreArchive(Path directory) {
        this(directory, Clock.systemUTC(), MAX_TOTAL_SIZE);
    }

    @VisibleForTesting
    UnreadableStoreArchive(Path directory, Clock clock, long maxTotalSize) {
        this.directory = directory;
        this.clock = clock;
        this.maxTotalSize = maxTotalSize;
    }

    /**
     * Keeps a copy of {@code sourceFile}, the content of the store at {@code storeFilePath} or of one of its backups.
     * A file which cannot be read at all is moved instead, as before, so that the store can be restored from a backup.
     * The move is not limited by {@link #MAX_TOTAL_SIZE}: it takes no further space on the disk, and a restore would
     * overwrite a file which stays in place. Never throws: the store loads also when the copy fails.
     *
     * @return whether the archive holds the content of {@code sourceFile}, from this call or from an earlier one
     */
    public synchronized boolean archive(Path storeFilePath, Path sourceFile, Reason reason) {
        String storeFileName = storeFilePath.getFileName().toString();
        Outcome outcome;
        Optional<Path> archivedFile = Optional.empty();
        try {
            String hash = hashOfFile(sourceFile).substring(0, HASH_LENGTH_IN_FILE_NAME);
            if (isAlreadyArchived(storeFileName, hash)) {
                // A copy with the same content exists, from this or an earlier start. The user has been told about it.
                log.info("The archive already holds a copy of {} with the same content", storeFileName);
                return true;
            }
            long size = Files.size(sourceFile);
            if (reason == Reason.SKIPPED_ENTRIES && getArchiveSize() + size > maxTotalSize) {
                outcome = Outcome.SIZE_LIMIT_REACHED;
            } else {
                FileMutatorUtils.createDirectories(directory);
                String fileName = storeFileName + "_" + DATE_FORMAT.format(LocalDateTime.now(clock)) + "_" + hash;
                Path target = directory.resolve(fileName);
                if (reason == Reason.NOT_READABLE) {
                    moveFile(sourceFile, target);
                } else {
                    // Copied under a temporary name first, so that a file with the final name is always complete
                    Path tempFile = directory.resolve(TEMP_FILE_PREFIX + fileName);
                    FileMutatorUtils.copyFile(sourceFile, tempFile);
                    moveFile(tempFile, target);
                }
                outcome = Outcome.ARCHIVED;
                archivedFile = Optional.of(target);
            }
        } catch (IOException | RuntimeException e) {
            log.error("Could not keep a copy of the store file {} which this version could not read completely",
                    storeFileName, e);
            outcome = Outcome.FAILED;
        }
        if (outcome == Outcome.SIZE_LIMIT_REACHED) {
            log.error("Could not keep a copy of the store file {} which this version could not read completely, " +
                    "because the archive {} reached its size limit", storeFileName, directory);
        } else if (outcome == Outcome.ARCHIVED) {
            log.warn("Kept a copy of the store file {} which this version could not read completely ({}) at {}",
                    storeFileName, reason, archivedFile.get());
        }
        unreadableStoreFiles.add(new UnreadableStoreFile(storeFileName, reason, outcome, archivedFile));
        return outcome == Outcome.ARCHIVED;
    }

    /**
     * Deletes the copies which are older than {@link #RETENTION}, and temporary files of an interrupted copy. Files
     * whose name does not have the form of a copy are not touched.
     */
    public synchronized void pruneExpired() {
        if (!Files.isDirectory(directory)) {
            return;
        }
        LocalDateTime oldestKept = LocalDateTime.now(clock).minus(RETENTION);
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                String fileName = file.getFileName().toString();
                if (fileName.startsWith(TEMP_FILE_PREFIX)) {
                    deleteFile(file);
                    return;
                }
                findArchiveDate(fileName)
                        .filter(archiveDate -> archiveDate.isBefore(oldestKept))
                        .ifPresent(archiveDate -> deleteFile(file));
            });
        } catch (IOException e) {
            log.error("Could not delete expired copies in {}", directory, e);
        }
    }

    /** The store files which this version could not read completely since the start of the node. */
    public List<UnreadableStoreFile> getUnreadableStoreFiles() {
        return List.copyOf(unreadableStoreFiles);
    }

    private boolean isAlreadyArchived(String storeFileName, String hash) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString())
                    .anyMatch(fileName -> fileName.startsWith(storeFileName + "_") && fileName.endsWith("_" + hash) &&
                            ARCHIVED_FILE_NAME.matcher(fileName).matches());
        }
    }

    private long getArchiveSize() throws IOException {
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(directory)) {
            long size = 0;
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                size += Files.size(file);
            }
            return size;
        }
    }

    private static Optional<LocalDateTime> findArchiveDate(String fileName) {
        Matcher matcher = ARCHIVED_FILE_NAME.matcher(fileName);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(matcher.group(1), DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String hashOfFile(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, read);
            }
            return Hex.encode(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void moveFile(Path source, Path target) throws IOException {
        if (!FileMutatorUtils.renameFile(source, target)) {
            throw new IOException("Could not move " + source + " to " + target);
        }
    }

    private static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
            log.info("Deleted {}", file);
        } catch (IOException e) {
            log.error("Could not delete {}", file, e);
        }
    }
}
