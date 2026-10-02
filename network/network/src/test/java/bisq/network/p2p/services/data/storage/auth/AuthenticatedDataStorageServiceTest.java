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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
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
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    // The time of the nodes. From SignedRequest.SIGNATURE_V1_CUTOFF_DATE on, they require signature V2. The tests of
    // requests without signature V2 set a time before that date, so they do not depend on the date at which they run.
    private long time = AT_CUTOFF;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        PersistenceService persistenceService = new PersistenceService(tempDir);
        PruneExpiredEntriesService pruneExpiredEntriesService = new PruneExpiredEntriesService();
        ownerStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, "owner",
                STORE_KEY, () -> time);
        peerStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, "peer",
                STORE_KEY, () -> time);
        authorizedDataStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService,
                "authorized", TestAuthorizedDistributedData.class.getSimpleName(), () -> time);
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
    void refreshWithModifiedCreationDateIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
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
    void refreshWithModifiedVersionIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
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

    // A refresh request needs signature V2, also before the cutoff date of signature V1. Nodes of earlier versions do
    // not relay refresh requests, so only a relay which removes signature V2 on purpose creates such a request.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void refreshWithoutSignatureV2IsRejected(boolean beforeCutoffDate)
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = beforeCutoffDate ? BEFORE_CUTOFF : AT_CUTOFF;
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RefreshAuthenticatedDataRequest withoutSignatureV2 = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder()
                        .clearSignatureV2()
                        .build());
        AuthenticatedDataRequest storedRequest = peerStore.getPersistableStore().getMap().get(refreshRequest.getStorageKey());

        DataStorageResult result = peerStore.refresh(withoutSignatureV2);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureV2Missing(), result.toString());
        assertSame(storedRequest, peerStore.getPersistableStore().getMap().get(refreshRequest.getStorageKey()));
        assertTrue(peerStore.refresh(refreshRequest).isSuccess());
    }

    // Nodes of versions up to 2.1.13 sign only the hash of the data. This signature covers neither the sequence number
    // nor the creation date, so a node cannot apply such a request. These nodes send a refresh request every few
    // minutes while the user is active, so the result is not a severe failure.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void refreshOfEarlierVersionIsRejected(boolean beforeCutoffDate)
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = beforeCutoffDate ? BEFORE_CUTOFF : AT_CUTOFF;
        addToOwnerAndPeer();
        AuthenticatedDataRequest storedRequest = peerStore.getPersistableStore().getMap().values().iterator().next();
        byte[] hash = DigestUtil.hash(authenticatedData.serializeForHash());
        //noinspection deprecation
        RefreshAuthenticatedDataRequest ofEarlierVersion = RefreshAuthenticatedDataRequest.fromProto(
                bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
                        .setVersion(1)
                        .setMetaData(TestDistributedData.META_DATA.toProto(false))
                        .setHash(ByteString.copyFrom(hash))
                        .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerKeyPair.getPublic().getEncoded()))
                        .setSequenceNumber(2)
                        .setSignature(ByteString.copyFrom(SignatureUtil.sign(hash, ownerKeyPair.getPrivate())))
                        .setCreated(System.currentTimeMillis())
                        .build());
        assertEquals(storedRequest.getStorageKey(), ofEarlierVersion.getStorageKey());

        DataStorageResult result = peerStore.refresh(ofEarlierVersion);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureV2Missing(), result.toString());
        assertFalse(result.isSevereFailure(), result.toString());
        assertSame(storedRequest, peerStore.getPersistableStore().getMap().get(storedRequest.getStorageKey()));
    }

    // A broadcast arrives from several peers. The node applies the first copy of a refresh request. For the later
    // copies the sequence number has not increased.
    @Test
    void copyOfAppliedRefreshIsNotAppliedAgain() throws GeneralSecurityException, UnresolvableProtobufException {
        addToOwnerAndPeer();
        List<AuthenticatedData> refreshedData = new ArrayList<>();
        peerStore.addListener(new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
            }

            @Override
            public void onRefreshed(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                refreshedData.add(authenticatedData);
            }
        });
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.refresh(refreshRequest).isSuccess());
        AuthenticatedDataRequest storedRequest = peerStore.getPersistableStore().getMap().get(refreshRequest.getStorageKey());

        DataStorageResult result = peerStore.refresh(RefreshAuthenticatedDataRequest.fromProto(refreshRequest.toValueProto(false)));

        assertFalse(result.isSuccess());
        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertFalse(result.isSevereFailure(), result.toString());
        assertSame(storedRequest, peerStore.getPersistableStore().getMap().get(refreshRequest.getStorageKey()));
        assertEquals(List.of(authenticatedData), refreshedData);
    }

    @Test
    void refreshOfRemovedDataIsRejected() throws GeneralSecurityException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.remove(removeRequest).isSuccess());

        DataStorageResult result = peerStore.refresh(refreshRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isAlreadyRemoved(), result.toString());
        assertSame(removeRequest, peerStore.getPersistableStore().getMap().get(refreshRequest.getStorageKey()));
    }

    // Storage key V2 contains the public key of the owner. A refresh signed with another key therefore does not find
    // the entry of the owner. Only the owner can create a refresh request of its data, so the test changes the key of
    // a received one.
    @Test
    void refreshSignedWithOtherKeyIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        addToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.fromProto(
                RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair)
                        .toValueProto(false).toBuilder()
                        .setOwnerPublicKeyBytes(ByteString.copyFrom(
                                KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded()))
                        .build());

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

    // A store accepts at most DataStorageService.MAX_MAP_SIZE entries (see
    // docs/specifications/network/storage-metadata.md)
    @Test
    void storeWithTheMaximumNumberOfEntriesAcceptsNoNewEntry() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(peerStore, authenticatedData, ownerKeyPair);
        Map<ByteArray, AuthenticatedDataRequest> map = peerStore.getPersistableStore().getMap();
        // The limit counts entries, so the same request under other keys fills the map
        for (int i = 0; i < DataStorageService.MAX_MAP_SIZE; i++) {
            map.put(new ByteArray(ByteBuffer.allocate(Integer.BYTES).putInt(i).array()), addRequest);
        }

        DataStorageResult resultOfAdd = peerStore.add(addRequest);
        DataStorageResult resultOfRemovalWithoutEntry = peerStore.remove(RemoveAuthenticatedDataRequest.from(peerStore,
                createData("other content"), ownerKeyPair));

        assertTrue(resultOfAdd.isMaxMapSizeReached(), resultOfAdd.toString());
        assertTrue(resultOfRemovalWithoutEntry.isMaxMapSizeReached(), resultOfRemovalWithoutEntry.toString());
        assertEquals(DataStorageService.MAX_MAP_SIZE, map.size());
        // The pending write of the other tests would have to serialize all entries
        map.clear();
    }

    // A node parses each copy of a request which it receives, so the copies are different objects with equal values.
    // The store must detect the copy before it verifies the signatures, also for authorized data, whose authorization
    // signature is an Optional of a byte array.
    @Test
    void duplicateAddRequestOfAuthorizedDataIsReportedAsAlreadyReceived() throws GeneralSecurityException {
        AuthorizedData authorizedData = authorize(new TestAuthorizedDistributedData("content"));
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(authorizedDataStore, authorizedData,
                authorizedKeyPair);
        assertTrue(authorizedDataStore.add(addRequest).isSuccess());
        AuthenticatedSequentialData sequentialData = addRequest.getAuthenticatedSequentialData();
        AuthorizedData copyOfAuthorizedData = new AuthorizedData(authorizedData.getAuthorizedDistributedData(),
                authorizedData.getSignature().map(byte[]::clone),
                authorizedKeyPair.getPublic());
        AddAuthenticatedDataRequest copy = new AddAuthenticatedDataRequest(
                new AuthenticatedSequentialData(copyOfAuthorizedData,
                        sequentialData.getSequenceNumber(),
                        sequentialData.getPubKeyHash().clone(),
                        sequentialData.getCreated()),
                addRequest.getSignatureV1().clone(),
                addRequest.getSignaturePubKeyEncoded().clone(),
                addRequest.getSignaturePublicKey(),
                addRequest.getSignatureV2().clone(),
                addRequest.getVersion());

        assertEquals(addRequest, copy);
        assertEquals(addRequest.hashCode(), copy.hashCode());
        DataStorageResult result = authorizedDataStore.add(copy);

        assertFalse(result.isSuccess());
        assertTrue(result.isRequestAlreadyReceived(), result.toString());
    }

    @Test
    void removeRequestWithInvalidSignatureIsNotStoredWithoutEntry()
            throws GeneralSecurityException, UnresolvableProtobufException {
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
    void removeRequestWithoutEntryExpiresAfterTtlOfRegisteredMetaData()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        // Signature V1 covers only the hash of the data. Signature V2 also covers created, but before the cutoff date
        // of signature V1 a relay can remove signature V2, because nodes accept requests of earlier versions. A relay
        // can therefore change metaData and created. TestDistributedData has a time to live of 10 days. The forged
        // MetaData claims 100 days.
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
    void relayedRemoveAndRefreshRequestsCarryTheRegisteredMetaData()
            throws GeneralSecurityException, UnresolvableProtobufException {
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
    void forgedMetaDataOfRemoveRequestChangesNeitherCostFactorNorPriority()
            throws GeneralSecurityException, UnresolvableProtobufException {
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
    void removeRequestWithClassNameOfOtherTypeIsRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
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

    @Test
    void staleAddCannotReplaceRefreshedEntry() throws GeneralSecurityException {
        AddAuthenticatedDataRequest original = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(original).isSuccess());
        assertTrue(peerStore.add(original).isSuccess());
        RefreshAuthenticatedDataRequest refresh = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.refresh(refresh).isSuccess());
        AuthenticatedDataRequest refreshed = peerStore.getPersistableStore().getMap().get(original.getStorageKey());

        DataStorageResult result = peerStore.add(original);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(refreshed, peerStore.getPersistableStore().getMap().get(original.getStorageKey()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void staleAddCannotReplaceRepublishedEntry(boolean legacy) throws GeneralSecurityException {
        if (legacy) {
            time = BEFORE_CUTOFF;
        }
        AddAuthenticatedDataRequest original = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(original).isSuccess());
        assertTrue(peerStore.add(original).isSuccess());
        RemoveAuthenticatedDataRequest remove = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.remove(remove).isSuccess());
        assertTrue(peerStore.remove(remove).isSuccess());
        AddAuthenticatedDataRequest republished = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        if (legacy) {
            republished = withoutSignatureV2(republished);
        }
        assertTrue(peerStore.add(republished).isSuccess());

        DataStorageResult result = peerStore.add(original);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(republished, peerStore.getPersistableStore().getMap().get(original.getStorageKey()));
    }

    @Test
    void differentAddWithEqualSequenceCannotReplaceEntry() throws GeneralSecurityException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest original = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.add(original).isSuccess());
        AddAuthenticatedDataRequest legacy = withoutSignatureV2(original);

        DataStorageResult result = peerStore.add(legacy);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(original, peerStore.getPersistableStore().getMap().get(original.getStorageKey()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, Integer.MAX_VALUE})
    void verifiedRemovalReplacesLegacyRemovalRegardlessOfLegacySequence(int legacySequence)
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(add).isSuccess());
        RemoveAuthenticatedDataRequest verified = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertEquals(2, verified.getSequenceNumber());
        RemoveAuthenticatedDataRequest legacy = RemoveAuthenticatedDataRequest.fromProto(verified.toValueProto(false).toBuilder()
                .setVersion(1).clearSignatureV2().setSequenceNumber(legacySequence).build());
        assertTrue(peerStore.remove(legacy).isSuccess());

        assertTrue(peerStore.remove(verified).isSuccess());

        assertSame(verified, peerStore.getPersistableStore().getMap().get(verified.getStorageKey()));
        assertTrue(peerStore.add(add).isSequenceNrInvalid());
        assertSame(verified, peerStore.getPersistableStore().getMap().get(verified.getStorageKey()));
    }

    @Test
    void verifiedRemovalRequiresHigherVerifiedSequenceForReplacement() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest first = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.remove(first).isSuccess());
        RemoveAuthenticatedDataRequest second = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.remove(second).isSuccess());
        RemoveAuthenticatedDataRequest third = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.remove(second).isSuccess());

        assertTrue(peerStore.remove(first).isAlreadyRemoved());
        assertSame(second, peerStore.getPersistableStore().getMap().get(second.getStorageKey()));
        assertTrue(peerStore.remove(second).isAlreadyRemoved());
        assertSame(second, peerStore.getPersistableStore().getMap().get(second.getStorageKey()));
        assertTrue(peerStore.remove(third).isAlreadyRemoved());
        assertSame(third, peerStore.getPersistableStore().getMap().get(third.getStorageKey()));
    }

    // The store accepts a removal only if it stores it. A node relays the removals which its store accepts (see
    // docs/specifications/network/storage-request-relay.md).
    @Test
    void removalIsAcceptedOnlyIfTheStoreStoresIt() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(add).isSuccess());
        RemoveAuthenticatedDataRequest verified = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData,
                ownerKeyPair);
        RemoveAuthenticatedDataRequest legacy = withoutSignatureV2(verified, verified.getSequenceNumber());

        DataStorageResult withoutEntry = peerStore.remove(legacy);
        DataStorageResult replacement = peerStore.remove(verified);
        DataStorageResult repetition = peerStore.remove(verified);
        DataStorageResult legacyAfterReplacement = peerStore.remove(legacy);
        DataStorageResult removalOfData = ownerStore.remove(verified);

        assertTrue(withoutEntry.isSuccess() && withoutEntry.isNoEntry(), withoutEntry.toString());
        assertTrue(replacement.isSuccess() && replacement.isAlreadyRemoved(), replacement.toString());
        assertFalse(repetition.isSuccess(), repetition.toString());
        assertTrue(repetition.isAlreadyRemoved(), repetition.toString());
        assertFalse(legacyAfterReplacement.isSuccess(), legacyAfterReplacement.toString());
        assertSame(verified, peerStore.getPersistableStore().getMap().get(verified.getStorageKey()));
        assertTrue(removalOfData.isSuccess() && !removalOfData.isAlreadyRemoved(), removalOfData.toString());
    }

    @Test
    void legacyRemovalCannotDiscardVerifiedRemoval() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveAuthenticatedDataRequest verified = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(peerStore.remove(verified).isSuccess());
        RemoveAuthenticatedDataRequest legacy = RemoveAuthenticatedDataRequest.fromProto(verified.toValueProto(false).toBuilder()
                .setVersion(1).clearSignatureV2().setSequenceNumber(Integer.MAX_VALUE).build());

        assertTrue(peerStore.remove(legacy).isAlreadyRemoved());

        assertSame(verified, peerStore.getPersistableStore().getMap().get(verified.getStorageKey()));
    }

    @Test
    void invalidSignatureV2CannotReplaceLegacyRemoval() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveAuthenticatedDataRequest verified = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest legacy = RemoveAuthenticatedDataRequest.fromProto(verified.toValueProto(false).toBuilder()
                .setVersion(1).clearSignatureV2().build());
        assertTrue(peerStore.remove(legacy).isSuccess());
        RemoveAuthenticatedDataRequest invalid = RemoveAuthenticatedDataRequest.fromProto(verified.toValueProto(false).toBuilder()
                .setSequenceNumber(verified.getSequenceNumber() + 1).build());

        assertTrue(peerStore.remove(invalid).isSignatureInvalid());

        assertSame(legacy, peerStore.getPersistableStore().getMap().get(legacy.getStorageKey()));
    }

    @Test
    void legacyRemovalCannotBlockAddWithForgedSequence()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest remove = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest legacy = RemoveAuthenticatedDataRequest.fromProto(remove.toValueProto(false).toBuilder()
                .setVersion(1).clearSignatureV2().setSequenceNumber(Integer.MAX_VALUE).build());
        assertTrue(peerStore.remove(legacy).isSuccess());

        assertTrue(peerStore.add(add).isSuccess());

        assertSame(add, peerStore.getPersistableStore().getMap().get(add.getStorageKey()));
    }

    // The owner creates its requests with its own store. A relay can set the sequence number of a removal without
    // signature V2 to Integer.MAX_VALUE. If the owner continued from that number, it could not create any request for
    // the data anymore.
    @Test
    void ownerRepublishesDataAfterLegacyRemovalWithForgedSequence()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(add).isSuccess());
        RemoveAuthenticatedDataRequest forged = withoutSignatureV2(
                RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair), Integer.MAX_VALUE);
        assertTrue(ownerStore.remove(forged).isSuccess());
        assertTrue(peerStore.remove(forged).isSuccess());

        AddAuthenticatedDataRequest republished = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData,
                ownerKeyPair);

        assertEquals(1, republished.getSequenceNumber());
        assertTrue(ownerStore.add(republished).isSuccess());
        assertTrue(peerStore.add(republished).isSuccess());
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData,
                ownerKeyPair);
        assertEquals(2, removal.getSequenceNumber());
        assertTrue(peerStore.remove(removal).isSuccess());
        assertSame(removal, peerStore.getPersistableStore().getMap().get(removal.getStorageKey()));
    }

    // Also a removal without signature V2 which the store of an earlier version holds. A removal with signature V2
    // replaces it regardless of the sequence numbers, and the owner continues from the signed number.
    @Test
    void ownerReplacesPersistedLegacyRemovalWithForgedSequence()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveAuthenticatedDataRequest forged = withoutSignatureV2(
                RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair), Integer.MAX_VALUE);
        ownerStore.getPersistableStore().getMap().put(forged.getStorageKey(), forged);

        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData,
                ownerKeyPair);

        assertEquals(1, removal.getSequenceNumber());
        assertTrue(ownerStore.remove(removal).isSuccess());
        assertSame(removal, ownerStore.getPersistableStore().getMap().get(removal.getStorageKey()));
        assertEquals(2, AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair)
                .getSequenceNumber());
    }

    // From the cutoff date of signature V1 on, a node accepts only requests with signature V2 (see
    // docs/specifications/network/storage-request-signatures.md). The same request is accepted before that date.
    @Test
    void addWithoutSignatureV2IsAcceptedOnlyBeforeTheCutoffDate() throws GeneralSecurityException {
        AddAuthenticatedDataRequest legacy = withoutSignatureV2(
                AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair));
        assertFalse(legacy.isSignatureV1Invalid());

        time = AT_CUTOFF;
        DataStorageResult result = peerStore.add(legacy);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureV2Missing(), result.toString());
        // Nodes of earlier versions send such requests
        assertFalse(result.isSevereFailure(), result.toString());
        assertTrue(peerStore.getPersistableStore().getMap().isEmpty());

        time = BEFORE_CUTOFF;
        assertTrue(peerStore.add(legacy).isSuccess());
    }

    // The case which the cutoff date ends: a relay removes signature V2 from a removal of the owner and changes the
    // sequence number and the creation date, which signature V1 does not cover. The creation date does not decide:
    // the node uses its own time.
    @Test
    void removalWithoutSignatureV2IsAcceptedOnlyBeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(add).isSuccess());
        assertTrue(peerStore.add(add).isSuccess());
        RemoveAuthenticatedDataRequest remove = RemoveAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest changedByRelay = RemoveAuthenticatedDataRequest.fromProto(
                remove.toValueProto(false).toBuilder()
                        .clearSignatureV2()
                        .setSequenceNumber(Integer.MAX_VALUE)
                        .setCreated(remove.getCreated() - TimeUnit.DAYS.toMillis(1))
                        .build());
        assertFalse(changedByRelay.isSignatureV1Invalid());
        AuthenticatedDataStorageService storeWithoutEntry = new AuthenticatedDataStorageService(
                new PersistenceService(tempDir), new PruneExpiredEntriesService(), "withoutEntry", STORE_KEY, () -> time);

        time = AT_CUTOFF;
        DataStorageResult result = peerStore.remove(changedByRelay);
        DataStorageResult resultWithoutEntry = storeWithoutEntry.remove(changedByRelay);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureV2Missing(), result.toString());
        assertSame(add, peerStore.getPersistableStore().getMap().get(add.getStorageKey()));
        // No entry is stored which would keep the changed sequence number
        assertTrue(resultWithoutEntry.isSignatureV2Missing(), resultWithoutEntry.toString());
        assertTrue(storeWithoutEntry.getPersistableStore().getMap().isEmpty());
        // The removal of the owner is still accepted
        assertTrue(peerStore.remove(remove).isSuccess());

        time = BEFORE_CUTOFF;
        assertTrue(storeWithoutEntry.remove(changedByRelay).isSuccess());
    }

    // A node of an earlier version removes signature V2 when it relays an add request. A node which got such a copy
    // first can still apply the refresh request of the owner, because the data of the copy is the data which the owner
    // signed. The refreshed entry then has signature V2.
    @Test
    void refreshOfCopyWithoutSignatureV2CreatesEntryWithSignatureV2() throws GeneralSecurityException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);
        assertTrue(ownerStore.add(addRequest).isSuccess());
        assertTrue(peerStore.add(withoutSignatureV2(addRequest)).isSuccess());
        RefreshAuthenticatedDataRequest refreshRequest = RefreshAuthenticatedDataRequest.from(ownerStore, authenticatedData, ownerKeyPair);

        DataStorageResult result = peerStore.refresh(refreshRequest);

        assertTrue(result.isSuccess(), result.toString());
        AuthenticatedDataRequest storedRequest = peerStore.getPersistableStore().getMap().get(refreshRequest.getStorageKey());
        assertTrue(storedRequest.isSignatureV2Present());
        assertFalse(storedRequest.isSignatureInvalid(AT_CUTOFF));
    }

    // Oracle nodes of earlier versions created their entries without signature V2. The transition rule for the owner of
    // authorized data depends on the creation date of the entry, the rule for signature V2 on the time of the node.
    // So from the cutoff date of signature V1 on, the store accepts no entry of an earlier version anymore.
    @Test
    void entryOfEarlierVersionWithLegacyOwnerIsAcceptedOnlyBeforeTheCutoffDateOfSignatureV1() throws GeneralSecurityException {
        AuthorizedData legacyType = authorize(TestAuthorizedDistributedData.withLegacyOwners("content"));
        KeyPair identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(legacyType,
                1,
                DigestUtil.hash(identityKeyPair.getPublic().getEncoded()),
                Math.min(System.currentTimeMillis(), AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime() - 1));
        // Another owner, which the transition rule accepts. Otherwise the sequential data could not be created.
        assertFalse(legacyType.isOwner(sequentialData.getPubKeyHash()));
        AddAuthenticatedDataRequest entryOfEarlierVersion = new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), identityKeyPair.getPrivate()),
                identityKeyPair.getPublic().getEncoded(),
                identityKeyPair.getPublic(),
                new byte[]{},
                0);

        time = AT_CUTOFF;
        DataStorageResult result = authorizedDataStore.add(entryOfEarlierVersion);

        assertTrue(result.isSignatureV2Missing(), result.toString());
        assertTrue(authorizedDataStore.getPersistableStore().getMap().isEmpty());

        time = BEFORE_CUTOFF;
        assertTrue(authorizedDataStore.add(entryOfEarlierVersion).isSuccess());
    }

    // A store which accepted requests without signature V2 does not keep them after the cutoff date. It would not
    // accept them anymore, and other nodes do not accept them from its inventory.
    @Test
    void entriesWithoutSignatureV2AreRemovedAtLoadFromTheCutoffDateOn()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest legacyAdd = withoutSignatureV2(
                AddAuthenticatedDataRequest.from(peerStore, authenticatedData, ownerKeyPair));
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(peerStore, createData("removed 1"),
                ownerKeyPair);
        RemoveAuthenticatedDataRequest legacyRemoval = RemoveAuthenticatedDataRequest.fromProto(
                RemoveAuthenticatedDataRequest.from(peerStore, createData("removed 2"), ownerKeyPair)
                        .toValueProto(false).toBuilder().setVersion(1).clearSignatureV2().build());
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(peerStore, createData("other content"),
                ownerKeyPair);
        assertTrue(peerStore.add(legacyAdd).isSuccess());
        assertTrue(peerStore.remove(legacyRemoval).isSuccess());
        assertTrue(peerStore.add(add).isSuccess());
        assertTrue(peerStore.remove(removal).isSuccess());
        Map<ByteArray, AuthenticatedDataRequest> map = peerStore.getPersistableStore().getMap();

        peerStore.prunePersisted(peerStore.getPersistableStore());
        assertEquals(4, map.size());

        time = AT_CUTOFF;
        peerStore.prunePersisted(peerStore.getPersistableStore());

        assertEquals(Map.of(add.getStorageKey(), add, removal.getStorageKey(), removal), map);
        assertEquals(Set.of(legacyAdd, legacyRemoval), peerStore.getPrunedAndExpiredDataRequests().getUnmodifiableSet());
    }

    // A node which runs at the cutoff date removes the entries with the task which removes expired entries. The
    // applications get the removal of the data, as at expiry.
    @Test
    void runningNodeRemovesEntriesWithoutSignatureV2FromTheCutoffDateOn() throws GeneralSecurityException {
        AtomicReference<Runnable> pruneExpiredTask = new AtomicReference<>();
        AuthenticatedDataStorageService store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService() {
                    @Override
                    public void addTask(Runnable task) {
                        pruneExpiredTask.set(task);
                    }
                },
                "running",
                STORE_KEY,
                () -> time);
        List<AuthenticatedData> removedData = new ArrayList<>();
        store.addListener(new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                removedData.add(authenticatedData);
            }
        });
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest legacyAdd = withoutSignatureV2(
                AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair));
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(store, createData("other content"),
                ownerKeyPair);
        assertTrue(store.add(legacyAdd).isSuccess());
        assertTrue(store.add(add).isSuccess());

        pruneExpiredTask.get().run();
        assertEquals(2, store.getPersistableStore().getMap().size());
        assertTrue(removedData.isEmpty());

        time = AT_CUTOFF;
        pruneExpiredTask.get().run();

        assertEquals(Map.of(add.getStorageKey(), add), store.getPersistableStore().getMap());
        assertEquals(List.of(authenticatedData), removedData);
    }

    // Most received add requests are copies of stored requests. A copy without signature V2, which a node of an earlier
    // version relayed, is not equal to the stored request. The store rejects it by its sequence number before it
    // verifies the signature, which is the most expensive check (finding F12.3 of the review
    // 2026-09-29_16-18-18-fable-5.1). A copy with an invalid signature shows the order of the checks.
    @Test
    void copyWithoutSignatureV2IsRejectedBeforeItsSignatureIsVerified() throws GeneralSecurityException {
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(peerStore, authenticatedData,
                ownerKeyPair);
        assertTrue(peerStore.add(request).isSuccess());
        AddAuthenticatedDataRequest copyWithInvalidSignature = new AddAuthenticatedDataRequest(
                request.getAuthenticatedSequentialData(),
                SignatureUtil.sign(new byte[]{1}, ownerKeyPair.getPrivate()),
                request.getSignaturePubKeyEncoded(),
                request.getSignaturePublicKey(),
                new byte[0],
                0);
        assertTrue(copyWithInvalidSignature.isSignatureInvalid(time));

        DataStorageResult result = peerStore.add(copyWithInvalidSignature);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertFalse(result.isSignatureInvalid());
        assertEquals(request, peerStore.getPersistableStore().getMap().get(request.getStorageKey()));
    }

    // At the cutoff date the prune task selects many entries at once. A request of the owner can replace an entry after
    // the task selected it. The task must not remove the new entry (finding F12.1 of the review
    // 2026-09-29_16-18-18-fable-5.1).
    @Test
    void pruneTaskKeepsAnEntryWhichARequestReplacedAfterTheSelection() throws GeneralSecurityException {
        AtomicReference<Runnable> pruneExpiredTask = new AtomicReference<>();
        AtomicReference<AddAuthenticatedDataRequest> replacement = new AtomicReference<>();
        AuthenticatedDataStorageService store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService() {
                    @Override
                    public void addTask(Runnable task) {
                        pruneExpiredTask.set(task);
                    }
                },
                "running",
                STORE_KEY,
                () -> time) {
            @Override
            protected boolean isToPrune(AuthenticatedDataRequest dataRequest) {
                boolean isToPrune = super.isToPrune(dataRequest);
                if (isToPrune && replacement.get() != null) {
                    getPersistableStore().getMap().put(replacement.get().getStorageKey(), replacement.get());
                }
                return isToPrune;
            }
        };
        List<AuthenticatedData> removedData = new ArrayList<>();
        store.addListener(new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                removedData.add(authenticatedData);
            }
        });
        time = BEFORE_CUTOFF;
        AddAuthenticatedDataRequest legacyAdd = withoutSignatureV2(
                AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair));
        assertTrue(store.add(legacyAdd).isSuccess());
        AddAuthenticatedDataRequest addOfOwner = AddAuthenticatedDataRequest.from(store, authenticatedData,
                ownerKeyPair);
        replacement.set(addOfOwner);

        time = AT_CUTOFF;
        pruneExpiredTask.get().run();

        assertEquals(Map.of(addOfOwner.getStorageKey(), addOfOwner), store.getPersistableStore().getMap());
        assertTrue(removedData.isEmpty());
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    // A user must not publish the data of another user. The user could remove the copy, and the application would
    // remove the data of the owner (see docs/specifications/network/storage-ownership.md). Such a copy is invalid, so
    // it cannot be created.
    @Test
    void addSignedByAnotherKeyThanTheOwnerCannotBeCreated() {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertThrows(IllegalArgumentException.class,
                () -> AddAuthenticatedDataRequest.from(peerStore, authenticatedData, otherKeyPair));
    }

    // Distributed data which names no owner has no owner as default authenticated data. Authorized data names no
    // owner, so it cannot be stored as default authenticated data, which would skip its authorization check.
    @Test
    void defaultAuthenticatedDataWithoutOwnerCannotBePublished() {
        AuthenticatedData dataWithoutOwner = new DefaultAuthenticatedData(new TestAuthorizedDistributedData("content"));

        assertThrows(IllegalArgumentException.class,
                () -> AddAuthenticatedDataRequest.from(authorizedDataStore, dataWithoutOwner, authorizedKeyPair));
    }

    // The authorization signature does not cover the key which signs the add request. The authorized key therefore
    // owns the data.
    @Test
    void authorizedDataIsOwnedByTheAuthorizedKey() throws GeneralSecurityException {
        AuthorizedData authorizedData = authorize(new TestAuthorizedDistributedData("content"));
        AddAuthenticatedDataRequest ownerRequest = AddAuthenticatedDataRequest.from(authorizedDataStore,
                authorizedData, authorizedKeyPair);

        assertTrue(authorizedDataStore.add(ownerRequest).isSuccess());
        assertThrows(IllegalArgumentException.class,
                () -> AddAuthenticatedDataRequest.from(authorizedDataStore, authorizedData, ownerKeyPair));
    }

    // For example AuthorizedOracleNode, which the key of its network ID owns
    @Test
    void authorizedDataWhichNamesItsOwnerIsOwnedByThatOwner() throws GeneralSecurityException {
        StorableTypesRegistry.register(AuthorizedDataWithOwner.class, AuthorizedDataWithOwner.META_DATA);
        AuthenticatedDataStorageService store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(), "authorizedWithOwner", AuthorizedDataWithOwner.class.getSimpleName());
        AuthorizedData authorizedData = authorize(new AuthorizedDataWithOwner(ownerKeyPair.getPublic()));

        DataStorageResult resultOfOwner = store.add(AddAuthenticatedDataRequest.from(store, authorizedData,
                ownerKeyPair));

        assertTrue(resultOfOwner.isSuccess(), resultOfOwner.toString());
        assertThrows(IllegalArgumentException.class,
                () -> AddAuthenticatedDataRequest.from(store, authorizedData, authorizedKeyPair));
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
        assertFalse(AuthenticatedSequentialData.isOwnerInvalid(legacyType, otherPubKeyHash, created));
        assertTrue(AuthenticatedSequentialData.isOwnerInvalid(otherType, otherPubKeyHash, created));
        new AuthenticatedSequentialData(legacyType, 1, otherPubKeyHash, created);
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedSequentialData(otherType, 1, otherPubKeyHash, created));
        // A refresh replaces the creation date. The store checks the owner of the refreshed entry with the new date,
        // because the stored entry was valid with its own date. A refresh from the cutoff date on cannot be created
        // before that date, so only the rule is tested here.
        assertTrue(AuthenticatedSequentialData.isOwnerInvalid(legacyType, otherPubKeyHash, cutoffDate));
    }

    // Before the cutoff date, the transition rule also accepts the refresh of an entry of another owner
    @Test
    void refreshOfLegacyEntryOfAnotherOwnerIsAcceptedBeforeTheCutoffDate() throws GeneralSecurityException {
        time = BEFORE_CUTOFF;
        AuthorizedData legacyType = authorize(TestAuthorizedDistributedData.withLegacyOwners("content"));
        KeyPair identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        assertTrue(authorizedDataStore.add(AddAuthenticatedDataRequest.from(authorizedDataStore, legacyType,
                identityKeyPair)).isSuccess());

        DataStorageResult result = authorizedDataStore.refresh(RefreshAuthenticatedDataRequest.from(authorizedDataStore,
                legacyType, identityKeyPair));

        assertTrue(result.isSuccess(), result.toString());
    }

    // The store holds one entry per data and publisher. The listeners get both, so that they can tell the entries of the
    // same data apart.
    @Test
    void listenersGetTheKeyWhichPublishedTheData() throws GeneralSecurityException {
        List<String> events = new ArrayList<>();
        peerStore.addListener(new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                events.add("added by " + Hex.encode(publisherPubKeyHash));
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                events.add("removed by " + Hex.encode(publisherPubKeyHash));
            }

            @Override
            public void onRefreshed(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                events.add("refreshed by " + Hex.encode(publisherPubKeyHash));
            }
        });

        addToOwnerAndPeer();
        assertTrue(peerStore.refresh(RefreshAuthenticatedDataRequest.from(peerStore, authenticatedData, ownerKeyPair))
                .isSuccess());
        assertTrue(peerStore.remove(RemoveAuthenticatedDataRequest.from(peerStore, authenticatedData, ownerKeyPair))
                .isSuccess());

        String owner = Hex.encode(DigestUtil.hash(ownerKeyPair.getPublic().getEncoded()));
        assertEquals(List.of("added by " + owner, "refreshed by " + owner, "removed by " + owner), events);
    }

    // At expiry the listeners get the key which published the entry, also if it is not the owner of the data, as in
    // entries of the transition rule
    @Test
    void listenersGetTheKeyWhichPublishedExpiredData() throws GeneralSecurityException {
        AtomicReference<Runnable> pruneExpiredTask = new AtomicReference<>();
        AuthenticatedDataStorageService store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService() {
                    @Override
                    public void addTask(Runnable task) {
                        pruneExpiredTask.set(task);
                    }
                },
                "expiry",
                TestAuthorizedDistributedData.class.getSimpleName());
        List<byte[]> publishers = new ArrayList<>();
        store.addListener(new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                publishers.add(publisherPubKeyHash);
            }
        });
        KeyPair publisherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(
                authorize(TestAuthorizedDistributedData.withLegacyOwners("content")),
                1,
                DigestUtil.hash(publisherKeyPair.getPublic().getEncoded()),
                System.currentTimeMillis() - TestAuthorizedDistributedData.META_DATA.getTtl() - 1000);
        AddAuthenticatedDataRequest expiredRequest = new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), publisherKeyPair.getPrivate()),
                publisherKeyPair.getPublic().getEncoded(),
                publisherKeyPair.getPublic(),
                new byte[]{},
                0);
        store.getPersistableStore().getMap().put(expiredRequest.getStorageKey(), expiredRequest);

        pruneExpiredTask.get().run();

        assertEquals(1, publishers.size());
        assertArrayEquals(DigestUtil.hash(publisherKeyPair.getPublic().getEncoded()), publishers.get(0));
    }

    private AuthorizedData authorize(AuthorizedDistributedData data) {
        try {
            byte[] signature = SignatureUtil.sign(data.serializeForHash(), authorizedKeyPair.getPrivate());
            return new AuthorizedData(data, Optional.of(signature), authorizedKeyPair.getPublic());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private AuthenticatedData createData(String content) {
        return new DefaultAuthenticatedData(new TestDistributedData(content, ownerKeyPair.getPublic()));
    }

    private AddAuthenticatedDataRequest withoutSignatureV2(AddAuthenticatedDataRequest request) {
        return new AddAuthenticatedDataRequest(request.getAuthenticatedSequentialData(),
                request.getSignatureV1(),
                request.getSignaturePubKeyEncoded(),
                request.getSignaturePublicKey(),
                new byte[0],
                0);
    }

    // A removal as a relay can change it: without signature V2, and with a sequence number which signature V1 does not
    // cover
    private static RemoveAuthenticatedDataRequest withoutSignatureV2(RemoveAuthenticatedDataRequest request,
                                                                     int sequenceNumber)
            throws UnresolvableProtobufException {
        return RemoveAuthenticatedDataRequest.fromProto(request.toValueProto(false).toBuilder()
                .setVersion(1)
                .clearSignatureV2()
                .setSequenceNumber(sequenceNumber)
                .build());
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
        public byte[] getOwnerPubKeyHash() {
            return ownerPubKeyHash;
        }
    }
}
