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

import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageData;
import bisq.network.p2p.services.data.storage.StorageService;
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

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.CLOCK_BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applications get only the data which its owner published (see docs/specifications/network/storage-ownership.md).
 * During the transition, the store also accepts authorized data which oracle nodes of earlier versions published with
 * the key pair of their network identity, and copies of anyone.
 * <p>
 * Entries of earlier versions have no signature V2. The node accepts them only before
 * SignedRequest.SIGNATURE_V1_CUTOFF_DATE, so the node of these tests has a time before that date.
 */
class OwnerPublicationTest {
    @TempDir
    Path appDataDir;

    private StorageService storageService;
    private AuthenticatedDataStorageService store;
    private final List<StorageData> addedData = new ArrayList<>();
    private final List<StorageData> removedData = new ArrayList<>();
    // The authorized key pair of the oracle node, which owns the data
    private KeyPair authorizedKeyPair;
    // The key pair of the network identity of the oracle node, with which earlier versions published the data
    private KeyPair identityKeyPair;
    private AuthorizedData authorizedData;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        storageService = new StorageService(new PersistenceService(appDataDir), CLOCK_BEFORE_CUTOFF);
        storageService.addListener(new StorageService.Listener() {
            @Override
            public void onAdded(StorageData storageData) {
                addedData.add(storageData);
            }

            @Override
            public void onRemoved(StorageData storageData) {
                removedData.add(storageData);
            }

            @Override
            public void onRefreshed(StorageData storageData) {
            }
        });
        store = storageService.getOrCreateAuthenticatedDataStore(TestAuthorizedDistributedData.class.getSimpleName())
                .join();
        authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        TestAuthorizedDistributedData data = TestAuthorizedDistributedData.withLegacyOwners("content");
        byte[] signature = SignatureUtil.sign(data.serializeForHash(), authorizedKeyPair.getPrivate());
        authorizedData = new AuthorizedData(data, Optional.of(signature), authorizedKeyPair.getPublic());
    }

    // The store writes asynchronously. JUnit deletes the directory after the test, and fails if a write still creates
    // files in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        storageService.shutdown();
    }

    @Test
    void applicationsDoNotGetDataOfOtherPublishers() throws GeneralSecurityException {
        assertTrue(store.add(createRequestOfEarlierVersion(identityKeyPair)).isSuccess());

        assertTrue(addedData.isEmpty());
        assertEquals(0, storageService.getAuthenticatedData().count());
    }

    // The case of an oracle node which publishes its data again with its authorized key pair. The removal or expiry of
    // the entry of the earlier version must not remove the data of the owner from the applications.
    @Test
    void removalOfEntryOfOtherPublisherDoesNotRemoveDataOfOwner() throws GeneralSecurityException {
        assertTrue(store.add(createRequestOfEarlierVersion(identityKeyPair)).isSuccess());
        assertTrue(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, authorizedKeyPair)).isSuccess());
        assertEquals(List.of(authorizedData), addedData);

        assertTrue(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedData, identityKeyPair))
                .isSuccess());

        assertTrue(removedData.isEmpty());
        assertEquals(List.of(authorizedData), storageService.getAuthenticatedData().toList());
    }

    @Test
    void removalByOwnerRemovesData() throws GeneralSecurityException {
        assertTrue(store.add(createRequestOfEarlierVersion(identityKeyPair)).isSuccess());
        assertTrue(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, authorizedKeyPair)).isSuccess());

        assertTrue(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedData, authorizedKeyPair))
                .isSuccess());

        assertEquals(List.of(authorizedData), removedData);
        assertEquals(0, storageService.getAuthenticatedData().count());
    }

    // An add request as oracle nodes of earlier versions created it: signed with the key pair of their network
    // identity, without signature V2. The transition rule accepts it if it was created before the cutoff date.
    private AddAuthenticatedDataRequest createRequestOfEarlierVersion(KeyPair publisherKeyPair)
            throws GeneralSecurityException {
        long created = Math.min(System.currentTimeMillis(), AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime() - 1);
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authorizedData,
                1,
                DigestUtil.hash(publisherKeyPair.getPublic().getEncoded()),
                created);
        return new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), publisherKeyPair.getPrivate()),
                publisherKeyPair.getPublic().getEncoded(),
                publisherKeyPair.getPublic(),
                new byte[]{},
                0);
    }
}
