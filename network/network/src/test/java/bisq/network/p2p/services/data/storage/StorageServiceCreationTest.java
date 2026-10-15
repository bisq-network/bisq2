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

package bisq.network.p2p.services.data.storage;

import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.persistence.backup.MaxBackupSize;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

class StorageServiceCreationTest {
    @TempDir
    Path tempDir;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        StorableTypesRegistry.register(ConcurrentAppendOnlyData.class, ConcurrentAppendOnlyData.META_DATA);
    }

    @ParameterizedTest
    @EnumSource(value = StoreType.class, names = {"AUTHENTICATED_DATA_STORE", "MAILBOX_DATA_STORE", "APPEND_ONLY_DATA_STORE"})
    void concurrentFirstRequestsShareOneStoreAndPersistenceClient(StoreType type) throws Exception {
        PersistenceService persistenceService = spy(new PersistenceService(tempDir));
        AtomicInteger registrations = new AtomicInteger();
        CountDownLatch firstRegistration = new CountDownLatch(1);
        CountDownLatch secondRegistration = new CountDownLatch(1);
        CountDownLatch allowRegistration = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (registrations.incrementAndGet() == 1) {
                firstRegistration.countDown();
            } else {
                secondRegistration.countDown();
            }
            assertTrue(allowRegistration.await(5, SECONDS));
            return invocation.callRealMethod();
        }).when(persistenceService).getOrCreatePersistence(any(PersistenceClient.class), any(Path.class),
                anyString(), any(PersistableStore.class), any(MaxBackupSize.class));
        StorageService service = new StorageService(persistenceService);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> getStore(service, type));
            assertTrue(firstRegistration.await(5, SECONDS));
            CountDownLatch secondStarted = new CountDownLatch(1);
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return getStore(service, type);
            });
            assertTrue(secondStarted.await(5, SECONDS));
            // Let a competing construction enter persistence if creation is not serialized.
            secondRegistration.await(250, MILLISECONDS);
            allowRegistration.countDown();

            assertSame(first.get(5, SECONDS), second.get(5, SECONDS));
            assertEquals(1, registrations.get());
            assertEquals(1, persistenceService.getClients().size());
        } finally {
            allowRegistration.countDown();
            executor.shutdownNow();
            service.shutdown();
        }
    }

    private static DataStorageService<? extends DataRequest> getStore(StorageService service, StoreType type) {
        return switch (type) {
            case AUTHENTICATED_DATA_STORE -> service
                    .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName()).join();
            case MAILBOX_DATA_STORE -> service.getOrCreateMailboxDataStore(AckMessage.class.getSimpleName()).join();
            case APPEND_ONLY_DATA_STORE -> service
                    .getOrCreateAppendOnlyDataStore(ConcurrentAppendOnlyData.class.getSimpleName()).join();
            default -> throw new IllegalArgumentException("Unsupported store type " + type);
        };
    }

    private static class ConcurrentAppendOnlyData implements AppendOnlyData {
        private static final MetaData META_DATA =
                new MetaData(MetaData.TTL_10_DAYS, ConcurrentAppendOnlyData.class.getSimpleName());

        @Override
        public MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return META_DATA.getBuilder(serializeForHash);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return META_DATA.toProto(serializeForHash);
        }

        @Override
        public byte[] encodeCanonical(CanonicalEncoder encoder, EncodingPurpose purpose) {
            return META_DATA.encodeCanonical(encoder, purpose);
        }
    }
}
