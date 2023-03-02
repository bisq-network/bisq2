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

import bisq.common.proto.ProtoResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class PersistableStoreReaderWriterTests {

    @Test
    void readNotExistingStore(@TempDir Path tempDir) {
        Path storageFilePath = tempDir.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager);

        Optional<TimestampStore> optionalTimestampStore = persistableStoreReaderWriter.read();
        assertThat(optionalTimestampStore).isEmpty();
    }

    @Test
    void writeAndReadStore(@TempDir Path tempDir) {
        var timestampStore = new TimestampStore();
        Map<String, Long> timestampsByProfileId = timestampStore.getTimestampsByProfileId();
        timestampsByProfileId.put("A", 1L);
        timestampsByProfileId.put("B", 2L);
        timestampsByProfileId.put("C", 3L);

        ProtoResolver<PersistableStore<?>> resolver = timestampStore.getResolver();
        PersistableStoreResolver.addResolver(resolver);

        Path storageFilePath = tempDir.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager);
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
    void writeStoreTwice(@TempDir Path tempDir) {
        var timestampStore = new TimestampStore();
        Map<String, Long> timestampsByProfileId = timestampStore.getTimestampsByProfileId();
        timestampsByProfileId.put("A", 1L);
        timestampsByProfileId.put("B", 2L);
        timestampsByProfileId.put("C", 3L);

        Path storageFilePath = tempDir.resolve("protoFile");
        var storeFileManager = new PersistableStoreFileManager(storageFilePath);
        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager);
        persistableStoreReaderWriter.write(timestampStore);

        // Triggers rename
        persistableStoreReaderWriter.write(timestampStore);
    }

    @Test
    void failRenameTempFileToCurrentFile(@TempDir Path tempDir) throws IOException {
        var timestampStore = new TimestampStore();
        Map<String, Long> timestampsByProfileId = timestampStore.getTimestampsByProfileId();
        timestampsByProfileId.put("A", 1L);
        timestampsByProfileId.put("B", 2L);
        timestampsByProfileId.put("C", 3L);

        Path storeFilePath = tempDir.resolve("protoFile");
        PersistableStoreFileManagerTests.createEmptyFile(storeFilePath);

        var storeFileManagerImpl = new PersistableStoreFileManager(storeFilePath);
        PersistableStoreFileManager storeFileManager = spy(storeFileManagerImpl);
        doThrow(new IOException("Rename failed.")).when(storeFileManager).renameTempFileToCurrentFile();

        var persistableStoreReaderWriter = new PersistableStoreReaderWriter<TimestampStore>(storeFileManager);
        persistableStoreReaderWriter.write(timestampStore);

        assertThat(storeFilePath).exists();

        Path backupFilePath = tempDir.resolve("backup_protoFile");
        assertThat(backupFilePath).doesNotExist();
    }
}
