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

package network.misq.network.p2p.services.data;

import lombok.extern.slf4j.Slf4j;
import network.misq.network.NetworkService;

@Slf4j
public class DataNodeBase {
    protected NetworkService p2pServiceSeed, p2pService1, p2pService2;

   /* protected void bootstrap(Set<NetworkServiceConfig> networkServiceConfigsSeed,
                             Set<NetworkServiceConfig> networkServiceConfigsNode1,
                             Set<NetworkServiceConfig> networkServiceConfigsNode2) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        getP2pServiceFuture(networkServiceConfigsSeed).whenComplete((p2pService, e) -> {
            assertNotNull(p2pService);
            this.p2pServiceSeed = p2pService;
            latch.countDown();
        });
        getP2pServiceFuture(networkServiceConfigsNode1).whenComplete((p2pService, e) -> {
            assertNotNull(p2pService);
            this.p2pService1 = p2pService;
            latch.countDown();
        });
        getP2pServiceFuture(networkServiceConfigsNode2).whenComplete((p2pService, e) -> {
            assertNotNull(p2pService);
            this.p2pService2 = p2pService;
            latch.countDown();
        });
        boolean bootstrapped = latch.await(1, TimeUnit.MINUTES);
        assertTrue(bootstrapped);
    }


    protected CompletableFuture<P2pServiceNodesByType> getP2pServiceFuture(Set<NetworkServiceConfig> networkServiceConfigs) {
        P2pServiceNodesByType p2pService = new P2pServiceNodesByType("", networkServiceConfigs, null);
        return p2pService.initializeOverlay().thenApply(result -> p2pService);
    }*/
}
