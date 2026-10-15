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

import bisq.persistence.UnreadableStoreArchive.Outcome;
import bisq.persistence.UnreadableStoreArchive.Reason;
import bisq.persistence.UnreadableStoreArchive.UnreadableStoreFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnreadableStoreArchiveTest {
    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @TempDir
    Path dataDir;
    private Path archiveDir;
    private Path storeFile;

    @BeforeEach
    void setUp() throws IOException {
        archiveDir = dataDir.resolve("db").resolve("unreadable");
        storeFile = dataDir.resolve("db").resolve("private").resolve("bisq_easy_trade_store.protobuf");
        Files.createDirectories(storeFile.getParent());
    }

    @Test
    void keepsACopyOfAStoreWithSkippedEntriesAndLeavesTheStoreFile() throws IOException {
        byte[] content = write(storeFile, "trades");
        UnreadableStoreArchive archive = archiveAt(NOW);

        assertTrue(archive.archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES));

        List<UnreadableStoreFile> unreadableStoreFiles = archive.getUnreadableStoreFiles();
        assertEquals(1, unreadableStoreFiles.size());
        UnreadableStoreFile unreadableStoreFile = unreadableStoreFiles.get(0);
        assertEquals("bisq_easy_trade_store.protobuf", unreadableStoreFile.storeFileName());
        assertEquals("bisq_easy_trade_store", unreadableStoreFile.storeName());
        assertEquals(Reason.SKIPPED_ENTRIES, unreadableStoreFile.reason());
        assertEquals(Outcome.ARCHIVED, unreadableStoreFile.outcome());
        Path copy = unreadableStoreFile.archivedFile().orElseThrow();
        assertEquals(archiveDir, copy.getParent());
        assertThat(copy.getFileName().toString()).matches("bisq_easy_trade_store\\.protobuf_2026-10-12_100000_[0-9a-f]{16}");
        assertArrayEquals(content, Files.readAllBytes(copy));
        assertArrayEquals(content, Files.readAllBytes(storeFile));
    }

    @Test
    void movesAStoreFileWhichCannotBeReadAtAll() throws IOException {
        byte[] content = write(storeFile, "not a store");
        UnreadableStoreArchive archive = archiveAt(NOW);

        assertTrue(archive.archive(storeFile, storeFile, Reason.NOT_READABLE));

        Path copy = archive.getUnreadableStoreFiles().get(0).archivedFile().orElseThrow();
        assertArrayEquals(content, Files.readAllBytes(copy));
        assertThat(storeFile).doesNotExist();
    }

    // A store which is not written in a session skips the same entries at the next start
    @Test
    void keepsOneCopyOfTheSameContentAndDoesNotReportItAgain() throws IOException {
        write(storeFile, "trades");
        archiveAt(NOW).archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES);
        UnreadableStoreArchive nextStart = archiveAt(NOW.plus(Duration.ofDays(1)));

        // The content is in the archive, so it counts as kept
        assertTrue(nextStart.archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES));

        assertEquals(1, filesIn(archiveDir).size());
        assertTrue(nextStart.getUnreadableStoreFiles().isEmpty());
    }

    @Test
    void keepsACopyOfChangedContent() throws IOException {
        UnreadableStoreArchive archive = archiveAt(NOW);
        write(storeFile, "trades");
        archive.archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES);
        write(storeFile, "other trades");

        archive.archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES);

        assertEquals(2, filesIn(archiveDir).size());
        assertEquals(2, archive.getUnreadableStoreFiles().size());
    }

    @Test
    void keepsNoCopyWhenTheArchiveWouldExceedItsSizeLimit() throws IOException {
        Files.createDirectories(archiveDir);
        Path existingCopy = write(archiveDir.resolve("identity_store.protobuf_2026-10-01_100000_0123456789abcdef"));
        write(storeFile, "trades");
        UnreadableStoreArchive archive = archiveAt(NOW, Files.size(existingCopy) + 5);

        assertFalse(archive.archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES));

        UnreadableStoreFile unreadableStoreFile = archive.getUnreadableStoreFiles().get(0);
        assertEquals(Outcome.SIZE_LIMIT_REACHED, unreadableStoreFile.outcome());
        assertTrue(unreadableStoreFile.archivedFile().isEmpty());
        // The older copy stays: a full archive must not push out copies before their retention ends
        assertEquals(List.of(existingCopy), filesIn(archiveDir));
    }

    // A move takes no further space, and a file left at the store path is overwritten by a restore from a backup
    @Test
    void movesAStoreFileWhichCannotBeReadAlsoWhenTheArchiveIsFull() throws IOException {
        Files.createDirectories(archiveDir);
        Path existingCopy = write(archiveDir.resolve("identity_store.protobuf_2026-10-01_100000_0123456789abcdef"));
        byte[] content = write(storeFile, "not a store");
        UnreadableStoreArchive archive = archiveAt(NOW, Files.size(existingCopy));

        assertTrue(archive.archive(storeFile, storeFile, Reason.NOT_READABLE));

        Path copy = archive.getUnreadableStoreFiles().get(0).archivedFile().orElseThrow();
        assertArrayEquals(content, Files.readAllBytes(copy));
        assertThat(storeFile).doesNotExist();
    }

    @Test
    void reportsAFailureAndKeepsTheStoreFileWhenTheArchiveCannotBeWritten() throws IOException {
        // A regular file where the folder of the archive should be
        Files.createDirectories(archiveDir.getParent());
        write(archiveDir);
        byte[] content = write(storeFile, "not a store");
        UnreadableStoreArchive archive = archiveAt(NOW);

        assertFalse(archive.archive(storeFile, storeFile, Reason.NOT_READABLE));

        assertEquals(Outcome.FAILED, archive.getUnreadableStoreFiles().get(0).outcome());
        assertArrayEquals(content, Files.readAllBytes(storeFile));
    }

    @Test
    void deletesACopyOnlyAfterItsRetention() throws IOException {
        write(storeFile, "trades");
        archiveAt(NOW).archive(storeFile, storeFile, Reason.SKIPPED_ENTRIES);

        archiveAt(NOW.plus(UnreadableStoreArchive.RETENTION).minusSeconds(1)).pruneExpired();
        assertEquals(1, filesIn(archiveDir).size());

        archiveAt(NOW.plus(UnreadableStoreArchive.RETENTION).plusSeconds(1)).pruneExpired();
        assertTrue(filesIn(archiveDir).isEmpty());
    }

    @Test
    void pruningDeletesTemporaryFilesAndLeavesOtherFiles() throws IOException {
        Files.createDirectories(archiveDir);
        Path tempFile = write(archiveDir.resolve("temp_bisq_easy_trade_store.protobuf_2026-10-12_100000_0123456789abcdef"));
        Path otherFile = write(archiveDir.resolve("notes.txt"));

        archiveAt(NOW.plus(Duration.ofDays(1000))).pruneExpired();

        assertThat(tempFile).doesNotExist();
        assertThat(otherFile).exists();
    }

    private UnreadableStoreArchive archiveAt(Instant instant) {
        return archiveAt(instant, UnreadableStoreArchive.MAX_TOTAL_SIZE);
    }

    private UnreadableStoreArchive archiveAt(Instant instant, long maxTotalSize) {
        return new UnreadableStoreArchive(archiveDir, Clock.fixed(instant, ZoneOffset.UTC), maxTotalSize);
    }

    private static byte[] write(Path file, String text) throws IOException {
        byte[] content = text.getBytes(StandardCharsets.UTF_8);
        Files.write(file, content);
        return content;
    }

    private static Path write(Path file) throws IOException {
        write(file, "content");
        return file;
    }

    private static List<Path> filesIn(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.toList();
        }
    }
}
