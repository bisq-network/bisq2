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

package network.misq.common.persistence;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.FileUtils;
import network.misq.common.util.OsUtils;
import network.misq.persistence.Persistable;
import network.misq.persistence.Persistence;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class PersistenceIntegrationTest {
    @EqualsAndHashCode
    @Getter
    static class MockObject implements Persistable {
        private final int index;
        private final ArrayList<Integer> list = new ArrayList<>();

        public MockObject(int index) {
            this.index = index;
        }
    }

    @Test
    public void testPersistence() throws IOException {
        String storageDirectory = OsUtils.getUserDataDir() + File.separator + "misq_PersistenceTest";
        FileUtils.makeDirs(storageDirectory);
        String fileName = "MockObject";
        String storagePath = storageDirectory + File.separator + fileName;
        MockObject mockObject = new MockObject(1);
        Persistence persistence = new Persistence(storageDirectory, fileName, mockObject);
        for (int i = 0; i < 100; i++) {
            mockObject.list.add(i);
            persistence.persist();
        }
        MockObject mockObject2 = (MockObject) Persistence.read(storagePath);
        assertEquals(mockObject.getIndex(), mockObject2.getIndex());
        assertEquals(mockObject.getList(), mockObject2.getList());
    }
}
