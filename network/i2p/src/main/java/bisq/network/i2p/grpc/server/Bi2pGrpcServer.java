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

import bisq.bi2p.protobuf.Bi2pGrpc;
import bisq.common.application.Service;
import bisq.common.threading.ExecutorFactory;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class Bi2pGrpcServer implements Service {
    private final Server server;

    public Bi2pGrpcServer(String host, int port, Bi2pGrpc.Bi2pImplBase service) {
        server = NettyServerBuilder
                .forAddress(new InetSocketAddress(host, port))
                .addService(service)
                .build();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pGrpcRouterMonitor.Server");
        CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Starting Grpc server");
                        server.start();
                        future.complete(true);
                        server.awaitTermination(); // Blocks until server is shut down
                        log.info("Grpc server terminated");
                    } catch (IOException e) {
                        log.warn("IOException at starting server", e);
                        future.completeExceptionally(e);
                    } catch (InterruptedException e) {
                        // Expected at shutdown
                        Thread.currentThread().interrupt(); // Restore interrupted state
                        future.completeExceptionally(e);
                    }
                }, executor)
                .whenComplete((r, t) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
        return future;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pGrpcRouterMonitorServer");
        return CompletableFuture.supplyAsync(() -> {
                    server.shutdown();
                    try {
                        if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                            server.shutdownNow();
                        }
                        return true;
                    } catch (InterruptedException e) {
                        log.warn("Interrupted while shutting down GrpcRouterMonitorServer", e);
                        Thread.currentThread().interrupt();
                        server.shutdownNow();
                    }
                    return false;
                }, executor)
                .whenComplete((r, t) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
    }
}
