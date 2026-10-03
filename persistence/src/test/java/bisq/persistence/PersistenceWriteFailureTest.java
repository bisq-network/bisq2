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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            files.when(() -> FileMutatorUtils.renameFile(manager.getTempFilePath(), storePath)).thenReturn(false);

            assertThrows(IOException.class, manager::renameTempFileToCurrentFile);
        }
    }

    private static void blockTemporaryFile(Persistence<?> persistence) throws Exception {
        Path storePath = persistence.getStorePath();
        Files.createDirectories(storePath.getParent());
        Files.createDirectory(storePath.resolveSibling(PersistableStoreFileManager.TEMP_FILE_PREFIX +
                storePath.getFileName()));
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
