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

package bisq.network.i2p.router;

import bisq.common.platform.PlatformUtils;
import bisq.common.threading.ExecutorFactory;
import bisq.network.i2p.grpc.server.Bi2pGrpcServer;
import bisq.network.i2p.grpc.server.Bi2pGrpcService;
import bisq.network.i2p.router.utils.I2PLogLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class I2PRouterMain {
    private static volatile I2PRouter i2pRouter;
    private static volatile Bi2pGrpcService i2pBridgeService;
    private static volatile Bi2pGrpcServer i2pGrpcServer;
    private static volatile ExecutorService executor;

    public static void main(String[] args) {
        executor = ExecutorFactory.newSingleThreadExecutor("I2pRouter.start");
        CompletableFuture.runAsync(() -> {
            Path i2pDirPath = PlatformUtils.getUserDataDir().resolve("Bisq2_I2P_router");
            i2pRouter = new I2PRouter(i2pDirPath,
                    RouterSetup.DEFAULT_I2CP_HOST,
                    RouterSetup.DEFAULT_I2CP_PORT,
                    RouterSetup.DEFAULT_HTTP_PROXY_HOST,
                    RouterSetup.DEFAULT_HTTP_PROXY_PORT,
                    true,
                    I2PLogLevel.DEBUG,
                    false,
                    (int) TimeUnit.SECONDS.toMillis(300),
                    512,
                    512,
                    50);

            i2pBridgeService = new Bi2pGrpcService(i2pRouter);
            i2pGrpcServer = new Bi2pGrpcServer(RouterSetup.DEFAULT_BI2P_GRPC_HOST, RouterSetup.DEFAULT_BI2P_GRPC_PORT, i2pBridgeService);
            i2pGrpcServer.initialize();
            i2pBridgeService.initialize();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Thread.currentThread().setName("I2pRouter.shutdownHook");
                shutdown();
            }));

            i2pRouter.startRouter()
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("I2P router failed to start, exiting.", throwable);
                            shutdown();
                            System.exit(PlatformUtils.EXIT_FAILURE);
                        }
                    });
        }, executor);

        keepRunning();
    }

    private static void shutdown() {
        if (i2pBridgeService != null) i2pBridgeService.shutdown().join();
        if (i2pGrpcServer != null) i2pGrpcServer.shutdown().join();
        if (i2pRouter != null) i2pRouter.shutdown().join();
        ExecutorFactory.shutdownAndAwaitTermination(executor);
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
