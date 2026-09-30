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

package bisq.network.p2p.services.data;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.broadcast.Broadcaster;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageData;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A node relays a received request if its store accepted the request, also a removal which removed no data (see
 * docs/specifications/network/storage-request-relay.md). Requests without signature V2 are accepted only before
 * SignedRequest.SIGNATURE_V1_CUTOFF_DATE, so the node of these tests has a time before that date.
 */
class DataServiceRelayTest {
    @TempDir
    Path tempDir;

    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final List<BroadcastMessage> relayedRequests = new ArrayList<>();
    private final List<StorageData> removedData = new ArrayList<>();
    private DataService dataService;
    private AuthenticatedDataStorageService authenticatedDataStore;
    private AuthenticatedData authenticatedData;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @BeforeEach
    void setUp() {
        dataService = new DataService(new PersistenceService(tempDir), () -> BEFORE_CUTOFF);
        dataService.addBroadcaster(new Broadcaster(null) {
            @Override
            public CompletableFuture<BroadcastResult> reBroadcast(BroadcastMessage broadcastMessage) {
                relayedRequests.add(broadcastMessage);
                return CompletableFuture.completedFuture(new BroadcastResult(0, 0, 0));
            }
        });
        dataService.addListener(new DataService.Listener() {
            @Override
            public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
                removedData.add(authenticatedData);
            }

            @Override
            public void onMailboxDataRemoved(MailboxData mailboxData) {
                removedData.add(mailboxData);
            }
        });
        authenticatedDataStore = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName())
                .join();
        authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content", ownerKeyPair.getPublic()));
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files in
    // it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        dataService.getStorageService().getStoresByStoreType(StoreType.ALL)
                .forEach(DataServiceRelayTest::waitForPendingWrites);
        dataService.shutdown();
    }

    @Test
    void removalOfStoredDataIsRelayed() throws GeneralSecurityException {
        AddAuthenticatedDataRequest add = AddAuthenticatedDataRequest.from(authenticatedDataStore, authenticatedData,
                ownerKeyPair);
        dataService.processAddDataRequest(add, false);
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(authenticatedDataStore,
                authenticatedData, ownerKeyPair);

        dataService.processRemoveDataRequest(removal, true);
        dataService.processRemoveDataRequest(removal, true);

        assertEquals(List.of(removal), relayedRequests);
        assertEquals(List.of(authenticatedData), removedData);
    }

    // A relay removed signature V2 and changed the sequence number, which signature V1 does not cover. The removal with
    // signature V2 replaces the stored one and must reach the nodes behind this node.
    @Test
    void removalWhichReplacesStoredRemovalIsRelayed() throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest verified = RemoveAuthenticatedDataRequest.from(authenticatedDataStore,
                authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest legacy = RemoveAuthenticatedDataRequest.fromProto(
                verified.toValueProto(false).toBuilder()
                        .setVersion(1)
                        .clearSignatureV2()
                        .setSequenceNumber(Integer.MAX_VALUE)
                        .build());

        dataService.processRemoveDataRequest(legacy, true);
        dataService.processRemoveDataRequest(verified, true);
        dataService.processRemoveDataRequest(verified, true);
        dataService.processRemoveDataRequest(legacy, true);

        assertEquals(List.of(legacy, verified), relayedRequests);
        assertSame(verified, authenticatedDataStore.getPersistableStore().getMap().get(verified.getStorageKey()));
        assertTrue(removedData.isEmpty());
    }

    // A relay removed signature V2 and moved the creation date back, which signature V1 does not cover
    @Test
    void mailboxRemovalWhichReplacesStoredRemovalIsRelayed()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(ownerKeyPair));
        KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        RemoveMailboxRequest verified = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        RemoveMailboxRequest legacy = RemoveMailboxRequest.fromProto(verified.toValueProto(false).toBuilder()
                .setVersion(1)
                .clearSignatureV2()
                .setCreated(verified.getCreated() - 1000)
                .build());

        dataService.processRemoveDataRequest(legacy, true);
        dataService.processRemoveDataRequest(verified, true);
        dataService.processRemoveDataRequest(verified, true);
        dataService.processRemoveDataRequest(legacy, true);

        assertEquals(List.of(legacy, verified), relayedRequests);
        assertTrue(removedData.isEmpty());
    }

    @Test
    void removalOfInventoryResponseIsNotRelayed() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(authenticatedDataStore,
                authenticatedData, ownerKeyPair);

        dataService.processRemoveDataRequest(removal, false);

        assertTrue(relayedRequests.isEmpty());
        assertSame(removal, authenticatedDataStore.getPersistableStore().getMap().get(removal.getStorageKey()));
    }

    private static <T extends DataRequest> void waitForPendingWrites(DataStorageService<T> store) {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }
}
