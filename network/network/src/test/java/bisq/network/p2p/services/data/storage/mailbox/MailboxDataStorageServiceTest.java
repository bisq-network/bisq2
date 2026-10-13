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

package bisq.network.p2p.services.data.storage.mailbox;

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.common.data.ByteArray;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * A mailbox store holds the messages of one MailboxMessage type. The message is encrypted, so the store checks the
 * class name which MailboxData and RemoveMailboxRequest carry.
 */
class MailboxDataStorageServiceTest {
    // A store holds the data of one type. Its key is the class name of that type.
    private static final String STORE_KEY = AckMessage.class.getSimpleName();

    @TempDir
    Path tempDir;

    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private MailboxDataStorageService store;
    private MailboxData mailboxData;
    // The time of the node. From SignedRequest.SIGNATURE_V1_CUTOFF_DATE on, it requires signature V2. The tests of
    // requests without signature V2 set a time before that date, so they do not depend on the date at which they run.
    private long time = AT_CUTOFF;
    private final AtomicReference<Runnable> pruneExpiredTask = new AtomicReference<>();

    @BeforeAll
    static void registerMetaData() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @BeforeEach
    void setUp() throws UnresolvableProtobufException {
        store = new MailboxDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService() {
                    @Override
                    public void addTask(Runnable task) {
                        pruneExpiredTask.set(task);
                    }
                },
                "mailbox",
                STORE_KEY,
                () -> time);
        mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
    }

    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void removalPreventsLaterAdd(boolean legacy, boolean addFirst)
            throws GeneralSecurityException, UnresolvableProtobufException {
        if (legacy) {
            time = BEFORE_CUTOFF;
        }
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        RemoveMailboxRequest remove = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        if (legacy) {
            remove = RemoveMailboxRequest.fromProto(remove.toValueProto(false).toBuilder()
                    .setVersion(1).clearSignatureV2().build());
        }
        if (addFirst) {
            assertTrue(store.add(add).isSuccess());
        }
        assertTrue(store.remove(remove).isSuccess());

        DataStorageResult result = store.add(add);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(remove, store.getPersistableStore().getMap().get(add.getStorageKey()));
        assertTrue(store.remove(remove).isAlreadyRemoved());
        assertSame(remove, store.getPersistableStore().getMap().get(add.getStorageKey()));
    }

    @Test
    void v2RemovalReplacesLegacyRemoval() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        RemoveMailboxRequest verified = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        RemoveMailboxRequest legacy = toLegacy(verified);
        assertTrue(store.remove(legacy).isSuccess());

        assertTrue(store.remove(verified).isAlreadyRemoved());

        assertSame(verified, store.getPersistableStore().getMap().get(add.getStorageKey()));
        assertTrue(store.add(add).isSequenceNrInvalid());
        assertSame(verified, store.getPersistableStore().getMap().get(add.getStorageKey()));
    }

    @Test
    void legacyRemovalDoesNotReplaceV2Removal() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveMailboxRequest verified = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        assertTrue(store.remove(verified).isSuccess());

        assertTrue(store.remove(toLegacy(verified)).isAlreadyRemoved());

        assertSame(verified, store.getPersistableStore().getMap().get(verified.getStorageKey()));
    }

    @Test
    void secondV2RemovalDoesNotReplaceFirst() throws GeneralSecurityException {
        RemoveMailboxRequest first = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        assertTrue(store.remove(first).isSuccess());
        RemoveMailboxRequest second = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        while (second.getCreated() == first.getCreated()) {
            second = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        }

        assertTrue(store.remove(second).isAlreadyRemoved());

        assertSame(first, store.getPersistableStore().getMap().get(first.getStorageKey()));
    }

    // The store accepts a removal only if it stores it. A node relays the removals which its store accepts (see
    // docs/specifications/network/storage-request-relay.md).
    @Test
    void removalIsAcceptedOnlyIfTheStoreStoresIt() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveMailboxRequest verified = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        RemoveMailboxRequest legacy = toLegacy(verified);
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        MailboxDataStorageService storeWithAdd = new MailboxDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(), "mailboxWithAdd", STORE_KEY, () -> time);
        assertTrue(storeWithAdd.add(add).isSuccess());

        DataStorageResult withoutEntry = store.remove(legacy);
        DataStorageResult replacement = store.remove(verified);
        DataStorageResult repetition = store.remove(verified);
        DataStorageResult legacyAfterReplacement = store.remove(legacy);
        DataStorageResult removalOfData = storeWithAdd.remove(verified);

        assertTrue(withoutEntry.isSuccess() && withoutEntry.isNoEntry(), withoutEntry.toString());
        assertTrue(replacement.isSuccess() && replacement.isAlreadyRemoved(), replacement.toString());
        assertFalse(repetition.isSuccess(), repetition.toString());
        assertTrue(repetition.isAlreadyRemoved(), repetition.toString());
        assertFalse(legacyAfterReplacement.isSuccess(), legacyAfterReplacement.toString());
        assertSame(verified, store.getPersistableStore().getMap().get(verified.getStorageKey()));
        assertTrue(removalOfData.isSuccess() && !removalOfData.isAlreadyRemoved(), removalOfData.toString());
        storeWithAdd.getPersistence().persistAsync(storeWithAdd.getPersistableStore().getClone()).join();
    }

    @Test
    void invalidSignatureV2CannotReplaceLegacyRemoval() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        RemoveMailboxRequest verified = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        RemoveMailboxRequest legacy = toLegacy(verified);
        assertTrue(store.remove(legacy).isSuccess());
        // Signature V2 covers created
        RemoveMailboxRequest invalid = RemoveMailboxRequest.fromProto(verified.toValueProto(false).toBuilder()
                .setCreated(verified.getCreated() + 1).build());

        assertTrue(store.remove(invalid).isSignatureInvalid());

        assertSame(legacy, store.getPersistableStore().getMap().get(legacy.getStorageKey()));
    }

    // The store records with a removal which replaces an add request the sequence number of that add request, so it
    // knows that it checked the removed message. A removal which replaces a stored removal keeps the number (see
    // docs/specifications/network/storage-request-ordering.md).
    @Test
    void removalRecordsTheSequenceNumberOfTheAddWhichItReplaced()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        RemoveMailboxRequest verified = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        assertTrue(store.add(add).isSuccess());
        Map<ByteArray, Integer> replacedAddSequenceNrs =
                store.getPersistableStore().getReplacedAddSequenceNrs();

        assertTrue(store.remove(toLegacy(verified)).isSuccess());
        assertEquals(Map.of(add.getStorageKey(), 1), replacedAddSequenceNrs);
        assertTrue(store.remove(verified).isSuccess());
        assertEquals(Map.of(add.getStorageKey(), 1), replacedAddSequenceNrs);
    }

    // A store which holds no entry for the key of a removal did not check the removed message
    @Test
    void removalWithoutEntryRecordsNoSequenceNumber() throws GeneralSecurityException {
        DataStorageResult result = store.remove(RemoveMailboxRequest.from(mailboxData, receiverKeyPair));

        assertTrue(result.isSuccess() && result.isNoEntry(), result.toString());
        assertTrue(store.getPersistableStore().getReplacedAddSequenceNrs().isEmpty());
    }

    // The store removes the number together with the removal when the removal expires
    @Test
    void replacedAddSequenceNrIsRemovedWithTheRemoval() throws GeneralSecurityException {
        AtomicReference<Runnable> pruneTask = new AtomicReference<>();
        MailboxDataStorageService store = new MailboxDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService() {
                    @Override
                    public void addTask(Runnable task) {
                        pruneTask.set(task);
                    }
                },
                "pruneRemoval",
                STORE_KEY,
                () -> time) {
            // As if the removal had expired
            @Override
            protected boolean isToPrune(MailboxRequest request) {
                return request instanceof RemoveMailboxRequest;
            }
        };
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertTrue(store.add(add).isSuccess());
        assertTrue(store.remove(RemoveMailboxRequest.from(mailboxData, receiverKeyPair)).isSuccess());
        assertEquals(Map.of(add.getStorageKey(), 1),
                store.getPersistableStore().getReplacedAddSequenceNrs());

        pruneTask.get().run();

        assertTrue(store.getPersistableStore().getMap().isEmpty());
        assertTrue(store.getPersistableStore().getReplacedAddSequenceNrs().isEmpty());
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    private static RemoveMailboxRequest toLegacy(RemoveMailboxRequest request) throws UnresolvableProtobufException {
        return RemoveMailboxRequest.fromProto(request.toValueProto(false).toBuilder()
                .setVersion(1).clearSignatureV2().build());
    }

    private static AddMailboxRequest toLegacy(AddMailboxRequest request) throws UnresolvableProtobufException {
        return AddMailboxRequest.fromProto(request.toValueProto(false).toBuilder()
                .clearVersion().clearSignatureV2().build());
    }

    // A copy without signature V2, which a node of an earlier version relayed, is not equal to the stored request. The
    // store rejects it by its sequence number before it verifies the signature, which is the most expensive check
    // (finding F12.3 of the review 2026-09-29_16-18-18-fable-5.1). A copy with an invalid signature shows the order.
    @Test
    void copyWithoutSignatureV2IsRejectedBeforeItsSignatureIsVerified()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertTrue(store.add(add).isSuccess());
        AddMailboxRequest copyWithInvalidSignature = AddMailboxRequest.fromProto(add.toValueProto(false).toBuilder()
                .clearVersion()
                .clearSignatureV2()
                .setSignature(ByteString.copyFrom(SignatureUtil.sign(new byte[]{1}, senderKeyPair.getPrivate())))
                .build());
        assertTrue(copyWithInvalidSignature.isSignatureInvalid(time));

        DataStorageResult result = store.add(copyWithInvalidSignature);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertFalse(result.isSignatureInvalid());
        assertSame(add, store.getPersistableStore().getMap().get(add.getStorageKey()));
    }

    // A relay of an earlier version removes signature V2, and the store holds that copy. The complete request of the
    // sender replaces it, so the store keeps the message from the cutoff date on (see
    // docs/specifications/network/storage-request-ordering.md).
    @Test
    void completeRequestReplacesItsCopyWithoutSignatureV2()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest complete = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertTrue(store.add(toLegacy(complete)).isSuccess());

        DataStorageResult result = store.add(complete);

        assertTrue(result.isSuccess(), result.toString());
        assertSame(complete, store.getPersistableStore().getMap().get(complete.getStorageKey()));
        assertTrue(store.add(toLegacy(complete)).isSequenceNrInvalid());

        time = AT_CUTOFF;
        store.prunePersisted(store.getPersistableStore());
        pruneExpiredTask.get().run();

        assertSame(complete, store.getPersistableStore().getMap().get(complete.getStorageKey()));
    }

    // Only a stored copy without signature V2 is replaced, so each node stores and relays the complete request at most
    // once (see docs/specifications/network/storage-request-ordering.md). The sender can create further complete
    // versions of the request: ECDSA signing is randomized, so a new signature V2 over the same preimage differs.
    @Test
    void secondCompleteRequestDoesNotReplaceTheFirst() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest complete = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertTrue(store.add(toLegacy(complete)).isSuccess());
        assertTrue(store.add(complete).isSuccess());
        AddMailboxRequest resigned = AddMailboxRequest.fromProto(complete.toValueProto(false).toBuilder()
                .setSignatureV2(ByteString.copyFrom(SignatureUtil.sign(complete.getPreImageV2(),
                        senderKeyPair.getPrivate())))
                .build());
        assertFalse(Arrays.equals(complete.getSignatureV2(), resigned.getSignatureV2()));
        assertFalse(resigned.isSignatureV2Invalid());

        DataStorageResult result = store.add(resigned);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(complete, store.getPersistableStore().getMap().get(complete.getStorageKey()));
    }

    // The receiver removed the message. The complete request of a copy which the store held before does not bring the
    // message back.
    @Test
    void removalStillRejectsTheCompleteRequestOfACopy() throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest complete = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertTrue(store.add(toLegacy(complete)).isSuccess());
        RemoveMailboxRequest removal = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        assertTrue(store.remove(removal).isSuccess());

        DataStorageResult result = store.add(complete);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(removal, store.getPersistableStore().getMap().get(complete.getStorageKey()));
    }

    // From the cutoff date of signature V1 on, a node accepts only requests with signature V2 (see
    // docs/specifications/network/storage-request-signatures.md). The same request is accepted before that date.
    @Test
    void addWithoutSignatureV2IsAcceptedOnlyBeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest legacy = toLegacy(AddMailboxRequest.from(mailboxData, senderKeyPair,
                receiverKeyPair.getPublic()));
        assertFalse(legacy.isSignatureV1Invalid());

        time = AT_CUTOFF;
        DataStorageResult result = store.add(legacy);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureV2Missing(), result.toString());
        // Nodes of earlier versions send such requests
        assertFalse(result.isSevereFailure(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());

        time = BEFORE_CUTOFF;
        assertTrue(store.add(legacy).isSuccess());
    }

    // Signature V1 of a removal does not cover the creation date, so the node uses its own time
    @Test
    void removalWithoutSignatureV2IsAcceptedOnlyBeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertTrue(store.add(add).isSuccess());
        RemoveMailboxRequest remove = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        RemoveMailboxRequest changedByRelay = RemoveMailboxRequest.fromProto(remove.toValueProto(false).toBuilder()
                .setVersion(1)
                .clearSignatureV2()
                .setCreated(remove.getCreated() - 1000)
                .build());
        assertFalse(changedByRelay.isSignatureV1Invalid());

        time = AT_CUTOFF;
        DataStorageResult result = store.remove(changedByRelay);

        assertFalse(result.isSuccess());
        assertTrue(result.isSignatureV2Missing(), result.toString());
        assertSame(add, store.getPersistableStore().getMap().get(add.getStorageKey()));

        time = BEFORE_CUTOFF;
        assertTrue(store.remove(changedByRelay).isSuccess());
    }

    // A store which accepted add requests without signature V2 does not keep them after the cutoff date: neither when
    // the node loads the store, nor while the node runs. It keeps the remove requests until they expire.
    @Test
    void addRequestsWithoutSignatureV2AreRemovedFromTheCutoffDateOn()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        // The storage key contains the key of the receiver, so each receiver has an entry of its own
        AddMailboxRequest legacyAdd = toLegacy(AddMailboxRequest.from(mailboxData, senderKeyPair,
                receiverKeyPair.getPublic()));
        RemoveMailboxRequest legacyRemoval = toLegacy(RemoveMailboxRequest.from(mailboxData,
                KeyGeneration.generateDefaultEcKeyPair()));
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair,
                KeyGeneration.generateDefaultEcKeyPair().getPublic());
        RemoveMailboxRequest removal = RemoveMailboxRequest.from(mailboxData, KeyGeneration.generateDefaultEcKeyPair());
        Map<ByteArray, MailboxRequest> entries = Map.of(legacyAdd.getStorageKey(), legacyAdd,
                legacyRemoval.getStorageKey(), legacyRemoval,
                add.getStorageKey(), add,
                removal.getStorageKey(), removal);
        assertTrue(store.add(legacyAdd).isSuccess());
        assertTrue(store.remove(legacyRemoval).isSuccess());
        assertTrue(store.add(add).isSuccess());
        assertTrue(store.remove(removal).isSuccess());
        Map<ByteArray, MailboxRequest> map = store.getPersistableStore().getMap();

        store.prunePersisted(store.getPersistableStore());
        pruneExpiredTask.get().run();
        assertEquals(entries, map);

        Map<ByteArray, MailboxRequest> entriesFromTheCutoffDateOn = Map.of(legacyRemoval.getStorageKey(), legacyRemoval,
                add.getStorageKey(), add,
                removal.getStorageKey(), removal);

        time = AT_CUTOFF;
        pruneExpiredTask.get().run();
        assertEquals(entriesFromTheCutoffDateOn, map);

        map.putAll(entries);
        store.prunePersisted(store.getPersistableStore());
        assertEquals(entriesFromTheCutoffDateOn, map);
    }

    // A node parses the persisted requests when it loads a store. From the cutoff date on, the store still keeps the
    // removals without signature V2 (rule 9 of docs/specifications/network/storage-request-signatures.md), so they
    // must pass this step. The constructors of the requests read the system time, so the test replaces the cutoff
    // rule for its thread with "signature V2 is required".
    @Test
    void removalWithoutSignatureV2IsKeptWhenTheStoreIsLoadedFromTheCutoffDateOn()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        // The storage key contains the key of the receiver, so each receiver has an entry of its own
        AddMailboxRequest legacyAdd = toLegacy(AddMailboxRequest.from(mailboxData, senderKeyPair,
                receiverKeyPair.getPublic()));
        RemoveMailboxRequest legacyRemoval = toLegacy(RemoveMailboxRequest.from(mailboxData,
                KeyGeneration.generateDefaultEcKeyPair()));
        assertTrue(store.add(legacyAdd).isSuccess());
        assertTrue(store.remove(legacyRemoval).isSuccess());
        bisq.network.protobuf.DataStore persisted = store.getPersistableStore().toProto(false);

        time = AT_CUTOFF;
        try (MockedStatic<SignedRequest> signedRequest = mockStatic(SignedRequest.class, CALLS_REAL_METHODS)) {
            signedRequest.when(() -> SignedRequest.isSignatureV2Required(anyLong())).thenReturn(true);
            @SuppressWarnings("unchecked")
            DataStore<MailboxRequest> loaded = (DataStore<MailboxRequest>) DataStore.fromProto(persisted);
            store.prunePersisted(loaded);

            assertEquals(Map.of(legacyRemoval.getStorageKey(), legacyRemoval), loaded.getMap());
        }
    }

    // The prune task selects the entries to remove and removes them afterwards. The removal of the receiver can replace
    // a selected add request in between. The removal must stay: without it, the store would accept the add request
    // again, and the receiver would get the message again.
    @Test
    void pruneTaskKeepsARemovalWhichReplacedASelectedAddRequest()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AtomicReference<Runnable> pruneTask = new AtomicReference<>();
        AtomicReference<RemoveMailboxRequest> removal = new AtomicReference<>();
        MailboxDataStorageService store = new MailboxDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService() {
                    @Override
                    public void addTask(Runnable task) {
                        pruneTask.set(task);
                    }
                },
                "pruneAndReplace",
                STORE_KEY,
                () -> time) {
            // Replaces the entry after the task has selected it, as a request of another thread can do
            @Override
            protected boolean isToPrune(MailboxRequest request) {
                boolean isToPrune = super.isToPrune(request);
                if (isToPrune) {
                    getPersistableStore().getMap().put(request.getStorageKey(), removal.get());
                }
                return isToPrune;
            }
        };
        AddMailboxRequest legacyAdd = toLegacy(AddMailboxRequest.from(mailboxData, senderKeyPair,
                receiverKeyPair.getPublic()));
        assertTrue(store.add(legacyAdd).isSuccess());
        removal.set(RemoveMailboxRequest.from(mailboxData, receiverKeyPair));
        assertEquals(legacyAdd.getStorageKey(), removal.get().getStorageKey());

        // From the cutoff date on, the task selects the add request without signature V2
        time = AT_CUTOFF;
        pruneTask.get().run();

        assertSame(removal.get(), store.getPersistableStore().getMap().get(legacyAdd.getStorageKey()));
        assertTrue(store.getPrunedAndExpiredDataRequests().isEmpty());
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    // The receiver removed the message with a node of an earlier version. The remove request still rejects the add
    // request from the cutoff date on. Otherwise the receiver would get the message again.
    @Test
    void removalWithoutSignatureV2StillPreventsLaterAddFromTheCutoffDateOn()
            throws GeneralSecurityException, UnresolvableProtobufException {
        time = BEFORE_CUTOFF;
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        RemoveMailboxRequest legacyRemoval = toLegacy(RemoveMailboxRequest.from(mailboxData, receiverKeyPair));
        assertTrue(store.add(add).isSuccess());
        assertTrue(store.remove(legacyRemoval).isSuccess());

        time = AT_CUTOFF;
        store.prunePersisted(store.getPersistableStore());
        pruneExpiredTask.get().run();
        DataStorageResult result = store.add(add);

        assertTrue(result.isSequenceNrInvalid(), result.toString());
        assertSame(legacyRemoval, store.getPersistableStore().getMap().get(add.getStorageKey()));
    }

    // The sender of the confidential message must sign the add request (see
    // docs/specifications/network/storage-ownership.md). The request is invalid, so it cannot be created.
    @Test
    void addRequestOfAnotherSenderCannotBeCreated() {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertThrows(IllegalArgumentException.class,
                () -> AddMailboxRequest.from(mailboxData, otherKeyPair, receiverKeyPair.getPublic()));
    }

    // A received add request of another sender fails at parsing, so it never reaches the store
    @Test
    void addRequestOfAnotherSenderIsRejectedAtParsing() throws GeneralSecurityException {
        AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        // The publisher hash still names the key of the request, so only the sender rule is broken
        byte[] otherPublicKeyBytes = KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded();
        bisq.network.protobuf.AddMailboxRequest.Builder builder = addRequest.toValueProto(false).toBuilder()
                .setSenderPublicKeyBytes(ByteString.copyFrom(otherPublicKeyBytes));
        builder.getMailboxSequentialDataBuilder()
                .setSenderPublicKeyHash(ByteString.copyFrom(DigestUtil.hash(otherPublicKeyBytes)));

        assertThrows(UnresolvableProtobufException.class, () -> AddMailboxRequest.fromProto(builder.build()));
    }

    // The store cannot check a removal without entry against the removed data, so every key pair can create it. Such
    // removals must not fill the store (see rule 12 of docs/specifications/network/storage-metadata.md).
    @Test
    void storeWithManyRemovalsAcceptsNoRemovalWithoutEntryButStillAddRequests()
            throws GeneralSecurityException, UnresolvableProtobufException {
        Map<ByteArray, MailboxRequest> map = store.getPersistableStore().getMap();
        RemoveMailboxRequest storedRemoval = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        // The limit counts removals without replaced add request, so the same removal under other keys fills the map
        for (int i = 0; i < DataStorageService.MAX_REMOVALS_WITHOUT_REPLACED_ADD - 1; i++) {
            map.put(new ByteArray(ByteBuffer.allocate(Integer.BYTES).putInt(i).array()), storedRemoval);
        }
        // Messages of other senders, so that the removals have other storage keys
        MailboxData dataOfRemovalBelowLimit = new MailboxData(AckMessage.META_DATA,
                createConfidentialMessage(KeyGeneration.generateDefaultEcKeyPair()));
        MailboxData dataOfRemovalAtLimit = new MailboxData(AckMessage.META_DATA,
                createConfidentialMessage(KeyGeneration.generateDefaultEcKeyPair()));

        DataStorageResult resultBelowLimit = store.remove(RemoveMailboxRequest.from(dataOfRemovalBelowLimit,
                receiverKeyPair));
        DataStorageResult resultAtLimit = store.remove(RemoveMailboxRequest.from(dataOfRemovalAtLimit,
                receiverKeyPair));
        DataStorageResult resultOfAdd = store.add(AddMailboxRequest.from(mailboxData, senderKeyPair,
                receiverKeyPair.getPublic()));
        DataStorageResult resultOfRemovalOfStoredData = store.remove(RemoveMailboxRequest.from(mailboxData,
                receiverKeyPair));

        assertTrue(resultBelowLimit.isSuccess() && resultBelowLimit.isNoEntry(), resultBelowLimit.toString());
        assertFalse(resultAtLimit.isSuccess(), resultAtLimit.toString());
        assertTrue(resultAtLimit.isMaxMapSizeReached(), resultAtLimit.toString());
        assertTrue(resultOfAdd.isSuccess(), resultOfAdd.toString());
        assertTrue(resultOfRemovalOfStoredData.isSuccess(), resultOfRemovalOfStoredData.toString());
        assertFalse(resultOfRemovalOfStoredData.isNoEntry(), resultOfRemovalOfStoredData.toString());
        assertEquals(DataStorageService.MAX_REMOVALS_WITHOUT_REPLACED_ADD + 1, map.size());
        // The pending write would have to serialize all entries
        map.clear();
        store.getPersistableStore().getReplacedAddSequenceNrs().clear();
    }

    // The store checked the removed message of a removal with replaced add request. Such removals do not count against
    // the limit (see rule 12 of docs/specifications/network/storage-metadata.md).
    @Test
    void removalsWithReplacedAddDoNotLimitTheRemovalsWithoutEntry()
            throws GeneralSecurityException, UnresolvableProtobufException {
        Map<ByteArray, MailboxRequest> map = store.getPersistableStore().getMap();
        Map<ByteArray, Integer> replacedAddSequenceNrs =
                store.getPersistableStore().getReplacedAddSequenceNrs();
        RemoveMailboxRequest storedRemoval = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        for (int i = 0; i < DataStorageService.MAX_REMOVALS_WITHOUT_REPLACED_ADD; i++) {
            ByteArray storageKey = new ByteArray(ByteBuffer.allocate(Integer.BYTES).putInt(i).array());
            map.put(storageKey, storedRemoval);
            replacedAddSequenceNrs.put(storageKey, 1);
        }
        // The message of another sender, so that the removal has another storage key
        MailboxData otherData = new MailboxData(AckMessage.META_DATA,
                createConfidentialMessage(KeyGeneration.generateDefaultEcKeyPair()));

        DataStorageResult result = store.remove(RemoveMailboxRequest.from(otherData, receiverKeyPair));

        assertTrue(result.isSuccess() && result.isNoEntry(), result.toString());
        assertEquals(DataStorageService.MAX_REMOVALS_WITHOUT_REPLACED_ADD + 1, map.size());
        // The pending write would have to serialize all entries
        map.clear();
        replacedAddSequenceNrs.clear();
    }

    @Test
    void addAndRemoveRequestsOfStoreTypeAreAccepted() throws GeneralSecurityException {
        AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        RemoveMailboxRequest removeRequest = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);

        DataStorageResult addResult = store.add(addRequest);
        DataStorageResult removeResult = store.remove(removeRequest);

        assertTrue(addResult.isSuccess(), addResult.toString());
        assertTrue(removeResult.isSuccess(), removeResult.toString());
        // The remove request carries only the hash of the mailbox data. It must find the entry of the add request.
        assertEquals(addRequest.getStorageKey(), removeRequest.getStorageKey());
        assertFalse(removeResult.isNoEntry(), removeResult.toString());
    }

    @Test
    void addRequestWithClassNameOfOtherTypeIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        // A relay can change className, because signature V1 does not cover it. It changes the className of the
        // deprecated metaData as well, otherwise parsing rejects the request.
        bisq.network.protobuf.AddMailboxRequest.Builder builder = addRequest.toValueProto(false).toBuilder();
        //noinspection deprecation
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder()
                .setClassName("OtherMailboxMessage")
                .setMetaData(new MetaData(MetaData.TTL_10_DAYS, "OtherMailboxMessage").toProto(false));
        AddMailboxRequest requestWithOtherClassName = AddMailboxRequest.fromProto(builder.build());

        DataStorageResult result = store.add(requestWithOtherClassName);

        assertFalse(result.isSuccess());
        assertTrue(result.isClassNameInvalid(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }

    @Test
    void removeRequestWithClassNameOfOtherTypeIsRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveMailboxRequest removeRequest = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        //noinspection deprecation
        RemoveMailboxRequest requestWithOtherClassName = RemoveMailboxRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setClassName("OtherMailboxMessage")
                        .setMetaData(new MetaData(MetaData.TTL_10_DAYS, "OtherMailboxMessage").toProto(false))
                        .build());

        DataStorageResult result = store.remove(requestWithOtherClassName);

        assertFalse(result.isSuccess());
        assertTrue(result.isClassNameInvalid(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }
}
