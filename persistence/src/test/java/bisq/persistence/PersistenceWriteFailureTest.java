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

import bisq.common.file.FileMutatorUtils;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

class PersistenceWriteFailureTest {
    @TempDir
    Path tempDir;

    @Test
    void asynchronousWriteReportsDiskFailure() throws Exception {
        TestClient client = new TestClient(new PersistenceService(tempDir));
        blockTemporaryFile(client.persistence);

        assertThrows(CompletionException.class,
                () -> client.persistence.persistAsync(client.store).join());
    }

    @Test
    void clientReportsDiskFailure() throws Exception {
        TestClient client = new TestClient(new PersistenceService(tempDir));
        blockTemporaryFile(client.persistence);

        assertFalse(client.persist().join());
    }

    // A rate-limited client writes at shutdown only if it knows that its state is not on disk
    @Test
    void rateLimitedClientWritesAFailedStateAgain() throws Exception {
        RateLimitedTestClient client = new RateLimitedTestClient(new PersistenceService(tempDir));
        blockTemporaryFile(client.getPersistence());

        assertFalse(client.persist().join());

        assertTrue(client.isDropped());
    }

    @Test
    void persistingAllClientsReportsAnyFailedWrite() throws Exception {
        PersistenceService service = new PersistenceService(tempDir);
        TestClient successful = new TestClient(service, "SuccessfulStore");
        TestClient failing = new TestClient(service, "FailingStore");
        blockTemporaryFile(failing.persistence);

        assertFalse(service.persistAllClients().join());
        assertFalse(successful.persistence.read().isEmpty());
    }

    @Test
    void asynchronousWriteReportsSerializationFailure() {
        TimestampStore store = spy(new TimestampStore());
        doThrow(new IllegalArgumentException("Cannot serialize store")).when(store).toAny();
        Persistence<TimestampStore> persistence = new Persistence<>(tempDir.resolve("db/private"),
                "TestStore", MaxBackupSize.ZERO, new RestoreService());

        assertThrows(CompletionException.class, () -> persistence.persistAsync(store).join());
    }

    @Test
    void failedRenameReportsReplacementFailure() throws Exception {
        Path storePath = tempDir.resolve("db/private/store.protobuf");
        PersistableStoreFileManager manager = new PersistableStoreFileManager(storePath);
        Files.createDirectories(storePath.getParent());
        Files.createFile(manager.getTempFilePath());
        try (MockedStatic<FileMutatorUtils> files = mockStatic(FileMutatorUtils.class, CALLS_REAL_METHODS)) {
            files.when(() -> FileMutatorUtils.replaceFile(manager.getTempFilePath(), storePath))
                    .thenThrow(new IOException("rename failed"));

            assertThrows(IOException.class, manager::replaceCurrentFileWithTempFile);
        }
    }

    // A write which fails at the replacement must leave the previous store readable. If the writer removed or moved
    // the current file before the rename, the next start would read no file and begin with an empty store.
    @ParameterizedTest
    @EnumSource(value = MaxBackupSize.class, names = {"ZERO", "TEN_MB"})
    void failedReplacementKeepsPreviousStore(MaxBackupSize maxBackupSize) throws Exception {
        Path storePath = tempDir.resolve("data/db/private/test_store.protobuf");
        PersistableStoreFileManager manager = new PersistableStoreFileManager(storePath, maxBackupSize);
        PersistableStoreReaderWriter<TimestampStore> readerWriter =
                new PersistableStoreReaderWriter<>(manager, new RestoreService());
        TimestampStore previous = new TimestampStore();
        previous.getTimestampsByProfileId().put("A", 1L);
        PersistableStoreResolver.addResolver(previous);
        readerWriter.write(previous);

        TimestampStore next = new TimestampStore();
        next.getTimestampsByProfileId().put("A", 2L);
        try (MockedStatic<FileMutatorUtils> files = mockStatic(FileMutatorUtils.class, CALLS_REAL_METHODS)) {
            files.when(() -> FileMutatorUtils.replaceFile(manager.getTempFilePath(), storePath))
                    .thenThrow(new IOException("rename failed"));

            assertThrows(IllegalStateException.class, () -> readerWriter.write(next));
        }

        TimestampStore read = readerWriter.read().orElseThrow();
        assertEquals(1L, read.getTimestampsByProfileId().get("A"));
    }

    // The clients turn a failed write into false and drop the cause, and most callers ignore the result. Without a log
    // entry where the write fails, the node ran on with unsaved data and nobody could see why.
    @Test
    void failedWriteIsLoggedWithTheStorePath() throws Exception {
        TestClient client = new TestClient(new PersistenceService(tempDir));
        blockTemporaryFile(client.persistence);

        List<ILoggingEvent> events = captureErrors(() -> assertFalse(client.persist().join()));

        assertEquals(1, events.size());
        assertTrue(events.get(0).getFormattedMessage().contains(client.persistence.getStorePath().toString()));
    }

    // The message of a serialization failure can contain values of the store, which can be private
    @Test
    void failedSerializationIsLoggedWithoutTheMessageOfItsCause() {
        TimestampStore store = spy(new TimestampStore());
        doThrow(new IllegalArgumentException("private value")).when(store).toAny();
        Persistence<TimestampStore> persistence = new Persistence<>(tempDir.resolve("db/private"),
                "TestStore", MaxBackupSize.ZERO, new RestoreService());

        List<ILoggingEvent> events = captureErrors(() ->
                assertThrows(CompletionException.class, () -> persistence.persistAsync(store).join()));

        assertEquals(1, events.size());
        assertTrue(events.get(0).getFormattedMessage().contains(persistence.getStorePath().toString()));
        assertFalse(events.get(0).getFormattedMessage().contains("private value"));
        assertTrue(events.get(0).getThrowableProxy() == null);
    }

    private static List<ILoggingEvent> captureErrors(Runnable runnable) {
        Logger logger = (Logger) LoggerFactory.getLogger(Persistence.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            runnable.run();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
    }

    private static void blockTemporaryFile(Persistence<?> persistence) throws Exception {
        Path storePath = persistence.getStorePath();
        Files.createDirectories(storePath.getParent());
        Files.createDirectory(storePath.resolveSibling(PersistableStoreFileManager.TEMP_FILE_PREFIX +
                storePath.getFileName()));
    }

    private static class RateLimitedTestClient extends RateLimitedPersistenceClient<TimestampStore> {
        private final TimestampStore store = new TimestampStore();
        private final Persistence<TimestampStore> persistence;

        RateLimitedTestClient(PersistenceService service) {
            persistence = service.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, "RateLimitedTestStore", store);
        }

        @Override
        public Persistence<TimestampStore> getPersistence() {
            return persistence;
        }

        @Override
        public TimestampStore getPersistableStore() {
            return store;
        }
    }

    private static class TestClient implements PersistenceClient<TimestampStore> {
        private final TimestampStore store = new TimestampStore();
        private final Persistence<TimestampStore> persistence;

        TestClient(PersistenceService service) {
            this(service, "TestStore");
        }

        TestClient(PersistenceService service, String name) {
            persistence = service.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, name, store);
        }

        @Override
        public Persistence<TimestampStore> getPersistence() {
            return persistence;
        }

        @Override
        public TimestampStore getPersistableStore() {
            return store;
        }
    }
}
