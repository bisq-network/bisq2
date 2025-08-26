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

package bisq.network.i2p.grpc.server;

import bisq.common.application.Service;
import bisq.common.threading.ExecutorFactory;
import bisq.i2p.protobuf.I2pRouterMonitorGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public final class GrpcRouterMonitorServer implements Service {
    private final Server server;
    private final ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pGrpcRouterMonitorServer");

    public GrpcRouterMonitorServer(int port, I2pRouterMonitorGrpc.I2pRouterMonitorImplBase i2pBridgeService) {
        server = ServerBuilder
                .forPort(port)
                .addService(i2pBridgeService)
                .build();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture.runAsync(() -> {
            try {
                server.start();
                server.awaitTermination();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                log.warn("Thread got interrupted at initialize", e);
                Thread.currentThread().interrupt(); // Restore interrupted state
                throw new RuntimeException(e);
            }
        }, executor);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        server.shutdown();
        ExecutorFactory.shutdownAndAwaitTermination(executor);
        return CompletableFuture.completedFuture(true);
    }

}
