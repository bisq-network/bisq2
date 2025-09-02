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

package bisq.network.i2p.grpc.client;

import bisq.network.i2p.grpc.messages.Topic;
import bisq.network.i2p.router.RouterSetup;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Bi2pGrpcClientServiceMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        Bi2pGrpcClientService service = new Bi2pGrpcClientService(RouterSetup.DEFAULT_BI2P_GRPC_HOST, 7777);
        service.initialize().join();
        service.subscribe(Topic.NETWORK_STATE); // Other methods not impl in server mock

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                service.shutdown().join();
            } catch (Throwable t) {
                log.warn("Error during service shutdown", t);
            }
        }, "GrpcRouterMonitorMain.shutdownHook"));

        keepRunning();
    }

    private static void keepRunning() {
        try {
            // Keep running
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at keepRunning method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
        }
    }
}
