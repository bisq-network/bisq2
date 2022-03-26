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
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static bisq.network.p2p.services.data.storage.StorageService.StoreType.APPEND_ONLY_DATA_STORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppendOnlyDataStorageServiceTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "bisq_StorageTest";

    @BeforeEach
    public void setup() {
        FileUtils.deleteDirectory(new File(appDirPath));
    }
    @Test
    public void testAppend() {
        MockAppendOnlyData data = new MockAppendOnlyData("test" + UUID.randomUUID());
        PersistenceService persistenceService = new PersistenceService(appDirPath);
        AppendOnlyDataStorageService store = new AppendOnlyDataStorageService(persistenceService, APPEND_ONLY_DATA_STORE.getStoreName(),
                data.getMetaData().getFileName());
        store.readPersisted().join();
        Map<ByteArray, AddAppendOnlyDataRequest> map = store.getPersistableStore().getClone().getMap();
        int previous = map.size();
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            data = new MockAppendOnlyData("test" + UUID.randomUUID());
            boolean result = store.add(new AddAppendOnlyDataRequest(data)).isSuccess();
            assertTrue(result);
        }
        assertEquals(iterations + previous, store.getPersistableStore().getClone().getMap().size());
    }
}
