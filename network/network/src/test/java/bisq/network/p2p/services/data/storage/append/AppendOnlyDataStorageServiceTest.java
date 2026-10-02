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

package bisq.network.p2p.services.data.storage.append;

import bisq.common.data.ByteArray;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppendOnlyDataStorageServiceTest {
    @TempDir
    Path tempDir;
    private AppendOnlyDataStorageService store;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestAppendOnlyData.class, TestAppendOnlyData.META_DATA);
    }

    @BeforeEach
    void setUp() {
        store = new AppendOnlyDataStorageService(new PersistenceService(tempDir),
                "appendOnlyTest",
                TestAppendOnlyData.class.getSimpleName());
    }

    @AfterEach
    void tearDown() {
        // The pending write would have to serialize all entries
        store.getPersistableStore().getMap().clear();
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    // The size limit applies only to entries under a new key. A full store still reports a request which it holds as
    // stored, not as a reached limit (see docs/specifications/network/storage-metadata.md).
    @Test
    void fullStoreReportsARequestWhichItHoldsAsAlreadyStored() {
        AddAppendOnlyDataRequest request = new AddAppendOnlyDataRequest(new TestAppendOnlyData("content"));
        assertTrue(store.add(request).isSuccess());
        Map<ByteArray, AddAppendOnlyDataRequest> map = store.getPersistableStore().getMap();
        // The limit counts entries, so the same request under other keys fills the map
        for (int i = 0; map.size() < DataStorageService.MAX_MAP_SIZE; i++) {
            map.put(new ByteArray(ByteBuffer.allocate(Integer.BYTES).putInt(i).array()), request);
        }

        DataStorageResult resultOfCopy = store.add(new AddAppendOnlyDataRequest(new TestAppendOnlyData("content")));
        DataStorageResult resultOfNewRequest = store.add(new AddAppendOnlyDataRequest(new TestAppendOnlyData("other")));

        assertTrue(resultOfCopy.isPayloadAlreadyStored(), resultOfCopy.toString());
        assertTrue(resultOfNewRequest.isMaxMapSizeReached(), resultOfNewRequest.toString());
        assertEquals(DataStorageService.MAX_MAP_SIZE, map.size());
    }

    private static final class TestAppendOnlyData implements AppendOnlyData {
        private static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS,
                TestAppendOnlyData.class.getSimpleName());
        // network.proto: message MetaData { ...; string className = 4; }
        private static final CanonicalSchema<TestAppendOnlyData> DEFAULT_SCHEMA =
                CanonicalSchema.<TestAppendOnlyData>newBuilder()
                        .string(4, data -> data.content)
                        .build();

        private final String content;

        private TestAppendOnlyData(String content) {
            this.content = content;
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(content);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            return canonicalEncoder.encode(this, DEFAULT_SCHEMA, encodingPurpose);
        }
    }
}
