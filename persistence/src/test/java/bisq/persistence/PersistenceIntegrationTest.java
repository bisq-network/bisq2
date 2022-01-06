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

import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class PersistenceIntegrationTest {
    record MockObject(int value) implements Serializable {
    }

    private String storageDirectory = OsUtils.getUserDataDir() + File.separator + "bisq_PersistenceTest";

    @Test
    public void testPersistence() {
        String fileName = "MockObject1";
        MockObject mockObject = new MockObject(1);
        FileUtils.deleteDirectory(new File(storageDirectory));
        Persistence<MockObject> persistence = new Persistence<>(storageDirectory, fileName);
        Optional<MockObject> persisted = persistence.read();
        assertTrue(persisted.isEmpty());

        boolean result = persistence.persistAsync(mockObject).join();
        assertTrue(result);

        persisted = persistence.readAsync().join();
        assertTrue(persisted.isPresent());
        assertEquals(mockObject, persisted.get());

        for (int i = 0; i < 10; i++) {
            persistence.persistAsync(new MockObject(i)).join();
        }
        assertEquals(mockObject, persisted.get());
    }

    @Test
    public void testRateLimitedPersistenceClient() {
        FileUtils.deleteDirectory(new File(storageDirectory));
        int maxWriteRateInMs = 100;
        MockRateLimitedPersistenceClient mockClient = new MockRateLimitedPersistenceClient(maxWriteRateInMs);
        assertFalse(mockClient.isDropped());
        for (int i = 0; i < 9; i++) {
            mockClient.updateAndPersist(i);
            log.error("{}", i);
            if (i == 0) {
                assertFalse(mockClient.isDropped());
            } else {
                assertTrue(mockClient.isDropped());
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignore) {
            }
        }
        assertTrue(mockClient.isDropped());
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignore) {
        }
        mockClient.updateAndPersist(99);
        assertFalse(mockClient.isDropped());

        for (int i = 100; i < 103; i++) {
            mockClient.updateAndPersist(i);
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignore) {
            }
        }
        assertTrue(mockClient.isDropped());
        // Cannot test shut down hook as test is terminated after System.exit, but with logs at Persistence and 
        // RateLimitedPersistenceClient.persistOnShutdown it is visible if persistence at shutdown works as expected.
        System.exit(0);
    }

    private class MockRateLimitedPersistenceClient extends RateLimitedPersistenceClient<MockObject> {
        @Getter
        private final Persistence<MockObject> persistence;
        private final int maxWriteRateInMs;
        @Getter
        MockObject mockObject = new MockObject(1);

        public MockRateLimitedPersistenceClient(int maxWriteRateInMs) {
            this.maxWriteRateInMs = maxWriteRateInMs;
            String fileName = "MockObject";
            persistence = new Persistence<>(storageDirectory, fileName);
        }

        public CompletableFuture<Boolean> updateAndPersist(int i) {
            mockObject = new MockObject(i);
            return persist();
        }

        @Override
        protected long getMaxWriteRateInMs() {
            return maxWriteRateInMs;
        }

        @Override
        public void applyPersisted(MockObject persisted) {
        }

        @Override
        public MockObject getCloneForPersistence() {
            return new MockObject(mockObject.value);
        }
    }
}
