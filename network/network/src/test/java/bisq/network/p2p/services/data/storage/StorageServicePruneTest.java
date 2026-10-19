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

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stores prune in the background from the initialization of the storage service on, not from its construction
 * (see docs/specifications/network/storage-lifecycle.md).
 */
class StorageServicePruneTest {
    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);

    @TempDir
    Path tempDir;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
    }

    @Test
    void initializeStartsPruningOfTheStores() throws Exception {
        StorageService storageService = new StorageService(new PersistenceService(tempDir));
        AuthenticatedDataStorageService store = storageService
                .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName())
                .join();
        Map<ByteArray, AuthenticatedDataRequest> map = store.getPersistableStore().getMap();
        AddAuthenticatedDataRequest expiredRequest = createExpiredRequest();
        map.put(expiredRequest.getStorageKey(), expiredRequest);
        try {
            storageService.initialize();

            long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
            while (!map.isEmpty()) {
                assertTrue(System.currentTimeMillis() < deadline, "The expired entry was not pruned in time");
                Thread.sleep(1);
            }
        } finally {
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
            storageService.shutdown();
        }
    }

    private static AddAuthenticatedDataRequest createExpiredRequest() throws GeneralSecurityException {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        DefaultAuthenticatedData data = new DefaultAuthenticatedData(new TestDistributedData("content",
                keyPair.getPublic()));
        long created = System.currentTimeMillis() - TestDistributedData.META_DATA.getTtl() - 1;
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(data,
                1,
                DigestUtil.hash(keyPair.getPublic().getEncoded()),
                created);
        AddAuthenticatedDataRequest request = new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), keyPair.getPrivate()),
                keyPair.getPublic().getEncoded(),
                keyPair.getPublic(),
                new byte[]{},
                0);
        assertTrue(request.isExpired());
        return request;
    }
}
