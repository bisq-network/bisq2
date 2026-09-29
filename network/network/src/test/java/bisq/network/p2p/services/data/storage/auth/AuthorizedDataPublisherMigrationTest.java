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

import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.CLOCK_BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Oracle nodes of earlier versions published their authorized data with the key pair of their network identity. The
 * migration publishes it again with the authorized key pair and removes the entries of the earlier key pair (see
 * docs/specifications/network/storage-ownership.md).
 * <p>
 * Entries of earlier versions have no signature V2. The node accepts them only before
 * SignedRequest.SIGNATURE_V1_CUTOFF_DATE, so the node of these tests has a time before that date.
 */
class AuthorizedDataPublisherMigrationTest {
    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService store;
    // The authorized key pair of the oracle node, which owns the data
    private KeyPair authorizedKeyPair;
    // The key pair of the network identity of the oracle node, with which earlier versions published the data
    private KeyPair identityKeyPair;
    private AuthorizedData authorizedData;
    // A full store accepts no entry for a new storage key
    private boolean isStoreFull;

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "migration",
                TestAuthorizedDistributedData.class.getSimpleName(),
                CLOCK_BEFORE_CUTOFF) {
            @Override
            protected boolean isExceedingMapSize() {
                return isStoreFull || super.isExceedingMapSize();
            }
        };
        authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedData = authorize("content", authorizedKeyPair);
    }

    // The store writes asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    // Nodes of earlier versions store both entries under one storage key (V1). They accept only higher sequence numbers:
    // the remove request replaces the entry of the earlier version, and the new entry replaces either of them.
    @Test
    void publishesDataAgainWithAuthorizedKeyPairAndRemovesEntryOfEarlierKeyPair() throws GeneralSecurityException {
        AddAuthenticatedDataRequest entryOfEarlierVersion = createRequestOfEarlierVersion(authorizedData, 5);
        assertTrue(store.add(entryOfEarlierVersion).isSuccess());

        List<AuthenticatedDataRequest> requests = AuthorizedDataPublisherMigration.migrate(store, identityKeyPair,
                authorizedKeyPair);

        assertEquals(2, requests.size());
        AddAuthenticatedDataRequest addRequest = assertInstanceOf(AddAuthenticatedDataRequest.class, requests.get(0));
        RemoveAuthenticatedDataRequest removeRequest = assertInstanceOf(RemoveAuthenticatedDataRequest.class,
                requests.get(1));
        assertEquals(authorizedData, addRequest.getAuthenticatedSequentialData().getAuthenticatedData());
        assertFalse(addRequest.getAuthenticatedSequentialData().isOwnerInvalid());
        assertEquals(6, removeRequest.getSequenceNumber());
        assertEquals(7, addRequest.getSequenceNumber());
        assertEquals(addRequest, store.getPersistableStore().getMap().get(addRequest.getStorageKey()));
        assertEquals(removeRequest, store.getPersistableStore().getMap().get(entryOfEarlierVersion.getStorageKey()));
    }

    @Test
    void migrationIsDoneOnce() throws GeneralSecurityException {
        assertTrue(store.add(createRequestOfEarlierVersion(authorizedData, 1)).isSuccess());
        assertEquals(2, AuthorizedDataPublisherMigration.migrate(store, identityKeyPair, authorizedKeyPair).size());

        assertTrue(AuthorizedDataPublisherMigration.migrate(store, identityKeyPair, authorizedKeyPair).isEmpty());
    }

    // The entry of the earlier key pair is the source of the data. The migration keeps it until the store accepts the
    // entry of the authorized key pair.
    @Test
    void entryOfEarlierKeyPairStaysIfStoreRejectsNewEntry() throws GeneralSecurityException {
        AddAuthenticatedDataRequest entryOfEarlierVersion = createRequestOfEarlierVersion(authorizedData, 1);
        assertTrue(store.add(entryOfEarlierVersion).isSuccess());
        isStoreFull = true;

        List<AuthenticatedDataRequest> requestsOfFullStore = AuthorizedDataPublisherMigration.migrate(store,
                identityKeyPair, authorizedKeyPair);

        assertTrue(requestsOfFullStore.isEmpty());
        assertEquals(Map.of(entryOfEarlierVersion.getStorageKey(), entryOfEarlierVersion),
                store.getPersistableStore().getMap());

        isStoreFull = false;
        List<AuthenticatedDataRequest> requests = AuthorizedDataPublisherMigration.migrate(store, identityKeyPair,
                authorizedKeyPair);

        assertEquals(2, requests.size());
        AddAuthenticatedDataRequest addRequest = assertInstanceOf(AddAuthenticatedDataRequest.class, requests.get(0));
        assertEquals(addRequest, store.getPersistableStore().getMap().get(addRequest.getStorageKey()));
        assertInstanceOf(RemoveAuthenticatedDataRequest.class,
                store.getPersistableStore().getMap().get(entryOfEarlierVersion.getStorageKey()));
    }

    // For example, the oracle node removed a bonded role with its authorized key pair before the migration ran
    @Test
    void dataWhichTheOwnerRemovedIsNotPublishedAgain() throws GeneralSecurityException {
        AddAuthenticatedDataRequest entryOfEarlierVersion = createRequestOfEarlierVersion(authorizedData, 1);
        assertTrue(store.add(entryOfEarlierVersion).isSuccess());
        RemoveAuthenticatedDataRequest removeRequestOfOwner = RemoveAuthenticatedDataRequest.from(store, authorizedData,
                authorizedKeyPair);
        assertTrue(store.remove(removeRequestOfOwner).isSuccess());

        List<AuthenticatedDataRequest> requests = AuthorizedDataPublisherMigration.migrate(store, identityKeyPair,
                authorizedKeyPair);

        assertEquals(1, requests.size());
        assertInstanceOf(RemoveAuthenticatedDataRequest.class, requests.get(0));
        assertEquals(removeRequestOfOwner, store.getPersistableStore().getMap().get(removeRequestOfOwner.getStorageKey()));
        assertInstanceOf(RemoveAuthenticatedDataRequest.class,
                store.getPersistableStore().getMap().get(entryOfEarlierVersion.getStorageKey()));
    }

    // Only the node's own data: entries of other publishers, for example copies, and data of other authorities stay
    @Test
    void entriesOfOtherPublishersAndDataOfOtherAuthoritiesAreNotMigrated() throws GeneralSecurityException {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AddAuthenticatedDataRequest copy = createRequest(authorizedData, otherKeyPair, 1,
                getCreationDateBeforeCutoff());
        AddAuthenticatedDataRequest dataOfOtherAuthority = createRequestOfEarlierVersion(
                authorize("content", otherKeyPair), 1);
        assertTrue(store.add(copy).isSuccess());
        assertTrue(store.add(dataOfOtherAuthority).isSuccess());

        assertTrue(AuthorizedDataPublisherMigration.migrate(store, identityKeyPair, authorizedKeyPair).isEmpty());
        assertEquals(2, store.getPersistableStore().getMap().size());
    }

    // An expired entry which the store did not prune yet must not get a new time to live
    @Test
    void expiredEntryIsNotPublishedAgain() throws GeneralSecurityException {
        AddAuthenticatedDataRequest expiredEntry = createRequest(authorizedData, identityKeyPair, 1,
                System.currentTimeMillis() - TestAuthorizedDistributedData.META_DATA.getTtl() - 1000);
        store.getPersistableStore().getMap().put(expiredEntry.getStorageKey(), expiredEntry);

        assertTrue(AuthorizedDataPublisherMigration.migrate(store, identityKeyPair, authorizedKeyPair).isEmpty());
    }

    private AuthorizedData authorize(String content, KeyPair keyPair) throws GeneralSecurityException {
        TestAuthorizedDistributedData data = TestAuthorizedDistributedData.withLegacyOwners(content);
        byte[] signature = SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate());
        return new AuthorizedData(data, Optional.of(signature), keyPair.getPublic());
    }

    private AddAuthenticatedDataRequest createRequestOfEarlierVersion(AuthorizedData data, int sequenceNumber)
            throws GeneralSecurityException {
        return createRequest(data, identityKeyPair, sequenceNumber, getCreationDateBeforeCutoff());
    }

    // An add request as nodes of earlier versions created it: without signature V2
    private static AddAuthenticatedDataRequest createRequest(AuthorizedData data,
                                                             KeyPair publisherKeyPair,
                                                             int sequenceNumber,
                                                             long created) throws GeneralSecurityException {
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

    // The transition rule accepts entries of the earlier version only if they were created before the cutoff date
    private static long getCreationDateBeforeCutoff() {
        return Math.min(System.currentTimeMillis(), AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime() - 1);
    }
}
