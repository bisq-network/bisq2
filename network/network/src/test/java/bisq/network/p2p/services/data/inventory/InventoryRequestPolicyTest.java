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

package bisq.network.p2p.services.data.inventory;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.network.p2p.common.RequestResponseHandler;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.network.p2p.services.peer_group.PeerGroupService;
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
import java.util.Set;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.inventory.InventoryRequestPolicy.NextTaskAfterRequestCompleted.RETRY_REQUEST_WITH_NEW_CONNECTION;
import static bisq.network.p2p.services.data.inventory.InventoryRequestPolicy.NextTaskAfterRequestCompleted.RETRY_REQUEST_WITH_SAME_CONNECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A response which reached the size limit makes the requester ask the same peer again, because the peer has more data.
 * The next response differs only if the filter of the requester lists something of the last one (see
 * docs/specifications/network/inventory-hash-set-filter.md).
 */
class InventoryRequestPolicyTest {
    @TempDir
    Path tempDir;

    private final Connection connection = mock(Connection.class);
    private DataService dataService;
    private InventoryRequestPolicy policy;
    // A response of one entry which reached the size limit
    private Inventory fullResponse;
    private AddAuthenticatedDataRequest entryOfFullResponse;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
    }

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        dataService = new DataService(new PersistenceService(tempDir));
        InventoryService.Config config = new InventoryService.Config(2000, 600_000, 2, 4, 5, 2, 2,
                List.of(InventoryFilterType.HASH_SET_V2));
        InventoryFilterFactory inventoryFilterFactory = new InventoryFilterFactory(Set.of(Feature.INVENTORY_HASH_SET_V2),
                dataService, config);
        @SuppressWarnings("unchecked")
        RequestResponseHandler<InventoryRequest, InventoryResponse> requestResponseHandler =
                mock(RequestResponseHandler.class);
        Node node = mock(Node.class);
        PeerGroupService peerGroupService = mock(PeerGroupService.class);
        when(peerGroupService.getShuffledNonSeedConnections(node)).thenAnswer(invocation -> Stream.of(connection));
        when(peerGroupService.getShuffledSeedConnections(node)).thenAnswer(invocation -> Stream.empty());
        policy = new InventoryRequestPolicy(config, new InventoryRequestModel(requestResponseHandler),
                inventoryFilterFactory, node, peerGroupService);

        Address address = Address.from("127.0.0.1", 8000);
        when(connection.getId()).thenReturn("connection");
        when(connection.getPeerAddress()).thenReturn(address);
        // Capability sorts the lists, so they must be mutable
        Capability capability = new Capability(Capability.VERSION, address,
                new ArrayList<>(List.of(TransportType.CLEAR)),
                new ArrayList<>(List.of(Feature.INVENTORY_HASH_SET_V2)),
                "2.1.14");
        when(connection.getPeersCapability()).thenReturn(capability);

        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthenticatedDataStorageService store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName())
                .join();
        entryOfFullResponse = AddAuthenticatedDataRequest.from(store,
                new DefaultAuthenticatedData(new TestDistributedData("content", keyPair.getPublic())),
                keyPair);
        fullResponse = new Inventory(List.of(entryOfFullResponse), true);
    }

    @AfterEach
    void tearDown() {
        dataService.getStorageService().getStoresByStoreType(StoreType.ALL)
                .forEach(InventoryRequestPolicyTest::waitForPendingWrites);
        dataService.shutdown();
    }

    // We hold none of the entries, so our next filter is the same, and so is the next response of the peer
    @Test
    void peerIsNotAskedAgainIfOurFilterListsNothingOfItsFullResponse() {
        assertEquals(List.of(connection), policy.getCandidatesForPeriodicRequests());

        policy.onInventoryProcessed(connection, fullResponse, 0);

        assertEquals(RETRY_REQUEST_WITH_NEW_CONNECTION, policy.onRequestCompleted(connection, fullResponse, null));
        assertTrue(policy.getCandidatesForPeriodicRequests().isEmpty());
    }

    @Test
    void peerIsAskedAgainIfWeStoredPartOfItsFullResponse() {
        policy.onInventoryProcessed(connection, fullResponse, 1);

        assertEquals(RETRY_REQUEST_WITH_SAME_CONNECTION, policy.onRequestCompleted(connection, fullResponse, null));
        assertEquals(List.of(connection), policy.getCandidatesForPeriodicRequests());
    }

    // Several initial requests use the same filter, so peers with the same data send the same response. We store the
    // entries of the first response and nothing of the later ones. Our next filter lists these entries, so the next
    // response of each of these peers differs.
    @Test
    void peerIsAskedAgainIfAnotherResponseDeliveredTheEntriesOfItsFullResponse() {
        assertTrue(dataService.processAddDataRequest(entryOfFullResponse, false).join().isSuccess());

        policy.onInventoryProcessed(connection, fullResponse, 0);

        assertEquals(RETRY_REQUEST_WITH_SAME_CONNECTION, policy.onRequestCompleted(connection, fullResponse, null));
        assertEquals(List.of(connection), policy.getCandidatesForPeriodicRequests());
    }

    private static <T extends DataRequest> void waitForPendingWrites(DataStorageService<T> store) {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }
}
