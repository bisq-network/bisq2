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
import bisq.network.i2p.grpc.server.GrpcRouterMonitorServer;
import bisq.network.i2p.grpc.server.GrpcRouterMonitorService;
import bisq.network.i2p.router.log.I2pLogLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class I2pRouterMain {
    public static void main(String[] args) {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouter.start");
        CompletableFuture.runAsync(() -> {
            Path i2pDirPath = PlatformUtils.getUserDataDir().resolve("Bisq2_I2P_router");
            I2pRouter i2pRouter = new I2pRouter(i2pDirPath,
                    "127.0.0.1",
                    8888,
                    I2pLogLevel.DEBUG,
                    false,
                    (int) TimeUnit.SECONDS.toMillis(300),
                    512,
                    512,
                    50);

            GrpcRouterMonitorService i2pBridgeService = new GrpcRouterMonitorService(i2pRouter);
            GrpcRouterMonitorServer i2pGrpcServer = new GrpcRouterMonitorServer(7777, i2pBridgeService);
            i2pGrpcServer.initialize();
            i2pBridgeService.initialize();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Thread.currentThread().setName("I2pRouter.shutdownHook");
                i2pRouter.shutdown();
            }));

            i2pRouter.startRouter()
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            i2pRouter.shutdown();
                            log.error("I2P router failed to start, exiting.");
                            System.exit(1);
                        }
                    });
        }, executor);

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
