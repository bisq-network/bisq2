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

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.broadcast.Broadcaster;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestAuthorizedDistributedData;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.CLOCK_BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The publisher migration of an oracle node can create thousands of requests. A connection closes if more than about
 * 100 messages wait for sending, so the node broadcasts the requests one at a time (see
 * docs/specifications/network/storage-ownership.md).
 */
class PublisherMigrationBroadcastTest {
    @TempDir
    Path tempDir;

    private final List<BroadcastMessage> broadcastMessages = new CopyOnWriteArrayList<>();
    private final KeyPair authorizedKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair identityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private DataService dataService;
    private AuthenticatedDataStorageService store;

    @BeforeEach
    void setUp() {
        setUp(tempDir);
    }

    private void setUp(Path dataDir) {
        StorableTypesRegistry.register(TestAuthorizedDistributedData.class, TestAuthorizedDistributedData.META_DATA);
        dataService = new DataService(new PersistenceService(dataDir), CLOCK_BEFORE_CUTOFF);
        dataService.addBroadcaster(new Broadcaster(null) {
            @Override
            public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
                broadcastMessages.add(broadcastMessage);
                return CompletableFuture.completedFuture(new BroadcastResult(1, 0, 0));
            }

            @Override
            public boolean hasConnections() {
                return true;
            }
        });
        store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(TestAuthorizedDistributedData.class.getSimpleName())
                .join();
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        dataService.shutdown();
    }

    @Test
    void requestsAreBroadcastOneAtATimeWithTheEntriesBeforeTheRemovals() throws Exception {
        assertTrue(store.add(createEntryOfEarlierVersion("first")).isSuccess());
        assertTrue(store.add(createEntryOfEarlierVersion("second")).isSuccess());

        dataService.migratePublisherOfAuthorizedData(identityKeyPair, authorizedKeyPair);

        // Nothing is sent at once
        assertTrue(broadcastMessages.isEmpty());
        waitUntilBroadcast(4);
        assertInstanceOf(AddAuthenticatedDataRequest.class, broadcastMessages.get(0));
        assertInstanceOf(AddAuthenticatedDataRequest.class, broadcastMessages.get(1));
        assertInstanceOf(RemoveAuthenticatedDataRequest.class, broadcastMessages.get(2));
        assertInstanceOf(RemoveAuthenticatedDataRequest.class, broadcastMessages.get(3));

        // The later runs at startup do not send the same requests again
        dataService.migratePublisherOfAuthorizedData(identityKeyPair, authorizedKeyPair);
        Thread.sleep(1500);
        assertEquals(4, broadcastMessages.size());
    }

    // The node restarts before it sent all requests. The first run after the restart finds nothing to migrate, but it
    // sends the requests of the earlier run again.
    @Test
    void requestsOfTheRunBeforeARestartAreBroadcastAgain() throws Exception {
        assertTrue(store.add(createEntryOfEarlierVersion("first")).isSuccess());
        dataService.migratePublisherOfAuthorizedData(identityKeyPair, authorizedKeyPair);
        waitUntilBroadcast(2);
        List<BroadcastMessage> requestsOfFirstStart = List.copyOf(broadcastMessages);
        broadcastMessages.clear();

        // The test type has no protobuf resolver, so the new store gets the entries of the old one in memory
        Map<ByteArray, AuthenticatedDataRequest> entriesBeforeRestart = Map.copyOf(store.getPersistableStore().getMap());
        tearDown();
        setUp(tempDir.resolve("after-restart"));
        store.getPersistableStore().getMap().putAll(entriesBeforeRestart);

        dataService.migratePublisherOfAuthorizedData(identityKeyPair, authorizedKeyPair);

        waitUntilBroadcast(2);
        assertEquals(requestsOfFirstStart, broadcastMessages);
    }

    private void waitUntilBroadcast(int numMessages) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (broadcastMessages.size() < numMessages && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(numMessages, broadcastMessages.size());
    }

    // An entry as oracle nodes of earlier versions published it: with the key pair of the network identity and without
    // signature V2
    private AddAuthenticatedDataRequest createEntryOfEarlierVersion(String content) throws GeneralSecurityException {
        TestAuthorizedDistributedData data = TestAuthorizedDistributedData.withLegacyOwners(content);
        AuthorizedData authorizedData = new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), authorizedKeyPair.getPrivate())),
                authorizedKeyPair.getPublic());
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authorizedData,
                1,
                DigestUtil.hash(identityKeyPair.getPublic().getEncoded()),
                Math.min(System.currentTimeMillis(), AuthorizedData.LEGACY_OWNER_CUTOFF_DATE.getTime() - 1));
        return new AddAuthenticatedDataRequest(sequentialData,
                SignatureUtil.sign(sequentialData.serializeForHash(), identityKeyPair.getPrivate()),
                identityKeyPair.getPublic().getEncoded(),
                identityKeyPair.getPublic(),
                new byte[]{},
                0);
    }
}
