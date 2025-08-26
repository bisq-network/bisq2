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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class GrpcRouterMonitorMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        GrpcRouterMonitorService service = new GrpcRouterMonitorService("127.0.0.1", 7777);
        service.initialize();
        service.subscribeAll();
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
