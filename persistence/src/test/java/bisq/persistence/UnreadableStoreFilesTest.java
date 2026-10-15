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
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class UnreadableStoreFilesTest {
    @TempDir
    Path dataDir;

    @Test
    void aPrivateStoreKeepsACopyWithTheSkippedEntryBeforeItsNextWrite() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.PRIVATE);
        byte[] storedBytes = writeStoreFile(persistence.getStorePath(), Map.of("profile", 1L, "", 2L));

        TimestampStore loaded = persistence.read().orElseThrow();
        persistence.persistAsync(loaded).join();

        assertEquals(Map.of("profile", 1L), loaded.getTimestampsByProfileId());
        List<UnreadableStoreFile> unreadableStoreFiles = persistenceService.getUnreadableStoreArchive()
                .getUnreadableStoreFiles();
        assertEquals(1, unreadableStoreFiles.size());
        UnreadableStoreFile unreadableStoreFile = unreadableStoreFiles.get(0);
        assertEquals("timestamp_store.protobuf", unreadableStoreFile.storeFileName());
        assertEquals(Reason.SKIPPED_ENTRIES, unreadableStoreFile.reason());
        assertEquals(Outcome.ARCHIVED, unreadableStoreFile.outcome());
        // The write removed the skipped entry from the store, the copy still holds it
        assertArrayEquals(storedBytes, Files.readAllBytes(unreadableStoreFile.archivedFile().orElseThrow()));
        assertThat(Files.readAllBytes(persistence.getStorePath())).isNotEqualTo(storedBytes);
    }

    @Test
    void aSettingsStoreAlsoKeepsACopy() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.SETTINGS);
        writeStoreFile(persistence.getStorePath(), Map.of("", 2L));

        persistence.read();

        assertEquals(1, persistenceService.getUnreadableStoreArchive().getUnreadableStoreFiles().size());
    }

    // Network and cache stores are filled again from the network, so they do not keep a copy
    @Test
    void aNetworkStoreKeepsNoCopy() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.NETWORK_DB);
        writeStoreFile(persistence.getStorePath(), Map.of("profile", 1L, "", 2L));

        persistence.read();

        assertTrue(persistenceService.getUnreadableStoreArchive().getUnreadableStoreFiles().isEmpty());
        assertThat(persistenceService.getUnreadableStoreArchive().getDirectory()).doesNotExist();
    }

    @Test
    void aStoreWithoutSkippedEntriesKeepsNoCopy() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.PRIVATE);
        writeStoreFile(persistence.getStorePath(), Map.of("profile", 1L));

        persistence.read();

        assertTrue(persistenceService.getUnreadableStoreArchive().getUnreadableStoreFiles().isEmpty());
    }

    @Test
    void aPrivateStoreFileWhichCannotBeReadIsMovedToTheArchive() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.PRIVATE);
        Files.createDirectories(persistence.getStorePath().getParent());
        byte[] storedBytes = "not a store".getBytes(StandardCharsets.UTF_8);
        Files.write(persistence.getStorePath(), storedBytes);

        assertTrue(persistence.read().isEmpty());

        UnreadableStoreFile unreadableStoreFile = persistenceService.getUnreadableStoreArchive()
                .getUnreadableStoreFiles().get(0);
        assertEquals(Reason.NOT_READABLE, unreadableStoreFile.reason());
        assertArrayEquals(storedBytes, Files.readAllBytes(unreadableStoreFile.archivedFile().orElseThrow()));
        assertThat(persistence.getStorePath()).doesNotExist();
        assertThat(persistence.getStorePath().getParent().resolve("corruptedFilesAtRead")).doesNotExist();
    }

    @Test
    void anUnreadableFileIsMovedToTheArchiveAndTheStoreIsRestoredFromABackup() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.PRIVATE);
        writeTwiceSoThatABackupExists(persistence);
        byte[] unreadableBytes = writeUnreadableFile(persistence.getStorePath());

        TimestampStore restored = persistence.read().orElseThrow();

        assertEquals(Map.of("profile", 1L), restored.getTimestampsByProfileId());
        UnreadableStoreFile unreadableStoreFile = persistenceService.getUnreadableStoreArchive()
                .getUnreadableStoreFiles().get(0);
        assertEquals(Reason.NOT_READABLE, unreadableStoreFile.reason());
        assertArrayEquals(unreadableBytes, Files.readAllBytes(unreadableStoreFile.archivedFile().orElseThrow()));
    }

    // Otherwise the restore from the backup would overwrite the file which stays at the store path
    @Test
    void anUnreadableFileGoesToTheEarlierFolderWhenTheArchiveCannotBeWritten() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.PRIVATE);
        writeTwiceSoThatABackupExists(persistence);
        byte[] unreadableBytes = writeUnreadableFile(persistence.getStorePath());
        Path archiveDir = persistenceService.getUnreadableStoreArchive().getDirectory();
        Files.createDirectories(archiveDir.getParent());
        Files.write(archiveDir, new byte[]{1});

        TimestampStore restored = persistence.read().orElseThrow();

        assertEquals(Map.of("profile", 1L), restored.getTimestampsByProfileId());
        assertEquals(Outcome.FAILED, persistenceService.getUnreadableStoreArchive()
                .getUnreadableStoreFiles().get(0).outcome());
        List<Path> keptFiles = filesIn(persistence.getStorePath().getParent().resolve("corruptedFilesAtRead"));
        assertEquals(1, keptFiles.size());
        assertArrayEquals(unreadableBytes, Files.readAllBytes(keptFiles.get(0)));
    }

    // The policy of the store of API access: its plaintext secrets must not outlive the store's own deletion
    @Test
    void aStoreWithThePolicyDeleteKeepsNothingOfAnUnreadableFile() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.PRIVATE,
                TimestampStore.withUnreadableStoreFilePolicy(UnreadableStoreFilePolicy.DELETE));
        writeUnreadableFile(persistence.getStorePath());

        assertTrue(persistence.read().isEmpty());

        assertThat(persistence.getStorePath()).doesNotExist();
        assertThat(persistence.getStorePath().getParent().resolve("corruptedFilesAtRead")).doesNotExist();
        assertThat(persistenceService.getUnreadableStoreArchive().getDirectory()).doesNotExist();
        assertTrue(persistenceService.getUnreadableStoreArchive().getUnreadableStoreFiles().isEmpty());
    }

    // The policy of the stores with network data in the settings directory, such as the store of peers
    @Test
    void aStoreWithThePolicyNoArchiveIsHandledLikeANetworkStore() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Persistence<TimestampStore> persistence = persistenceOf(persistenceService, DbSubDirectory.SETTINGS,
                TimestampStore.withUnreadableStoreFilePolicy(UnreadableStoreFilePolicy.NO_ARCHIVE));
        writeStoreFile(persistence.getStorePath(), Map.of("profile", 1L, "", 2L));
        persistence.read();
        byte[] unreadableBytes = writeUnreadableFile(persistence.getStorePath());

        persistence.read();

        assertTrue(persistenceService.getUnreadableStoreArchive().getUnreadableStoreFiles().isEmpty());
        List<Path> keptFiles = filesIn(persistence.getStorePath().getParent().resolve("corruptedFilesAtRead"));
        assertArrayEquals(unreadableBytes, Files.readAllBytes(keptFiles.get(0)));
    }

    @Test
    void readingAllStoresDeletesExpiredCopies() throws IOException {
        PersistenceService persistenceService = new PersistenceService(dataDir);
        Path archiveDir = persistenceService.getUnreadableStoreArchive().getDirectory();
        Files.createDirectories(archiveDir);
        Path expiredCopy = archiveDir.resolve("timestamp_store.protobuf_2020-01-01_000000_0123456789abcdef");
        Files.write(expiredCopy, new byte[]{1});

        persistenceService.readAllPersisted().join();

        assertThat(expiredCopy).doesNotExist();
    }

    private static Persistence<TimestampStore> persistenceOf(PersistenceService persistenceService,
                                                             DbSubDirectory dbSubDirectory) {
        return persistenceOf(persistenceService, dbSubDirectory, new TimestampStore());
    }

    private static Persistence<TimestampStore> persistenceOf(PersistenceService persistenceService,
                                                             DbSubDirectory dbSubDirectory,
                                                             TimestampStore store) {
        @SuppressWarnings("unchecked")
        PersistenceClient<TimestampStore> client = mock(PersistenceClient.class);
        return persistenceService.getOrCreatePersistence(client, dbSubDirectory, store);
    }

    // The second write copies the first store into the backups
    private static void writeTwiceSoThatABackupExists(Persistence<TimestampStore> persistence) {
        for (long timestamp = 1; timestamp <= 2; timestamp++) {
            TimestampStore store = new TimestampStore();
            store.getTimestampsByProfileId().put("profile", timestamp);
            persistence.persistAsync(store).join();
        }
    }

    private static byte[] writeUnreadableFile(Path storePath) throws IOException {
        byte[] unreadableBytes = "not a store".getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(storePath.getParent());
        Files.write(storePath, unreadableBytes);
        return unreadableBytes;
    }

    private static List<Path> filesIn(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.toList();
        }
    }

    private static byte[] writeStoreFile(Path storePath, Map<String, Long> timestampsByProfileId) throws IOException {
        bisq.persistence.protobuf.TimestampStore.Builder builder = bisq.persistence.protobuf.TimestampStore.newBuilder();
        timestampsByProfileId.forEach((profileId, timestamp) -> builder.addStringLongPairs(
                bisq.common.protobuf.StringLongPair.newBuilder().setKey(profileId).setValue(timestamp)));
        Files.createDirectories(storePath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(storePath)) {
            Any.pack(builder.build()).writeDelimitedTo(outputStream);
        }
        return Files.readAllBytes(storePath);
    }
}
