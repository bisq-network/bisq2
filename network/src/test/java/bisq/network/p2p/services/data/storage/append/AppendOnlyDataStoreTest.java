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

import bisq.common.util.OsUtils;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static bisq.network.p2p.services.data.storage.StorageService.StoreType.APPEND_ONLY_DATA_STORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppendOnlyDataStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "bisq_StorageTest";

    @Test
    public void testAppend() throws IOException {
        MockAppendOnlyPayload data = new MockAppendOnlyPayload("test" + UUID.randomUUID());
        PersistenceService persistenceService = new PersistenceService(appDirPath);
        AppendOnlyDataStore store = new AppendOnlyDataStore(persistenceService, APPEND_ONLY_DATA_STORE.getStoreName(),
                data.getMetaData().getFileName());
        store.readPersisted().join();
        int previous = store.getClone().size();
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            data = new MockAppendOnlyPayload("test" + UUID.randomUUID());
            boolean result = store.add(new AddAppendOnlyDataRequest(data)).isSuccess();
            assertTrue(result);
        }
        assertEquals(iterations + previous, store.getClone().size());
    }
}
