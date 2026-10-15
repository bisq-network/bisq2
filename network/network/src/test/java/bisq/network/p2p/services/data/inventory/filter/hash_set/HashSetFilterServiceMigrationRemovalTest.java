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

package bisq.network.p2p.services.data.inventory.filter.hash_set;

import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.AuthorizedDataPublisherMigration;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestAuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.CLOCK_BEFORE_CUTOFF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// A requester of an earlier version (HASH_SET) holds one request per storage key V1. It cannot store a removal for
// which the responder holds an add request with the same storage key V1 and a higher sequence number. See rule 11 of
// docs/specifications/network/inventory-hash-set-filter.md.
class HashSetFilterServiceMigrationRemovalTest {
    private static final int MAX_SIZE = 2_000_000;

    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService store;
    private StorageService storageService;
    // The authorized key pair of the oracle node, which owns the data
    private KeyPair authorizedKeyPair;
    // The key pair of the network identity of the oracle node, with which earlier versions published the data
    private KeyPair identityKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "inventory",
                TestAuthorizedDistributedData.class.getSimpleName(),
                CLOCK_BEFORE_CUTOFF);
        storageService = mock(StorageService.class);
        when(storageService.getAuthenticatedDataStoreMaps())
                .thenAnswer(invocation -> Stream.of(store.getPersistableStore().getMap()));
        when(storageService.getMailboxStoreMaps()).thenAnswer(invocation -> Stream.empty());
        when(storageService.getAddAppendOnlyDataStoreMaps()).thenAnswer(invocation -> Stream.empty());
        authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    // The requester holds the entry of the authorized key pair with sequence number n + 2, under storage key V1. Before
    // rule 11, every response contained the removal n + 1 again.
    @Test
    void doesNotSendMigrationRemovalToRequesterWithTheNewEntry() throws GeneralSecurityException {
        Migration migration = migrate("content", 5);
        var filterOfEarlierVersion = new HashSetFilter(InventoryFilterType.HASH_SET,
                new ArrayList<>(List.of(entryV1(migration.newEntry()))));

        Inventory inventory = filterService(InventoryFilterType.HASH_SET).createInventory(filterOfEarlierVersion);

        assertThat(inventory.getEntries()).isEmpty();
        assertThat(inventory.isMaxSizeReached()).isFalse();
    }

    // The requester missed the migration and holds the entry of the network identity. The new entry replaces it, so it
    // does not need the removal.
    @Test
    void sendsOnlyTheNewEntryToRequesterWhichMissedTheMigration() throws GeneralSecurityException {
        Migration migration = migrate("content", 5);
        var filterOfEarlierVersion = new HashSetFilter(InventoryFilterType.HASH_SET,
                new ArrayList<>(List.of(entryV1(migration.entryOfEarlierVersion()))));

        Inventory inventory = filterService(InventoryFilterType.HASH_SET).createInventory(filterOfEarlierVersion);

        assertThat(entries(inventory)).containsExactly(migration.newEntry());
    }

    // Requesters of this version hold one request per storage key V2, so they store the removal and need it
    @Test
    void sendsMigrationRemovalToRequesterOfThisVersion() throws GeneralSecurityException {
        Migration migration = migrate("content", 5);
        var emptyFilter = new HashSetFilter(InventoryFilterType.HASH_SET_V2, new ArrayList<>());

        Inventory inventory = filterService(InventoryFilterType.HASH_SET_V2).createInventory(emptyFilter);

        assertThat(entries(inventory)).containsExactlyInAnyOrder(migration.newEntry(), migration.removal());
    }

    // A removal without an add request with a higher sequence number is a removal which the requester can store
    @Test
    void sendsRemovalOfTheOwnerToRequesterOfEarlierVersion() throws GeneralSecurityException {
        AuthorizedData data = authorize("removed by owner");
        AddAuthenticatedDataRequest entry = createRequest(data, authorizedKeyPair, 3);
        assertThat(store.add(entry).isSuccess()).isTrue();
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(store, data, authorizedKeyPair);
        assertThat(store.remove(removal).isSuccess()).isTrue();
        var filterOfEarlierVersion = new HashSetFilter(InventoryFilterType.HASH_SET,
                new ArrayList<>(List.of(entryV1(entry))));

        Inventory inventory = filterService(InventoryFilterType.HASH_SET).createInventory(filterOfEarlierVersion);

        assertThat(entries(inventory)).containsExactly(removal);
    }

    // With many migrated entries, the removals alone filled every response of a requester of an earlier version, so it
    // repeated its request without end and received no mailbox data (inventory-migration-removals-loop)
    @Test
    void responseToRequesterWithAllNewEntriesStaysEmptyForManyMigratedEntries() throws GeneralSecurityException {
        List<HashSetFilterEntry> entries = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            entries.add(entryV1(migrate("content " + i, 1).newEntry()));
        }

        Inventory inventory = filterService(InventoryFilterType.HASH_SET)
                .createInventory(new HashSetFilter(InventoryFilterType.HASH_SET, entries));

        assertThat(inventory.getEntries()).isEmpty();
    }

    private HashSetFilterService filterService(InventoryFilterType inventoryFilterType) {
        return new HashSetFilterService(storageService, MAX_SIZE, inventoryFilterType);
    }

    private Migration migrate(String content, int sequenceNumber) throws GeneralSecurityException {
        AddAuthenticatedDataRequest entryOfEarlierVersion = createRequest(authorize(content), identityKeyPair,
                sequenceNumber);
        assertThat(store.add(entryOfEarlierVersion).isSuccess()).isTrue();
        List<AuthenticatedDataRequest> requests = AuthorizedDataPublisherMigration.migrate(store,
                identityKeyPair,
                authorizedKeyPair);
        assertThat(requests).hasSize(2);
        return new Migration(entryOfEarlierVersion,
                (AddAuthenticatedDataRequest) requests.get(0),
                (RemoveAuthenticatedDataRequest) requests.get(1));
    }

    private static List<DataRequest> entries(Inventory inventory) {
        return new ArrayList<>(inventory.getEntries());
    }

    // The entry which a node of an earlier version computes for a request which it holds
    private static HashSetFilterEntry entryV1(DataRequest request) {
        return new HashSetFilterEntry(request.getStorageKeyV1().getBytes(),
                ((AuthenticatedDataRequest) request).getSequenceNumber());
    }

    private AuthorizedData authorize(String content) throws GeneralSecurityException {
        TestAuthorizedDistributedData data = TestAuthorizedDistributedData.withLegacyOwners(content);
        byte[] signature = SignatureUtil.sign(data.serializeForHash(), authorizedKeyPair.getPrivate());
        return new AuthorizedData(data, Optional.of(signature), authorizedKeyPair.getPublic());
    }

    // An add request as nodes of earlier versions created it: without signature V2
    private static AddAuthenticatedDataRequest createRequest(AuthorizedData data,
                                                             KeyPair publisherKeyPair,
                                                             int sequenceNumber) throws GeneralSecurityException {
        long created = Math.min(System.currentTimeMillis(), AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime() - 1);
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(data,
                sequenceNumber,
                DigestUtil.hash(publisherKeyPair.getPublic().getEncoded()),
                created);
        return new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), publisherKeyPair.getPrivate()),
                publisherKeyPair.getPublic().getEncoded(),
                publisherKeyPair.getPublic(),
                new byte[]{},
                0);
    }

    private record Migration(AddAuthenticatedDataRequest entryOfEarlierVersion,
                             AddAuthenticatedDataRequest newEntry,
                             RemoveAuthenticatedDataRequest removal) {
    }
}
