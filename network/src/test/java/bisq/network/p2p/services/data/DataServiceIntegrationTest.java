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

import bisq.common.util.OsUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.storage.auth.MockAuthenticatedPayload;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.security.KeyGeneration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class DataServiceIntegrationTest extends DataServiceNodeBase {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "bisq_DataServiceIntegrationTest";
    int numSeeds = 2;
    int numNodes = 4;

    // @Test
    public void testBroadcast() throws InterruptedException, ExecutionException, GeneralSecurityException {
        List<DataService> dataServicePerTransports = getBootstrappedDataServices();
        DataService dataServicePerTransport = dataServicePerTransports.get(0);
        int minExpectedConnections = numSeeds + numNodes - 2;
        KeyPair keyPair = KeyGeneration.generateKeyPair();
        MockAuthenticatedPayload payload = new MockAuthenticatedPayload("Test offer " + UUID.randomUUID());
        //todo
        // BroadcastResult result = dataServicePerTransport.addNetworkPayload(payload, keyPair).get();
        // log.error("result={}", result.toString());
        // assertTrue(result.numSuccess() >= minExpectedConnections);
    }


    // TODO with getBootstrappedDataServices(); we don't get a deterministic set up nodes connected to each other.
    // Mostly the small test network is well enough connected that the test succeeds, but not always.
    // We would likely need a more deterministic peerGroup management for tests (e.g. all nodes are connected to each other).
   // @Test
    public void testAddAuthenticatedDataRequest() throws GeneralSecurityException, InterruptedException, ExecutionException {
        MockAuthenticatedPayload payload = new MockAuthenticatedPayload("Test offer " + UUID.randomUUID());
        KeyPair keyPair = KeyGeneration.generateKeyPair();
        List<DataService> dataServices = getBootstrappedDataServices();
        DataService dataService_0 = dataServices.get(0);
        DataService dataService_1 = dataServices.get(1);
        DataService dataService_2 = dataServices.get(2);

        CountDownLatch latch = new CountDownLatch(1);
        dataService_1.addListener(new DataService.Listener() {
            @Override
            public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                log.info("onNetworkDataAdded at dataService_1");
                latch.countDown();
            }

            @Override
            public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
            }
        });
        dataService_2.addListener(new DataService.Listener() {
            @Override
            public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
                log.info("onNetworkDataAdded at dataService_2");
                latch.countDown();
            }

            @Override
            public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
            }
        });
        List<CompletableFuture<BroadcastResult>> broadcastResultFutures = dataService_0.addNetworkPayload(payload, keyPair).get();
        broadcastResultFutures.forEach(CompletableFuture::join);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    //todo
    private List<DataService> getBootstrappedDataServices() {
        Transport.Type type = Transport.Type.CLEAR;
        bootstrapMultiNodesSetup(Set.of(type), numSeeds, numNodes);
        List<Address> nodes = multiNodesSetup.getNodeAddresses(type, numNodes);
        List<NetworkService> networkServices = multiNodesSetup.getNetworkServicesByAddress().entrySet().stream()
                .filter(e -> nodes.contains(e.getKey()))
                .map(Map.Entry::getValue)
                /* .flatMap(networkService -> networkService.getDataService().stream())*/
                .collect(Collectors.toList());

        PeerGroup peerGroup = networkServices.get(0).getServiceNodesByTransport().findServiceNode(type).orElseThrow()
                .getPeerGroupService().orElseThrow().getPeerGroup();

        int expectedConnections = numSeeds + numNodes - 2;
        while (true) {
            long numCon = peerGroup.getAllConnections().count();
            if (numCon >= expectedConnections) {
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
            }
        }

        return networkServices.stream()
                .flatMap(networkService -> networkService.getDataService().stream())
                .collect(Collectors.toList());
    }
}
