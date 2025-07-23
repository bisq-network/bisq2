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

package bisq.oracle_node.bisq1_bridge.grpc.demo;

import bisq.oracle_node.bisq1_bridge.grpc.Bisq1BridgeGrpcClientService;
import io.grpc.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bisq1BridgeGrpcClientDemoApp {
    private Server server;

    public static void main(String[] args) throws InterruptedException {
        Bisq1BridgeGrpcClientService.Config config = new Bisq1BridgeGrpcClientService.Config(50051);
        Bisq1BridgeGrpcClientService bisq1BridgeGrpcClient = new Bisq1BridgeGrpcClientService(config);
        bisq1BridgeGrpcClient.initialize().join();
        bisq1BridgeGrpcClient.subscribeBlockUpdate();

        Thread.currentThread().join();
    }
}
