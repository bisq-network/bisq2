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
import bisq.common.encoding.Hex;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
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
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedDataStorageServiceTest {
    // A store holds the data of one type. Its key is the class name of that type.
    private static final String STORE_KEY = TestDistributedData.class.getSimpleName();

    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService ownerStore;
    private AuthenticatedDataStorageService peerStore;
    private AuthenticatedDataStorageService authorizedDataStore;
    private KeyPair ownerKeyPair;
    private KeyPair authorizedKeyPair;
    private AuthenticatedData authenticatedData;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        PersistenceService persistenceService = new PersistenceService(tempDir);
        PruneExpiredEntriesService pruneExpiredEntriesService = new PruneExpiredEntriesService();
        ownerStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, "owner", STORE_KEY);
        peerStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, "peer", STORE_KEY);
        authorizedDataStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService,
                "authorized", TestAuthorizedDistributedData.class.getSimpleName());
        ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content", ownerKeyPair.getPublic()));
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
                new PruneExpiredEntriesService(), "newNode", STORE_KEY);
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
    void refreshWithModifiedVersionIsRejected() throws GeneralSecurityException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RefreshAuthenticatedDataRequest modifiedRefreshRequest = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder()
                        .setVersion(refreshRequest.getVersion() + 1)
                        .build());

        DataStorageResult result = peerStore.refresh(modifiedRefreshRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureInvalid(), result.toString());
    }

    @Test
    void refreshWithoutSignatureV2IsVerifiedWithSignatureV1() throws GeneralSecurityException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        // A node of an earlier version sends version 1 and no signature V2
        RefreshAuthenticatedDataRequest asFromEarlierVersion = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder()
                        .setVersion(1)
                        .clearSignatureV2()
                        .build());

        DataStorageResult result = peerStore.refresh(asFromEarlierVersion);

        assertTrue(result.isSuccess(), result.toString());
    }

    // Storage key V2 contains the public key of the owner. A refresh signed with another key therefore does not find
    // the entry of the owner.
    @Test
    void refreshSignedWithOtherKeyIsRejected() throws GeneralSecurityException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore,
                authenticatedData,
                KeyGeneration.generateDefaultEcKeyPair());

        DataStorageResult result = peerStore.refresh(refreshRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isNoEntry(), result.toString());
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
                new DefaultAuthenticatedData(new TestDistributedData("other content", ownerKeyPair.getPublic())),
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

    @Test
    void removeRequestWithoutEntryExpiresAfterTtlOfRegisteredMetaData() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        // Signature V1 covers only the hash of the data. Signature V2 also covers created, but a relay can remove
        // signature V2, because nodes accept requests of earlier versions. A relay can therefore change metaData and
        // created. TestDistributedData has a time to live of 10 days. The forged MetaData claims 100 days.
        //noinspection deprecation
        RemoveAuthenticatedDataRequest forgedRequest = RemoveAuthenticatedDataRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setMetaData(new MetaData(MetaData.TTL_100_DAYS, TestDistributedData.class.getSimpleName()).toProto(false))
                        .setCreated(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(11))
                        .clearSignatureV2()
                        .build());

        assertSame(TestDistributedData.META_DATA, forgedRequest.getMetaData());
        DataStorageResult result = peerStore.remove(forgedRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isExpired(), result.toString());
        assertFalse(peerStore.getPersistableStore().getMap().containsKey(removeRequest.getStorageKey()));
    }

    // Nodes of earlier versions compute expiry, priority and cost factor from the field metaData. A relay sends them the
    // registered MetaData. The hash does not cover the field, so the signature and the storage key stay the same.
    @Test
    void relayedRemoveAndRefreshRequestsCarryTheRegisteredMetaData() throws GeneralSecurityException {
        MetaData forgedMetaData = new MetaData(MetaData.TTL_2_DAYS, MetaData.HIGHEST_PRIORITY,
                TestDistributedData.class.getSimpleName(), 0);

        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData,
                ownerKeyPair);
        //noinspection deprecation
        RemoveAuthenticatedDataRequest forgedRemoveRequest = RemoveAuthenticatedDataRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder().setMetaData(forgedMetaData.toProto(false)).build());
        //noinspection deprecation
        assertEquals(TestDistributedData.META_DATA.toProto(false), forgedRemoveRequest.toValueProto(false).getMetaData());
        assertArrayEquals(removeRequest.serializeForHash(), forgedRemoveRequest.serializeForHash());

        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore,
                authenticatedData, ownerKeyPair);
        //noinspection deprecation
        RefreshAuthenticatedDataRequest forgedRefreshRequest = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder().setMetaData(forgedMetaData.toProto(false)).build());
        //noinspection deprecation
        assertEquals(TestDistributedData.META_DATA.toProto(false), forgedRefreshRequest.toValueProto(false).getMetaData());
        assertArrayEquals(refreshRequest.serializeForHash(), forgedRefreshRequest.serializeForHash());
    }

    // Nodes of earlier versions compute the cost factor from the field metaData, which carries the registered
    // MetaData. If the registered MetaData differs from the values of the policy, the sender pays the higher value.
    @Test
    void senderPaysTheCostFactorWhichEarlierVersionsComputeFromMetaData() throws GeneralSecurityException {
        // The policy has no values for TestDistributedData. Its registered MetaData has a time to live of 10 days and
        // a maximum map size of 1000, which gives a storage cost factor of 0.1 + 0.1.
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore,
                authenticatedData, ownerKeyPair);
        assertEquals(0.1, MessageCostPolicy.getCostFactor(removeRequest));
        assertEquals(0.2, MessageCostPolicy.getCostFactorToPay(removeRequest), 1e-9);

        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore,
                authenticatedData, ownerKeyPair);
        assertEquals(0.1, MessageCostPolicy.getCostFactor(refreshRequest));
        assertEquals(0.2, MessageCostPolicy.getCostFactorToPay(refreshRequest), 1e-9);
    }

    @Test
    void forgedMetaDataOfRemoveRequestChangesNeitherCostFactorNorPriority() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        //noinspection deprecation
        RemoveAuthenticatedDataRequest forgedRequest = RemoveAuthenticatedDataRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setMetaData(new MetaData(MetaData.TTL_2_DAYS, MetaData.HIGHEST_PRIORITY,
                                TestDistributedData.class.getSimpleName(), 0).toProto(false))
                        .build());

        assertEquals(MessageCostPolicy.getCostFactor(removeRequest), MessageCostPolicy.getCostFactor(forgedRequest));
        assertEquals(TestDistributedData.META_DATA.getPriority(), forgedRequest.getPriority());
    }

    @Test
    void storeRejectsRequestsOfOtherType() throws GeneralSecurityException {
        AuthenticatedDataStorageService storeOfOtherType = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(), "other", "OtherDistributedData");
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);

        assertTrue(storeOfOtherType.add(addRequest).isClassNameInvalid());
        assertTrue(storeOfOtherType.remove(removeRequest).isClassNameInvalid());
        assertTrue(storeOfOtherType.refresh(refreshRequest).isClassNameInvalid());
        assertTrue(storeOfOtherType.getPersistableStore().getMap().isEmpty());
    }

    @Test
    void removeRequestWithClassNameOfOtherTypeIsRejected() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        // A relay can change className, because the signature does not cover it
        RemoveAuthenticatedDataRequest requestWithOtherClassName = RemoveAuthenticatedDataRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setClassName("OtherDistributedData")
                        .build());

        DataStorageResult result = peerStore.remove(requestWithOtherClassName);

        assertFalse(result.isSuccess());
        assertTrue(result.isClassNameInvalid(), result.toString());
    }

    // A user must not publish the data of another user. The user could remove the copy, and the application would
    // remove the data of the owner (see docs/specifications/network/storage-ownership.md).
    @Test
    void addSignedByAnotherKeyThanTheOwnerIsRejected() throws GeneralSecurityException {
        AddAuthenticatedDataRequest ownerRequest = AddAuthenticatedDataRequest.from(peerStore, authenticatedData,
                ownerKeyPair);
        assertTrue(peerStore.add(ownerRequest).isSuccess());
        AddAuthenticatedDataRequest copy = AddAuthenticatedDataRequest.from(peerStore, authenticatedData,
                KeyGeneration.generateDefaultEcKeyPair());
        assertFalse(copy.isSignatureInvalid());

        DataStorageResult result = peerStore.add(copy);

        assertFalse(result.isSuccess());
        assertTrue(result.isOwnerInvalid(), result.toString());
        assertEquals(Map.of(ownerRequest.getStorageKey(), ownerRequest), peerStore.getPersistableStore().getMap());
    }

    // Distributed data which names no owner has no owner as default authenticated data. Authorized data names no
    // owner, so it cannot be stored as default authenticated data, which would skip its authorization check.
    @Test
    void defaultAuthenticatedDataWithoutOwnerIsRejected() throws GeneralSecurityException {
        AuthenticatedData dataWithoutOwner = new DefaultAuthenticatedData(new TestAuthorizedDistributedData("content"));

        DataStorageResult result = authorizedDataStore.add(AddAuthenticatedDataRequest.from(authorizedDataStore,
                dataWithoutOwner, authorizedKeyPair));

        assertFalse(result.isSuccess());
        assertTrue(result.isOwnerInvalid(), result.toString());
        assertTrue(authorizedDataStore.getPersistableStore().getMap().isEmpty());
    }

    // The authorization signature does not cover the key which signs the add request. The authorized key therefore
    // owns the data.
    @Test
    void authorizedDataIsOwnedByTheAuthorizedKey() throws GeneralSecurityException {
        AuthorizedData authorizedData = authorize(new TestAuthorizedDistributedData("content"));
        AddAuthenticatedDataRequest ownerRequest = AddAuthenticatedDataRequest.from(authorizedDataStore,
                authorizedData, authorizedKeyPair);
        assertTrue(authorizedDataStore.add(ownerRequest).isSuccess());

        DataStorageResult resultOfCopy = authorizedDataStore.add(AddAuthenticatedDataRequest.from(authorizedDataStore,
                authorizedData, ownerKeyPair));

        assertTrue(resultOfCopy.isOwnerInvalid(), resultOfCopy.toString());
        assertEquals(Map.of(ownerRequest.getStorageKey(), ownerRequest),
                authorizedDataStore.getPersistableStore().getMap());
    }

    // For example AuthorizedOracleNode, which the key of its network ID owns
    @Test
    void authorizedDataWhichNamesItsOwnerIsOwnedByThatOwner() throws GeneralSecurityException {
        StorableTypesRegistry.register(AuthorizedDataWithOwner.class, AuthorizedDataWithOwner.META_DATA);
        AuthenticatedDataStorageService store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(), "authorizedWithOwner", AuthorizedDataWithOwner.class.getSimpleName());
        AuthorizedData authorizedData = authorize(new AuthorizedDataWithOwner(ownerKeyPair.getPublic()));

        DataStorageResult resultOfAuthorizedKey = store.add(AddAuthenticatedDataRequest.from(store, authorizedData,
                authorizedKeyPair));
        DataStorageResult resultOfOwner = store.add(AddAuthenticatedDataRequest.from(store, authorizedData,
                ownerKeyPair));

        assertTrue(resultOfAuthorizedKey.isOwnerInvalid(), resultOfAuthorizedKey.toString());
        assertTrue(resultOfOwner.isSuccess(), resultOfOwner.toString());
    }

    // Oracle nodes of earlier versions published some types with the key of their network identity. Data of these
    // types can have another owner if it was created before the cutoff date.
    @Test
    void otherOwnerIsAcceptedOnlyForLegacyTypesCreatedBeforeTheCutoffDate() {
        long cutoffDate = AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime();
        AuthorizedData legacyType = authorize(TestAuthorizedDistributedData.withLegacyOwners("content"));
        AuthorizedData otherType = authorize(new TestAuthorizedDistributedData("content"));

        assertTrue(legacyType.isOtherOwnerAccepted(cutoffDate - 1));
        assertFalse(legacyType.isOtherOwnerAccepted(cutoffDate));
        assertFalse(otherType.isOtherOwnerAccepted(cutoffDate - 1));
        assertFalse(authenticatedData.isOtherOwnerAccepted(cutoffDate - 1));

        // A request can have a creation date before the cutoff date, but not in the future
        long created = Math.min(System.currentTimeMillis(), cutoffDate - 1);
        byte[] otherPubKeyHash = DigestUtil.hash(ownerKeyPair.getPublic().getEncoded());
        assertFalse(new AuthenticatedSequentialData(legacyType, 1, otherPubKeyHash, created).isOwnerInvalid());
        assertTrue(new AuthenticatedSequentialData(otherType, 1, otherPubKeyHash, created).isOwnerInvalid());
    }

    // A refresh replaces the creation date, on which the transition rule for authorized data depends. So the store
    // checks the owner of the refreshed entry again.
    @Test
    void refreshOfEntryOfAnotherOwnerIsRejected() throws GeneralSecurityException {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AddAuthenticatedDataRequest copy = AddAuthenticatedDataRequest.from(peerStore, authenticatedData, otherKeyPair);
        // An entry which the store did not check, as earlier versions stored it
        peerStore.getPersistableStore().getMap().put(copy.getStorageKey(), copy);

        DataStorageResult result = peerStore.refresh(RefreshAuthenticatedDataRequest.from(peerStore, authenticatedData,
                otherKeyPair));

        assertFalse(result.isSuccess());
        assertTrue(result.isOwnerInvalid(), result.toString());
        assertSame(copy, peerStore.getPersistableStore().getMap().get(copy.getStorageKey()));
    }

    // Earlier versions did not check the owner at add. The store removes such entries when it loads its data.
    @Test
    void persistedDataOfAnotherOwnerIsRemovedAtLoad() throws GeneralSecurityException {
        AddAuthenticatedDataRequest ownerRequest = AddAuthenticatedDataRequest.from(peerStore, authenticatedData,
                ownerKeyPair);
        AddAuthenticatedDataRequest copy = AddAuthenticatedDataRequest.from(peerStore, authenticatedData,
                KeyGeneration.generateDefaultEcKeyPair());
        Map<ByteArray, AuthenticatedDataRequest> map = peerStore.getPersistableStore().getMap();
        map.put(ownerRequest.getStorageKey(), ownerRequest);
        map.put(copy.getStorageKey(), copy);

        peerStore.onPersistedApplied(peerStore.getPersistableStore());

        assertEquals(Map.of(ownerRequest.getStorageKey(), ownerRequest), map);
    }

    private AuthorizedData authorize(AuthorizedDistributedData data) {
        try {
            byte[] signature = SignatureUtil.sign(data.serializeForHash(), authorizedKeyPair.getPrivate());
            return new AuthorizedData(data, Optional.of(signature), authorizedKeyPair.getPublic());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void addToOwnerAndPeer() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(addRequest).isSuccess());
        assertTrue(peerStore.add(addRequest).isSuccess());
    }

    // Authorized data which names its owner, like AuthorizedOracleNode
    private static final class AuthorizedDataWithOwner implements AuthorizedDistributedData, OwnershipVerifiableData {
        private static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS,
                AuthorizedDataWithOwner.class.getSimpleName());

        private final byte[] ownerPubKeyHash;

        private AuthorizedDataWithOwner(PublicKey owner) {
            ownerPubKeyHash = DigestUtil.hash(owner.getEncoded());
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(Hex.encode(ownerPubKeyHash));
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public boolean isDataInvalid() {
            return false;
        }

        @Override
        public byte[] getOwnerPubKeyHash() {
            return ownerPubKeyHash;
        }
    }
}
