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

package bisq.network.p2p.services.data.inventory;

import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.CLOCK_BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The filter of an inventory request lists the requests which the requester holds. If it lists none of the requests of
 * a response which reached the size limit, the next filter is the same for the responder, and so is the next response
 * (see docs/specifications/network/inventory-hash-set-filter.md).
 */
class InventoryProgressTest {
    @TempDir
    Path tempDir;

    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private DataService requester;
    private DataService responder;
    private AuthenticatedDataStorageService storeOfResponder;
    private final List<AuthenticatedData> dataOfResponder = new ArrayList<>();

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
    }

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        requester = new DataService(new PersistenceService(tempDir.resolve("requester")), CLOCK_BEFORE_CUTOFF);
        responder = new DataService(new PersistenceService(tempDir.resolve("responder")), CLOCK_BEFORE_CUTOFF);
        storeOfResponder = responder.getStorageService()
                .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName())
                .join();
        for (String content : List.of("a", "b", "c")) {
            AuthenticatedData data = new DefaultAuthenticatedData(new TestDistributedData(content,
                    ownerKeyPair.getPublic()));
            AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(storeOfResponder, data, ownerKeyPair);
            assertTrue(responder.processAddDataRequest(add, false).join().isSuccess());
            dataOfResponder.add(data);
        }
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files in
    // it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        shutdown(requester);
        shutdown(responder);
    }

    @Test
    void storedEntriesOfResponseAreCounted() {
        Inventory inventory = createInventoryOfResponder(InventoryFilterType.HASH_SET_V2, Integer.MAX_VALUE);

        assertEquals(3, InventoryService.storeEntries(requester, inventory).join());
        // The requester holds them now
        assertEquals(0, InventoryService.storeEntries(requester, inventory).join());
    }

    // For example, the requester got the removals of the owner, and the responder missed them. The responder sends the
    // earlier add requests, which the requester rejects.
    @Test
    void fullResponseWhichTheRequesterRejectsIsSentAgain() throws GeneralSecurityException {
        for (AuthenticatedData data : dataOfResponder) {
            RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(storeOfResponder, data,
                    ownerKeyPair);
            assertTrue(requester.processRemoveDataRequest(removal, false).join().isSuccess());
        }
        int maxSize = spaceForOneAddRequest();

        Inventory inventory = createInventoryOfResponder(InventoryFilterType.HASH_SET_V2, maxSize);

        assertTrue(inventory.isMaxSizeReached());
        assertFalse(inventory.getEntries().isEmpty());
        assertEquals(0, InventoryService.storeEntries(requester, inventory).join());
        assertFalse(createFilterServiceOfRequester(InventoryFilterType.HASH_SET_V2)
                .isAnyRequestInFilter(inventory.getEntries()));
        assertEquals(inventory.getEntries(),
                createInventoryOfResponder(InventoryFilterType.HASH_SET_V2, maxSize).getEntries());
    }

    // Several initial requests use the same filter, so two peers with the same data send the same response. The
    // requester stores the requests of the first response and none of the second one. Its filter lists them now, so the
    // next response of each peer differs.
    @ParameterizedTest
    @EnumSource(value = InventoryFilterType.class, names = {"HASH_SET", "HASH_SET_V2"})
    void filterListsTheEntriesOfAResponseWhichAnotherPeerDeliveredFirst(InventoryFilterType inventoryFilterType) {
        int maxSize = spaceForOneAddRequest();
        Inventory inventory = createInventoryOfResponder(inventoryFilterType, maxSize);
        HashSetFilterService filterServiceOfRequester = createFilterServiceOfRequester(inventoryFilterType);
        assertTrue(inventory.isMaxSizeReached());
        assertEquals(1, inventory.getEntries().size());
        assertFalse(filterServiceOfRequester.isAnyRequestInFilter(inventory.getEntries()));

        assertEquals(1, InventoryService.storeEntries(requester, inventory).join());
        // The same response of the other peer
        assertEquals(0, InventoryService.storeEntries(requester, inventory).join());

        assertTrue(filterServiceOfRequester.isAnyRequestInFilter(inventory.getEntries()));
        assertNotEquals(inventory.getEntries(), createInventoryOfResponder(inventoryFilterType, maxSize).getEntries());
    }

    // The inventory which the responder sends for the filter of the requester. Both use the same filter type, so the
    // filter entries and the inventory use the same storage key version.
    private Inventory createInventoryOfResponder(InventoryFilterType inventoryFilterType, int maxSize) {
        return new HashSetFilterService(responder.getStorageService(), maxSize, inventoryFilterType)
                .createInventory(createFilterServiceOfRequester(inventoryFilterType).getFilter());
    }

    // Each add request of the responder fits alone, and no two fit together. The add requests differ in size by a few
    // bytes, because the length of a signature varies. A request which is larger than the size limit is skipped, and
    // does not end the response (rule 12 of docs/specifications/network/inventory-hash-set-filter.md), so the limit is
    // the size of the largest one.
    private int spaceForOneAddRequest() {
        return storeOfResponder.getPersistableStore().getMap().values().stream()
                .mapToInt(AuthenticatedDataRequest::getSerializedSize)
                .max()
                .orElseThrow();
    }

    private HashSetFilterService createFilterServiceOfRequester(InventoryFilterType inventoryFilterType) {
        return new HashSetFilterService(requester.getStorageService(), Integer.MAX_VALUE, inventoryFilterType);
    }

    private static void shutdown(DataService dataService) {
        dataService.getStorageService().getStoresByStoreType(StoreType.ALL)
                .forEach(InventoryProgressTest::waitForPendingWrites);
        dataService.shutdown();
    }

    private static <T extends DataRequest> void waitForPendingWrites(DataStorageService<T> store) {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }
}
