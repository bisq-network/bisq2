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

import bisq.common.data.ByteArray;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.persistence.PersistenceService;
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

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    // docs/specifications/network/storage-ownership.md)
    @Test
    void addRequestOfAnotherSenderIsRejected() throws GeneralSecurityException {
        AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, KeyGeneration.generateDefaultEcKeyPair(),
                receiverKeyPair.getPublic());
        assertFalse(addRequest.isSignatureInvalid());

        DataStorageResult result = store.add(addRequest);

        assertFalse(result.isSuccess());
        assertTrue(result.isOwnerInvalid(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
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
        // A relay can change className, because the signature does not cover it
        bisq.network.protobuf.AddMailboxRequest.Builder builder = addRequest.toValueProto(false).toBuilder();
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder().setClassName("OtherMailboxMessage");
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
        RemoveMailboxRequest requestWithOtherClassName = RemoveMailboxRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setClassName("OtherMailboxMessage")
                        .build());

        DataStorageResult result = store.remove(requestWithOtherClassName);

        assertFalse(result.isSuccess());
        assertTrue(result.isClassNameInvalid(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }
}
