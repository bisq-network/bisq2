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

package bisq.user.profile;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterEntry;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import bisq.user.identity.UserIdentityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Nodes up to version 2.1.13 refresh their user profile with signature V1 over the hash of the data, the bytes which
 * signature V1 of a removal signs. A peer which receives such a refresh can turn it into a removal of the profile with
 * any sequence number. The store of user profiles accepts and keeps only removals with signature V2 (see
 * docs/specifications/network/storage-request-signatures.md).
 */
class UserProfileRemovalSignatureTest {
    private static final HashCashProofOfWorkService PROOF_OF_WORK_SERVICE = new HashCashProofOfWorkService();
    // Before the cutoff date of signature V1, when a store accepts the removals of other types without signature V2
    private static final long BEFORE_CUTOFF = SignedRequest.SIGNATURE_V1_CUTOFF_DATE.getTime() - TimeUnit.DAYS.toMillis(1);

    @TempDir
    Path tempDir;

    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private AuthenticatedDataStorageService store;
    private AuthenticatedData userProfile;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(UserProfile.class, UserProfile.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "userProfiles",
                UserProfile.class.getSimpleName(),
                () -> BEFORE_CUTOFF);
        userProfile = new DefaultAuthenticatedData(createUserProfile(new PubKey(ownerKeyPair.getPublic(), "key")));
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files.
    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, Integer.MAX_VALUE})
    void removalWithoutSignatureV2IsRefusedAndTheProfileStays(int sequenceNumber)
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(store, userProfile, ownerKeyPair);
        assertTrue(store.add(add).isSuccess());
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(store, userProfile, ownerKeyPair);

        DataStorageResult result = store.remove(withoutSignatureV2(removal, sequenceNumber));

        assertTrue(result.isSignatureV2Missing(), result.toString());
        assertSame(add, store.getPersistableStore().getMap().get(add.getStorageKey()));
        // The removal of the owner with signature V2 still removes the profile
        assertTrue(store.remove(removal).isSuccess());
        assertSame(removal, store.getPersistableStore().getMap().get(add.getStorageKey()));
    }

    // Nodes of earlier versions accept the removal and then reject the add request of the profile, whose sequence
    // number is lower. This node does not send them the add request, which they would reject in every inventory
    // response (rule 12 of docs/specifications/network/inventory-hash-set-filter.md).
    @ParameterizedTest
    @ValueSource(ints = {2, Integer.MAX_VALUE})
    void inventoryResponseToRequesterWithTheRefusedRemovalDoesNotContainTheProfile(int sequenceNumber)
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(store, userProfile, ownerKeyPair);
        assertTrue(store.add(add).isSuccess());
        RemoveAuthenticatedDataRequest removal = withoutSignatureV2(
                RemoveAuthenticatedDataRequest.from(store, userProfile, ownerKeyPair), sequenceNumber);
        assertTrue(store.remove(removal).isSignatureV2Missing());
        HashSetFilter filterOfNodeWithTheRemoval = new HashSetFilter(InventoryFilterType.HASH_SET, new ArrayList<>(
                List.of(new HashSetFilterEntry(removal.getStorageKeyV1().getBytes(), sequenceNumber))));
        HashSetFilter filterOfNodeWithoutTheProfile = new HashSetFilter(InventoryFilterType.HASH_SET,
                new ArrayList<>());

        Inventory inventory = filterServiceForEarlierVersions().createInventory(filterOfNodeWithTheRemoval);

        assertTrue(inventory.getEntries().isEmpty());
        assertEquals(List.of(add),
                filterServiceForEarlierVersions().createInventory(filterOfNodeWithoutTheProfile).getEntries());
    }

    // A store of version 2.1.13 can hold such a removal when the node upgrades
    @Test
    void persistedRemovalWithoutSignatureV2IsPruned() throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest removal = withoutSignatureV2(
                RemoveAuthenticatedDataRequest.from(store, userProfile, ownerKeyPair), 2);
        Map<?, ?> map = store.getPersistableStore().getMap();
        store.getPersistableStore().getMap().put(removal.getStorageKey(), removal);

        store.prunePersisted(store.getPersistableStore());

        assertTrue(map.isEmpty());
    }

    // The service which answers the inventory requests of nodes of earlier versions, for the store of user profiles
    private HashSetFilterService filterServiceForEarlierVersions() {
        StorageService storageService = mock(StorageService.class);
        when(storageService.getAuthenticatedDataStoreMaps())
                .thenAnswer(invocation -> Stream.of(store.getPersistableStore().getMap()));
        when(storageService.getMailboxStoreMaps()).thenAnswer(invocation -> Stream.empty());
        when(storageService.getAddAppendOnlyDataStoreMaps()).thenAnswer(invocation -> Stream.empty());
        return new HashSetFilterService(storageService, 2_000_000, InventoryFilterType.HASH_SET);
    }

    // As a peer creates it from a refresh of version 2.1.13: signature V1 only, and a sequence number which signature V1
    // does not cover
    private static RemoveAuthenticatedDataRequest withoutSignatureV2(RemoveAuthenticatedDataRequest request,
                                                                     int sequenceNumber)
            throws UnresolvableProtobufException {
        return RemoveAuthenticatedDataRequest.fromProto(request.toValueProto(false).toBuilder()
                .setVersion(1)
                .clearSignatureV2()
                .setSequenceNumber(sequenceNumber)
                .build());
    }

    private static UserProfile createUserProfile(PubKey pubKey) {
        ProofOfWork proofOfWork = PROOF_OF_WORK_SERVICE.mint(pubKey.getHash(), null,
                UserIdentityService.MINT_NYM_DIFFICULTY);
        Address address = Address.from("127.0.0.1", 1000);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                pubKey);
        return new UserProfile(1, "Alice", proofOfWork, 0, networkId, "terms", "statement", "1.0.0");
    }
}
