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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.data.ByteArray;
import bisq.common.util.ByteArrayUtils;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageKeyV2;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Storage key V2 of authenticated data. Remove and refresh requests carry only the hash of the data, so the add, remove
 * and refresh requests of one owner must compute the same key from it.
 */
class StorageKeyV2Test {
    private static final String STORE_KEY = TestDistributedData.class.getSimpleName();

    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService store;
    private KeyPair ownerKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "store",
                STORE_KEY);
        ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    }

    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @Test
    void preImageHasTheSpecifiedLayout() {
        byte[] dataHash = DigestUtil.hash("data".getBytes(StandardCharsets.UTF_8));
        byte[] publicKeyBytes = ownerKeyPair.getPublic().getEncoded();

        byte[] expected = ByteArrayUtils.concat("bisq.AuthenticatedDataRequest.storageKeyV2".getBytes(StandardCharsets.US_ASCII),
                new byte[]{0},
                dataHash,
                publicKeyBytes);
        assertArrayEquals(expected,
                StorageKeyV2.createPreImage(StorageKeyV2.AUTHENTICATED_DATA_TAG, dataHash, publicKeyBytes));
        assertEquals(new ByteArray(DigestUtil.RIPEMD160(expected)),
                StorageKeyV2.create(StorageKeyV2.AUTHENTICATED_DATA_TAG, dataHash, publicKeyBytes));

        // The data hash has a fixed length. Otherwise the layout would be ambiguous.
        assertThrows(IllegalArgumentException.class,
                () -> StorageKeyV2.createPreImage(StorageKeyV2.AUTHENTICATED_DATA_TAG, new byte[32], publicKeyBytes));
    }

    // For AuthorizedData, the STORAGE_KEY encoding contains the signature, but serializeForHash(), which remove and
    // refresh requests hash, does not. Both authenticated data types must give the same key for all request types.
    @Test
    void addRemoveAndRefreshRequestsOfOneOwnerHaveTheSameStorageKey() throws GeneralSecurityException {
        for (AuthenticatedData authenticatedData : List.of(
                new DefaultAuthenticatedData(new TestDistributedData("content", ownerKeyPair.getPublic())),
                getAuthorizedDataWithSignature())) {
            AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
            RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
            RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

            String description = authenticatedData.getClass().getSimpleName();
            assertEquals(addRequest.getStorageKey(), removeRequest.getStorageKey(), description);
            assertEquals(addRequest.getStorageKey(), refreshRequest.getStorageKey(), description);
            assertEquals(addRequest.getStorageKeyV1(), removeRequest.getStorageKeyV1(), description);
            assertEquals(addRequest.getStorageKeyV1(), refreshRequest.getStorageKeyV1(), description);
        }
    }

    @Test
    void storageKeyContainsTheOwnerPublicKey() throws GeneralSecurityException {
        AuthenticatedData authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content",
                ownerKeyPair.getPublic()));
        AddAuthenticatedDataRequest ownerRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        AddAuthenticatedDataRequest otherRequest = AddAuthenticatedDataRequest.from(store,
                authenticatedData,
                KeyGeneration.generateDefaultEcKeyPair());

        assertEquals(ownerRequest.getStorageKeyV1(), otherRequest.getStorageKeyV1());
        assertNotEquals(ownerRequest.getStorageKey(), otherRequest.getStorageKey());
    }

    // The requests find the sequence number of the stored entry under the key with which the store holds it
    @Test
    void republishAndRemoveUseTheNextSequenceNumberOfTheStoredEntry() throws GeneralSecurityException {
        AuthenticatedData authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content",
                ownerKeyPair.getPublic()));
        assertTrue(store.add(AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair)).isSuccess());

        AddAuthenticatedDataRequest republished = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        assertEquals(2, republished.getSequenceNumber());
        assertTrue(store.add(republished).isSuccess());

        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        assertEquals(3, removeRequest.getSequenceNumber());
        var result = store.remove(removeRequest);
        assertTrue(result.isSuccess(), result.toString());
        assertFalse(result.isNoEntry(), result.toString());
        assertSame(removeRequest, store.getPersistableStore().getMap().get(removeRequest.getStorageKey()));
    }

    // Earlier versions persisted the entries under storage key V1
    @Test
    void persistedEntriesAreStoredUnderTheirCurrentStorageKey() throws GeneralSecurityException {
        AuthenticatedData authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content",
                ownerKeyPair.getPublic()));
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        DataStore<AuthenticatedDataRequest> persisted = store.getPersistableStore();
        persisted.getMap().put(addRequest.getStorageKeyV1(), addRequest);

        DataStore<AuthenticatedDataRequest> preProcessed = store.preProcessPersisted(persisted);

        assertEquals(Map.of(addRequest.getStorageKey(), addRequest), preProcessed.getMap());
    }

    private AuthorizedData getAuthorizedDataWithSignature() {
        return new AuthorizedData(new TestAuthorizedDistributedData("content"),
                Optional.of(new byte[70]),
                ownerKeyPair.getPublic());
    }
}
