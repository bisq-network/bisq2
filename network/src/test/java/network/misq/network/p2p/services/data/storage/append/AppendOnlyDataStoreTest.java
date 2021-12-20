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

package network.misq.network.p2p.services.data.storage.append;

import network.misq.common.util.OsUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppendOnlyDataStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testAppend() throws IOException {
        MockAppendOnlyData data = new MockAppendOnlyData("test" + UUID.randomUUID());
        AppendOnlyDataStore store = new AppendOnlyDataStore(appDirPath, data.getMetaData());
        int previous = store.getMap().size();
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            data = new MockAppendOnlyData("test" + UUID.randomUUID());
            boolean result = store.append(data);
            assertTrue(result);
        }
        assertEquals(iterations + previous, store.getMap().size());
    }
}
