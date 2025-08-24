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

import bisq.common.observable.ReadOnlyObservable;
import bisq.common.platform.PlatformUtils;
import bisq.common.threading.ExecutorFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.Destination;
import net.i2p.router.Router;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class I2pRouter {
    public static void main(String[] args) {
        String i2pDirPath = PlatformUtils.getUserDataDir().resolve("bisq2_i2p_embedded_router").toString();
        I2pRouter router = new I2pRouter(i2pDirPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("I2pRouter.shutdownHook");
            router.shutdown();
        }));

        router.start()
                .orTimeout(3, TimeUnit.MINUTES)
                .whenComplete((result, throwable) -> {
                    if (throwable != null || !result) {
                        router.shutdown();
                        log.error("I2P router failed to start, exiting.");
                        System.exit(1);
                    }
                });

        keepRunning();
    }

    private final String dirPath;
    private final RouterSetup routerSetup;
    private final RouterStateObserver routerStateObserver;

    private volatile Router router;
    private volatile boolean isShutdownInProgress;

    public I2pRouter(String i2pDirPath) {
        this(i2pDirPath, false, 512, 512, 50);
    }

    public I2pRouter(String i2pDirPath,
                     boolean isEmbedded,
                     int inboundKBytesPerSecond,
                     int outboundKBytesPerSecond,
                     int bandwidthSharePercentage) {
        this.dirPath = i2pDirPath;

        // Must be set before I2P router is created as otherwise log outputs are routed by I2P log system
        System.setProperty("I2P_DISABLE_OUTPUT_OVERRIDE", "true");

        // Having IPv6 enabled can cause problems with certain configurations.
        System.setProperty("java.net.preferIPv4Stack", "true");

        routerStateObserver = new RouterStateObserver();
        routerSetup = new RouterSetup(i2pDirPath,
                isEmbedded,
                inboundKBytesPerSecond,
                outboundKBytesPerSecond,
                bandwidthSharePercentage);
    }

    public CompletableFuture<Boolean> start() {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouter.start");
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        routerSetup.initialize();

                        log.info("Launching router");
                        long ts = System.currentTimeMillis();
                        router = new Router();
                        router.setKillVMOnEnd(false);
                        log.info("Router created");

                        CountDownLatch latch = new CountDownLatch(1);
                        routerStateObserver.getState().addObserver(state -> {
                            if (state == RouterStateObserver.State.RUNNING_OK) {
                                latch.countDown();
                            }
                        });
                        routerStateObserver.start(router);

                        router.runRouter();
                        latch.await(3, TimeUnit.MINUTES); //todo use config

                        log.info("Starting router and connecting to I2P network took {} ms", System.currentTimeMillis() - ts);
                        return true;
                    } catch (InterruptedException e) {
                        log.warn("Thread got interrupted at shutdown", e);
                        Thread.currentThread().interrupt(); // Restore interrupted state
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        log.error("Starting router failed", e);
                        throw new RuntimeException(e);
                    }
                }, executor)
                .orTimeout(3, TimeUnit.MINUTES)
                .whenComplete((result, throwable) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
    }

    public CompletableFuture<Boolean> shutdown() {
        if (router == null || isShutdownInProgress) {
            return CompletableFuture.completedFuture(true);
        }
        isShutdownInProgress = true;

        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouter.shutdown");
        return CompletableFuture.supplyAsync(() -> {
                    long ts = System.currentTimeMillis();
                    router.shutdown(1);
                    log.info("I2P router shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
                    return true;
                }, executor)
                .whenComplete((result, throwable) -> {
                    routerStateObserver.shutdown();
                    ExecutorFactory.shutdownAndAwaitTermination(executor);
                });
    }

    public ReadOnlyObservable<RouterStateObserver.State> getState() {
        return routerStateObserver.getState();
    }

    public ReadOnlyObservable<Integer> getOutboundTunnelCount() {
        return routerStateObserver.getOutboundTunnelCount();
    }

    // Tracks recent failed connection attempts
    public boolean wasUnreachable(Destination destination) {
        return router.getContext().commSystem().wasUnreachable(destination.getHash());
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
