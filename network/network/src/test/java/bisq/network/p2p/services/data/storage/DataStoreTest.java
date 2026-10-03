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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A store keeps, for a removal which replaced an add request, the sequence number of that add request (see
 * docs/specifications/network/storage-request-ordering.md). The DataStore persists this number with the entry of the
 * removal.
 */
class DataStoreTest {
    private static final String STORE_KEY = TestDistributedData.class.getSimpleName();

    @TempDir
    Path tempDir;

    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private AuthenticatedDataStorageService store;
    private DataStore<AuthenticatedDataRequest> dataStore;
    private RemoveAuthenticatedDataRequest removal;
    private RemoveAuthenticatedDataRequest otherRemoval;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "store",
                STORE_KEY);
        dataStore = store.getPersistableStore();
        removal = RemoveAuthenticatedDataRequest.from(store, createData("content"), ownerKeyPair);
        otherRemoval = RemoveAuthenticatedDataRequest.from(store, createData("other content"), ownerKeyPair);
        dataStore.getMap().put(removal.getStorageKey(), removal);
        dataStore.getMap().put(otherRemoval.getStorageKey(), otherRemoval);
        dataStore.getReplacedAddSequenceNrs().put(removal.getStorageKey(), 5);
    }

    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @Test
    void replacedAddSequenceNrIsPersistedWithItsEntry()
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        DataStore<?> persisted = (DataStore<?>) DataStore.fromProto(
                bisq.network.protobuf.DataStore.parseFrom(dataStore.serialize()));

        assertEquals(dataStore.getMap(), persisted.getMap());
        assertEquals(Map.of(removal.getStorageKey(), 5), persisted.getReplacedAddSequenceNrs());
    }

    @Test
    void sequenceNumberWithoutEntryIsNotPersisted()
            throws InvalidProtocolBufferException, UnresolvableProtobufException {
        ByteArray keyWithoutEntry = new ByteArray(new byte[20]);
        dataStore.getReplacedAddSequenceNrs().put(keyWithoutEntry, 7);

        DataStore<?> persisted = (DataStore<?>) DataStore.fromProto(
                bisq.network.protobuf.DataStore.parseFrom(dataStore.serialize()));

        assertEquals(Map.of(removal.getStorageKey(), 5), persisted.getReplacedAddSequenceNrs());
    }

    @Test
    void cloneAndAppliedStoreContainTheSequenceNumbers() {
        DataStore<AuthenticatedDataRequest> clone = dataStore.getClone();
        DataStore<AuthenticatedDataRequest> mutableClone = dataStore.getMutableClone();
        DataStore<AuthenticatedDataRequest> applied = new DataStore<>();
        applied.getReplacedAddSequenceNrs().put(otherRemoval.getStorageKey(), 9);

        applied.applyPersisted(clone);

        Map<ByteArray, Integer> expected = Map.of(removal.getStorageKey(), 5);
        assertEquals(expected, clone.getReplacedAddSequenceNrs());
        assertEquals(expected, mutableClone.getReplacedAddSequenceNrs());
        assertEquals(expected, applied.getReplacedAddSequenceNrs());
        assertEquals(dataStore.getMap(), applied.getMap());
    }

    // The store drops entries when it loads the persisted data, for example expired entries. It drops their sequence
    // numbers as well.
    @Test
    void loadingKeepsOnlyTheSequenceNumbersOfTheRemainingEntries() {
        AuthenticatedDataStorageService storeWhichPrunesTheRemoval = new AuthenticatedDataStorageService(
                new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "pruning",
                STORE_KEY) {
            @Override
            protected boolean isToPrune(AuthenticatedDataRequest dataRequest) {
                return dataRequest.equals(removal);
            }
        };

        DataStore<AuthenticatedDataRequest> preProcessed = storeWhichPrunesTheRemoval.preProcessPersisted(dataStore);
        assertEquals(Map.of(removal.getStorageKey(), 5), preProcessed.getReplacedAddSequenceNrs());

        DataStore<AuthenticatedDataRequest> pruned = storeWhichPrunesTheRemoval.prunePersisted(preProcessed);

        assertEquals(Map.of(otherRemoval.getStorageKey(), otherRemoval), pruned.getMap());
        assertTrue(pruned.getReplacedAddSequenceNrs().isEmpty());
    }

    private AuthenticatedData createData(String content) {
        return new DefaultAuthenticatedData(new TestDistributedData(content, ownerKeyPair.getPublic()));
    }
}
