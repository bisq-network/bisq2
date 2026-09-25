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
import bisq.common.fsm.FsmModel;
import bisq.common.fsm.SnapshotLockTimeoutException;
import bisq.common.fsm.State;
import bisq.common.proto.ProtoResolver;
import bisq.persistence.backup.MaxBackupSize;
import bisq.persistence.backup.RestoreService;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersistableStoreReaderWriterTests {

    @Test
    void readNotExistingStore(@TempDir Path tempDirPath) {
        Path storageFilePath = tempDirPath.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager, new RestoreService());

        Optional<TimestampStore> optionalTimestampStore = persistableStoreReaderWriter.read();
        assertThat(optionalTimestampStore).isEmpty();
    }

    @Test
    void writeAndReadStore(@TempDir Path tempDirPath) {
        var timestampStore = new TimestampStore();
        Map<String, Long> timestampsByProfileId = timestampStore.getTimestampsByProfileId();
        timestampsByProfileId.put("A", 1L);
        timestampsByProfileId.put("B", 2L);
        timestampsByProfileId.put("C", 3L);

        ProtoResolver<PersistableStore<?>> resolver = timestampStore.getResolver();
        PersistableStoreResolver.addResolver(resolver);

        Path storageFilePath = tempDirPath.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager, new RestoreService());
        persistableStoreReaderWriter.write(timestampStore);

        Optional<TimestampStore> readOptionalStore = persistableStoreReaderWriter.read();
        assertThat(readOptionalStore.isPresent()).isTrue();

        TimestampStore readStore = readOptionalStore.get();
        Map<String, Long> readTimestampsByProfileId = readStore.getTimestampsByProfileId();

        assertThat(readTimestampsByProfileId.get("A")).isEqualTo(1L);
        assertThat(readTimestampsByProfileId.get("B")).isEqualTo(2L);
        assertThat(readTimestampsByProfileId.get("C")).isEqualTo(3L);
    }

    @Test
    void readFromBackupAfterCorruption(@TempDir Path tempDirPath) throws Exception {
        // original store (will become the backup)
        var originalStore = new TimestampStore();
        Map<String, Long> originalMap = originalStore.getTimestampsByProfileId();
        originalMap.put("A", 1L);
        originalMap.put("B", 2L);
        originalMap.put("C", 3L);

        // modified store (will overwrite main file, backup keeps original)
        var modifiedStore = new TimestampStore();
        Map<String, Long> modifiedMap = modifiedStore.getTimestampsByProfileId();
        modifiedMap.put("A", 10L);
        modifiedMap.put("B", 20L);
        modifiedMap.put("C", 30L);

        // register resolver
        ProtoResolver<PersistableStore<?>> resolver = originalStore.getResolver();
        PersistableStoreResolver.addResolver(resolver);

        Path storageFilePath = tempDirPath.resolve("alice").resolve("db").resolve("test-protofile-store.protobuf");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath, MaxBackupSize.TEN_MB);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager, new RestoreService());

        // write original
        persistableStoreReaderWriter.write(originalStore);

        // write modified (main now has modified values)
        persistableStoreReaderWriter.write(modifiedStore);

        // corrupt the main store file
        FileMutatorUtils.writeToPath("corrupted-data".getBytes(), storeFileManager.getStoreFilePath());

        // read should recover from the backup (original values)
        Optional<TimestampStore> readOptionalStore = persistableStoreReaderWriter.read();
        assertThat(readOptionalStore).isPresent();

        TimestampStore readStore = readOptionalStore.get();
        Map<String, Long> readTimestampsByProfileId = readStore.getTimestampsByProfileId();
        assertThat(readTimestampsByProfileId.get("A")).isEqualTo(1L);
        assertThat(readTimestampsByProfileId.get("B")).isEqualTo(2L);
        assertThat(readTimestampsByProfileId.get("C")).isEqualTo(3L);

        // one corrupted file should be stored
        Path corruptedFilesAtReadPath = storageFilePath.getParent().resolve("corruptedFilesAtRead");
        long count;
        try (var stream = Files.list(corruptedFilesAtReadPath)) {
            count = stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(storageFilePath.getFileName().toString()))
                    .count();
        }
        assertEquals(1, count, "Expected exactly one file starting with " + storageFilePath.getFileName());
    }

    @Test
    void writeStoreTwice(@TempDir Path tempDirPath) {
        var timestampStore = new TimestampStore();
        Map<String, Long> timestampsByProfileId = timestampStore.getTimestampsByProfileId();
        timestampsByProfileId.put("A", 1L);
        timestampsByProfileId.put("B", 2L);
        timestampsByProfileId.put("C", 3L);

        Path storageFilePath = tempDirPath.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager, new RestoreService());
        persistableStoreReaderWriter.write(timestampStore);

        // Triggers rename
        persistableStoreReaderWriter.write(timestampStore);
    }

    @Test
    void writePropagatesSnapshotLockTimeout(@TempDir Path tempDirPath) {
        Path storageFilePath = tempDirPath.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<SnapshotLockTimeoutStore>(storeFileManager, new RestoreService());

        // Unlike other serialization failures, which write() logs and swallows, a SnapshotLockTimeoutException
        // must escape so that the caller does not treat the skipped write cycle as a success
        // (see RateLimitedPersistenceClient#persist).
        assertThatThrownBy(() -> persistableStoreReaderWriter.write(new SnapshotLockTimeoutStore()))
                .isInstanceOf(SnapshotLockTimeoutException.class);

        // The failed cycle must not have produced a (necessarily incomplete) store file.
        assertThat(Files.notExists(storageFilePath)).isTrue();
    }

    // Minimal store whose serialization fails like a trade store does when a live FSM transition holds the
    // FsmModel lock past the bounded snapshot wait (see BisqEasyTradeStore#getBuilder).
    private static final class SnapshotLockTimeoutStore implements PersistableStore<SnapshotLockTimeoutStore> {
        @Override
        public Message.Builder getBuilder(boolean serializeForHash) {
            throw new SnapshotLockTimeoutException(new FsmModel(State.FsmState.ERROR), 500);
        }

        @Override
        public SnapshotLockTimeoutStore getClone() {
            return this;
        }

        @Override
        public void applyPersisted(SnapshotLockTimeoutStore persisted) {
        }

        @Override
        public ProtoResolver<PersistableStore<?>> getResolver() {
            throw new UnsupportedOperationException("Not used in this test");
        }
    }
}
