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

import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedDataStorageServiceTest {
    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService ownerStore;
    private AuthenticatedDataStorageService peerStore;
    private KeyPair ownerKeyPair;
    private AuthenticatedData authenticatedData;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = new PersistenceService(tempDir);
        PruneExpiredEntriesService pruneExpiredEntriesService = new PruneExpiredEntriesService();
        ownerStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, "test", "OwnerStore");
        peerStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, "test", "PeerStore");
        ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content"));
    }

    // The stores write asynchronously, on the single thread of the persistence executor. JUnit deletes tempDir after
    // the test, and fails if a write still creates files in it. A write queued now completes after all earlier ones.
    @AfterEach
    void waitForPendingWrites() {
        peerStore.getPersistence().persistAsync(peerStore.getPersistableStore().getClone()).join();
    }

    @Test
    void refreshIsAcceptedAndStoredEntryHasValidSignature() throws GeneralSecurityException {
        addToOwnerAndPeer();

        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.refresh(refreshRequest).isSuccess());
        DataStorageResult result = peerStore.refresh(refreshRequest);

        assertTrue(result.isSuccess(), result.toString());
        AddAuthenticatedDataRequest storedRequest = (AddAuthenticatedDataRequest) peerStore.getPersistableStore().getMap()
                .get(refreshRequest.getStorageKey());
        assertEquals(2, storedRequest.getSequenceNumber());
        assertEquals(refreshRequest.getCreated(), storedRequest.getCreated());
        assertFalse(storedRequest.isSignatureInvalid());
    }

    @Test
    void refreshedEntryIsAcceptedByNodeWithoutEntry() throws GeneralSecurityException {
        // Simulates the inventory case: a node that did not have the data receives the refreshed add request.
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.refresh(refreshRequest).isSuccess());
        AddAuthenticatedDataRequest refreshedEntry = (AddAuthenticatedDataRequest) peerStore.getPersistableStore().getMap()
                .get(refreshRequest.getStorageKey());

        AuthenticatedDataStorageService newNodeStore = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(), "test", "NewNodeStore");
        DataStorageResult result = newNodeStore.add(refreshedEntry);

        assertTrue(result.isSuccess(), result.toString());
    }

    @Test
    void refreshWithModifiedCreationDateIsRejected() throws GeneralSecurityException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.RefreshAuthenticatedDataRequest modifiedProto = refreshRequest.toValueProto(false).toBuilder()
                .setCreated(refreshRequest.getCreated() + 1)
                .build();
        RefreshAuthenticatedDataRequest modifiedRefreshRequest = RefreshAuthenticatedDataRequest.fromProto(modifiedProto);

        DataStorageResult result = peerStore.refresh(modifiedRefreshRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureInvalid(), result.toString());
    }

    @Test
    void refreshSignedWithOtherKeyIsRejected() throws GeneralSecurityException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore,
                authenticatedData,
                KeyGeneration.generateDefaultEcKeyPair());

        DataStorageResult result = peerStore.refresh(refreshRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isPublicKeyHashInvalid(), result.toString());
    }

    @Test
    void duplicateAddRequestIsReportedAsAlreadyReceived() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.add(addRequest).isSuccess());

        DataStorageResult result = peerStore.add(addRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isRequestAlreadyReceived(), result.toString());
    }

    @Test
    void removeRequestWithInvalidSignatureIsNotStoredWithoutEntry() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest otherRemoveRequest = RemoveAuthenticatedDataRequest.from(ownerStore,
                new DefaultAuthenticatedData(new TestDistributedData("other content")),
                ownerKeyPair);
        // Signature of other data, so it does not match the hash of this request.
        RemoveAuthenticatedDataRequest invalidRemoveRequest = RemoveAuthenticatedDataRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setSignature(otherRemoveRequest.toValueProto(false).getSignature())
                        .build());

        DataStorageResult result = peerStore.remove(invalidRemoveRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureInvalid(), result.toString());
        assertFalse(peerStore.getPersistableStore().getMap().containsKey(removeRequest.getStorageKey()));
    }

    @Test
    void validRemoveRequestIsStoredWithoutEntry() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);

        DataStorageResult result = peerStore.remove(removeRequest);

        assertTrue(result.isSuccess(), result.toString());
        assertTrue(result.isNoEntry());
        assertTrue(peerStore.getPersistableStore().getMap().containsKey(removeRequest.getStorageKey()));
    }

    private void addToOwnerAndPeer() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(addRequest).isSuccess());
        assertTrue(peerStore.add(addRequest).isSuccess());
    }
}
