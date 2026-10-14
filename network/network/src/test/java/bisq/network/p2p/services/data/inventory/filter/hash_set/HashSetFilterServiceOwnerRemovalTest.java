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

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestAuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStorageService;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.confidential.ack.AckMessage;
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
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// A requester of an earlier version (HASH_SET) accepts the removal of an add request only from the owner of that add
// request. A responder of this version does not send it a removal of another key than the owner of the data of an add
// request which the responder holds with the same storage key V1. See rule 11 of
// docs/specifications/network/inventory-hash-set-filter.md.
class HashSetFilterServiceOwnerRemovalTest {
    private static final int MAX_SIZE = 2_000_000;

    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService store;
    private MailboxDataStorageService mailboxStore;
    private StorageService storageService;
    // The authorized key pair, which owns the authorized data
    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    // The key pair of the network identity of an oracle node, with which earlier versions published the data
    private final KeyPair identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    // A key pair which owns no data
    private final KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir.resolve("authenticated")),
                new PruneExpiredEntriesService(),
                "inventory",
                TestAuthorizedDistributedData.class.getSimpleName(),
                CLOCK_BEFORE_CUTOFF);
        mailboxStore = new MailboxDataStorageService(new PersistenceService(tempDir.resolve("mailbox")),
                new PruneExpiredEntriesService(),
                "mailbox",
                AckMessage.class.getSimpleName(),
                CLOCK_BEFORE_CUTOFF);
        storageService = mock(StorageService.class);
        when(storageService.getAuthenticatedDataStoreMaps())
                .thenAnswer(invocation -> Stream.of(store.getPersistableStore().getMap()));
        when(storageService.getMailboxStoreMaps())
                .thenAnswer(invocation -> Stream.of(mailboxStore.getPersistableStore().getMap()));
        when(storageService.getAddAppendOnlyDataStoreMaps()).thenAnswer(invocation -> Stream.empty());
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void waitForPendingWrites() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        mailboxStore.getPersistence().persistAsync(mailboxStore.getPersistableStore().getClone()).join();
    }

    // Before rule 11 covered the owner, every response contained such removals again, also with a higher sequence
    // number than the add request. With the same sequence number, the filter entry of the add request already hides
    // the removal, because an entry of HASH_SET contains no owner.
    @Test
    void doesNotSendRemovalOfAnotherKeyToRequesterWithTheAddRequest() throws GeneralSecurityException {
        AuthorizedData data = authorize("content");
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(store, data, ownerKeyPair);
        assertThat(store.add(add).isSuccess()).isTrue();
        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, data, otherKeyPair)).isSuccess()).isTrue();
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(store, data, otherKeyPair);
        assertThat(store.remove(removal).isSuccess()).isTrue();
        assertThat(removal.getSequenceNumber()).isGreaterThan(add.getSequenceNumber());

        Inventory inventory = filterService(InventoryFilterType.HASH_SET)
                .createInventory(filterOfEarlierVersion(entryV1(add)));

        assertThat(inventory.getEntries()).isEmpty();
        assertThat(inventory.isMaxSizeReached()).isFalse();
    }

    // The requester stores the add request of the response, and then cannot store the removal of another key
    @Test
    void sendsOnlyTheAddRequestToRequesterWithoutIt() throws GeneralSecurityException {
        AuthorizedData data = authorize("content");
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(store, data, ownerKeyPair);
        assertThat(store.add(add).isSuccess()).isTrue();
        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, data, otherKeyPair)).isSuccess()).isTrue();

        Inventory inventory = filterService(InventoryFilterType.HASH_SET).createInventory(filterOfEarlierVersion());

        assertThat(entries(inventory)).containsExactly(add);
    }

    // Requesters of this version hold one request per storage key V2, so they store the removal of another key
    @Test
    void sendsRemovalOfAnotherKeyToRequesterOfThisVersion() throws GeneralSecurityException {
        AuthorizedData data = authorize("content");
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(store, data, ownerKeyPair);
        assertThat(store.add(add).isSuccess()).isTrue();
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(store, data, otherKeyPair);
        assertThat(store.remove(removal).isSuccess()).isTrue();

        Inventory inventory = filterService(InventoryFilterType.HASH_SET_V2)
                .createInventory(new HashSetFilter(InventoryFilterType.HASH_SET_V2, new ArrayList<>()));

        assertThat(entries(inventory)).containsExactlyInAnyOrder(add, removal);
    }

    // The owner is the key which the data names, not the key which signed the add request. During the migration of the
    // publisher, the responder can hold the entry of the network identity and the removal of the owner. A requester
    // which holds the entry of the owner needs that removal.
    @Test
    void sendsRemovalOfTheOwnerWhileTheEntryOfTheNetworkIdentityIsHeld() throws GeneralSecurityException {
        AuthorizedData data = authorize("content");
        AddAuthenticatedDataRequest entryOfOwner = AddAuthenticatedDataRequest.from(store, data, ownerKeyPair);
        assertThat(store.add(entryOfOwner).isSuccess()).isTrue();
        RemoveAuthenticatedDataRequest removalOfOwner = RemoveAuthenticatedDataRequest.from(store, data, ownerKeyPair);
        assertThat(store.remove(removalOfOwner).isSuccess()).isTrue();
        AddAuthenticatedDataRequest entryOfIdentity = createRequestWithoutSignatureV2(data, identityKeyPair,
                entryOfOwner.getSequenceNumber());
        assertThat(store.add(entryOfIdentity).isSuccess()).isTrue();
        assertThat(removalOfOwner.getSequenceNumber()).isGreaterThan(entryOfOwner.getSequenceNumber());

        Inventory inventory = filterService(InventoryFilterType.HASH_SET)
                .createInventory(filterOfEarlierVersion(entryV1(entryOfOwner)));

        assertThat(entries(inventory)).containsExactly(removalOfOwner);
    }

    @Test
    void doesNotSendMailboxRemovalOfAnotherKeyThanTheReceiver()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertThat(mailboxStore.add(add).isSuccess()).isTrue();
        RemoveMailboxRequest removal = RemoveMailboxRequest.from(mailboxData, otherKeyPair);
        assertThat(mailboxStore.remove(removal).isSuccess()).isTrue();

        Inventory responseOfEarlierVersion = filterService(InventoryFilterType.HASH_SET)
                .createInventory(filterOfEarlierVersion());
        Inventory responseOfThisVersion = filterService(InventoryFilterType.HASH_SET_V2)
                .createInventory(new HashSetFilter(InventoryFilterType.HASH_SET_V2, new ArrayList<>()));

        assertThat(entries(responseOfEarlierVersion)).containsExactly(add);
        assertThat(entries(responseOfThisVersion)).containsExactlyInAnyOrder(add, removal);
    }

    // The removal of the receiver replaces its add request, so the responder holds no add request with that storage
    // key V1, and the requester can store the removal
    @Test
    void sendsMailboxRemovalOfTheReceiver() throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        AddMailboxRequest add = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        assertThat(mailboxStore.add(add).isSuccess()).isTrue();
        RemoveMailboxRequest removal = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        assertThat(mailboxStore.remove(removal).isSuccess()).isTrue();

        Inventory inventory = filterService(InventoryFilterType.HASH_SET).createInventory(filterOfEarlierVersion());

        assertThat(entries(inventory)).containsExactly(removal);
    }

    private HashSetFilterService filterService(InventoryFilterType inventoryFilterType) {
        return new HashSetFilterService(storageService, MAX_SIZE, inventoryFilterType);
    }

    private static HashSetFilter filterOfEarlierVersion(HashSetFilterEntry... entries) {
        return new HashSetFilter(InventoryFilterType.HASH_SET, new ArrayList<>(List.of(entries)));
    }

    private static List<DataRequest> entries(Inventory inventory) {
        return new ArrayList<>(inventory.getEntries());
    }

    // The entry which a node of an earlier version computes for a request which it holds
    private static HashSetFilterEntry entryV1(AddAuthenticatedDataRequest request) {
        return new HashSetFilterEntry(request.getStorageKeyV1().getBytes(), request.getSequenceNumber());
    }

    private AuthorizedData authorize(String content) throws GeneralSecurityException {
        TestAuthorizedDistributedData data = TestAuthorizedDistributedData.withLegacyOwners(content);
        byte[] signature = SignatureUtil.sign(data.serializeForHash(), ownerKeyPair.getPrivate());
        return new AuthorizedData(data, Optional.of(signature), ownerKeyPair.getPublic());
    }

    // An add request as nodes of earlier versions created it: without signature V2
    private static AddAuthenticatedDataRequest createRequestWithoutSignatureV2(AuthorizedData data,
                                                                               KeyPair publisherKeyPair,
                                                                               int sequenceNumber)
            throws GeneralSecurityException {
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
}
