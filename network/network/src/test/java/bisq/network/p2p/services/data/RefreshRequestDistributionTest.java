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

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The owner broadcasts a refresh request. A node which holds the data applies the request and relays it (see
 * docs/specifications/network/storage-refresh-requests.md). The tests pass the messages which the node of the owner
 * sends to the DataNetworkService of a peer.
 */
class RefreshRequestDistributionTest {
    @TempDir
    Path ownerDir;
    @TempDir
    Path peerDir;

    private DataService ownerDataService;
    private DataService peerDataService;
    private Node ownerNode;
    private Node peerNode;
    private DataNetworkService peerDataNetworkService;
    private KeyPair ownerKeyPair;
    private AuthenticatedData authenticatedData;
    private final List<AuthenticatedData> dataRefreshedAtPeer = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        ownerDataService = new DataService(new PersistenceService(ownerDir));
        peerDataService = new DataService(new PersistenceService(peerDir));
        ownerNode = mockNode();
        peerNode = mockNode();
        new DataNetworkService(ownerNode, ownerDataService);
        peerDataNetworkService = new DataNetworkService(peerNode, peerDataService);
        peerDataService.addListener(new DataService.Listener() {
            @Override
            public void onAuthenticatedDataRefreshed(AuthenticatedData authenticatedData) {
                dataRefreshedAtPeer.add(authenticatedData);
            }
        });
        ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content", ownerKeyPair.getPublic()));
    }

    // The stores write asynchronously, on the single thread of the persistence executor. JUnit deletes the directories
    // after the test, and fails if a write still creates files in them. A write queued now completes after all earlier
    // ones.
    @AfterEach
    void tearDown() {
        AuthenticatedDataStorageService store = ownerDataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName()).join();
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        ownerDataService.shutdown();
        peerDataService.shutdown();
    }

    @Test
    void receivedRefreshRequestIsAppliedAndRelayed() {
        publishDataToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = refreshDataAtOwner();
        int numMessagesSentByPeer = getNumSentMessages(peerNode);

        peerDataNetworkService.onMessage(asReceived(refreshRequest), mock(Connection.class), null);

        AddAuthenticatedDataRequest storedRequest = getStoredRequest(peerDataService);
        assertEquals(2, storedRequest.getSequenceNumber());
        assertEquals(refreshRequest.getCreated(), storedRequest.getCreated());
        assertFalse(storedRequest.isSignatureInvalid());
        assertEquals(List.of(authenticatedData), dataRefreshedAtPeer);
        assertEquals(numMessagesSentByPeer + 1, getNumSentMessages(peerNode));
        assertEquals(refreshRequest, getLastSentMessage(peerNode));
    }

    // A broadcast arrives from several peers. Only the first copy is applied and relayed, so the relaying ends.
    @Test
    void copyOfAppliedRefreshRequestIsNotRelayed() {
        publishDataToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = refreshDataAtOwner();
        peerDataNetworkService.onMessage(asReceived(refreshRequest), mock(Connection.class), null);
        int numMessagesSentByPeer = getNumSentMessages(peerNode);

        peerDataNetworkService.onMessage(asReceived(refreshRequest), mock(Connection.class), null);

        assertEquals(numMessagesSentByPeer, getNumSentMessages(peerNode));
        assertEquals(1, dataRefreshedAtPeer.size());
        assertEquals(2, getStoredRequest(peerDataService).getSequenceNumber());
    }

    @Test
    void refreshRequestWhichARelayChangedIsNotAppliedAndNotRelayed() {
        publishDataToOwnerAndPeer();
        RefreshAuthenticatedDataRequest refreshRequest = refreshDataAtOwner();
        RefreshAuthenticatedDataRequest withChangedSequenceNumber = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder().setSequenceNumber(Integer.MAX_VALUE).build());
        RefreshAuthenticatedDataRequest withChangedCreationDate = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder().setCreated(refreshRequest.getCreated() + 1).build());
        RefreshAuthenticatedDataRequest withoutSignatureV2 = RefreshAuthenticatedDataRequest.fromProto(
                refreshRequest.toValueProto(false).toBuilder().clearSignatureV2().build());
        int numMessagesSentByPeer = getNumSentMessages(peerNode);

        peerDataNetworkService.onMessage(withChangedSequenceNumber, mock(Connection.class), null);
        peerDataNetworkService.onMessage(withChangedCreationDate, mock(Connection.class), null);
        peerDataNetworkService.onMessage(withoutSignatureV2, mock(Connection.class), null);

        assertEquals(1, getStoredRequest(peerDataService).getSequenceNumber());
        assertTrue(dataRefreshedAtPeer.isEmpty());
        assertEquals(numMessagesSentByPeer, getNumSentMessages(peerNode));
    }

    // A refresh request contains no data. A node which does not hold the data cannot apply it, and creates no store
    // for the class name which the request names.
    @Test
    void refreshRequestForDataWhichTheNodeDoesNotHoldIsNotRelayed() {
        ownerDataService.addAuthenticatedData(authenticatedData, ownerKeyPair).join();
        RefreshAuthenticatedDataRequest refreshRequest = refreshDataAtOwner();

        peerDataNetworkService.onMessage(asReceived(refreshRequest), mock(Connection.class), null);

        verify(peerNode, atLeast(0)).isInitialized();
        assertEquals(0, getNumSentMessages(peerNode));
        assertTrue(dataRefreshedAtPeer.isEmpty());
        assertEquals(0, peerDataService.getStorageService().getStoresByStoreType(StoreType.ALL).count());
    }

    // Nodes of versions up to 2.1.13 sign only the hash of the data. A node cannot verify the sequence number and the
    // creation date of such a request.
    @Test
    void refreshRequestOfEarlierVersionIsNotAppliedAndNotRelayed() throws GeneralSecurityException {
        publishDataToOwnerAndPeer();
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
        int numMessagesSentByPeer = getNumSentMessages(peerNode);

        peerDataNetworkService.onMessage(ofEarlierVersion, mock(Connection.class), null);

        assertEquals(1, getStoredRequest(peerDataService).getSequenceNumber());
        assertTrue(dataRefreshedAtPeer.isEmpty());
        assertEquals(numMessagesSentByPeer, getNumSentMessages(peerNode));
    }

    // Refresh requests are not part of inventory responses. The stores hold the refreshed add request, which a node
    // without the data accepts.
    @Test
    void storeHoldsTheRefreshedAddRequestForInventoryResponses() {
        publishDataToOwnerAndPeer();
        peerDataNetworkService.onMessage(asReceived(refreshDataAtOwner()), mock(Connection.class), null);

        List<? extends DataRequest> requestsForInventory = peerDataService.getStorageService()
                .getAllDataRequestMapEntries()
                .map(entry -> entry.getValue())
                .toList();

        assertEquals(1, requestsForInventory.size());
        AddAuthenticatedDataRequest refreshedAddRequest = assertInstanceOf(AddAuthenticatedDataRequest.class,
                requestsForInventory.get(0));
        assertEquals(2, refreshedAddRequest.getSequenceNumber());
        DataService dataServiceOfOtherNode = new DataService(new PersistenceService(peerDir.resolve("otherNode")));
        try {
            assertTrue(dataServiceOfOtherNode.getStorageService().onAddDataRequest(refreshedAddRequest).join().isSuccess());
        } finally {
            dataServiceOfOtherNode.shutdown();
        }
    }

    private void publishDataToOwnerAndPeer() {
        ownerDataService.addAuthenticatedData(authenticatedData, ownerKeyPair).join();
        AddAuthenticatedDataRequest addRequest = assertInstanceOf(AddAuthenticatedDataRequest.class,
                getLastSentMessage(ownerNode));
        peerDataNetworkService.onMessage(addRequest, mock(Connection.class), null);
        assertEquals(1, getStoredRequest(peerDataService).getSequenceNumber());
    }

    // Returns the refresh request which the node of the owner broadcasts
    private RefreshAuthenticatedDataRequest refreshDataAtOwner() {
        ownerDataService.refreshAuthenticatedData(authenticatedData, ownerKeyPair).join();
        assertEquals(2, getStoredRequest(ownerDataService).getSequenceNumber());
        return assertInstanceOf(RefreshAuthenticatedDataRequest.class, getLastSentMessage(ownerNode));
    }

    // A refresh request contains no data, so the test can parse it as a receiving node does
    private static RefreshAuthenticatedDataRequest asReceived(RefreshAuthenticatedDataRequest refreshRequest) {
        return RefreshAuthenticatedDataRequest.fromProto(refreshRequest.toValueProto(false));
    }

    private static AddAuthenticatedDataRequest getStoredRequest(DataService dataService) {
        List<? extends DataRequest> requests = dataService.getStorageService().getAllDataRequestMapEntries()
                .map(entry -> entry.getValue())
                .toList();
        assertEquals(1, requests.size());
        return assertInstanceOf(AddAuthenticatedDataRequest.class, requests.get(0));
    }

    private static Node mockNode() {
        Node node = mock(Node.class);
        Connection connection = mock(Connection.class);
        when(node.isInitialized()).thenReturn(true);
        when(node.getAllActiveConnections()).thenAnswer(invocation -> Stream.of(connection));
        when(node.sendAsync(any(EnvelopePayloadMessage.class), any(Connection.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));
        return node;
    }

    private static List<EnvelopePayloadMessage> getSentMessages(Node node) {
        ArgumentCaptor<EnvelopePayloadMessage> captor = ArgumentCaptor.forClass(EnvelopePayloadMessage.class);
        verify(node, atLeast(0)).sendAsync(captor.capture(), any(Connection.class));
        return captor.getAllValues();
    }

    private static int getNumSentMessages(Node node) {
        return getSentMessages(node).size();
    }

    private static EnvelopePayloadMessage getLastSentMessage(Node node) {
        List<EnvelopePayloadMessage> sentMessages = getSentMessages(node);
        assertFalse(sentMessages.isEmpty());
        return sentMessages.get(sentMessages.size() - 1);
    }
}
