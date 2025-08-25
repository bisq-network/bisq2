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
        String i2pDirPath = PlatformUtils.getUserDataDir().resolve("bisq2_i2p_router").toString();
        I2pRouter router = new I2pRouter(i2pDirPath, 7654);

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

    @Getter
    private final String dirPath;
    @Getter
    private final String i2cpHost;
    @Getter
    private final int i2cpPort;
    private final long routerStartupTimeout;
    private final RouterSetup routerSetup;
    private final RouterStateObserver routerStateObserver;

    private volatile Router router;
    private volatile boolean isShutdownInProgress;

    public I2pRouter(String i2pDirPath, int i2cpPort) {
        this(i2pDirPath,
                "127.0.0.1",
                i2cpPort,
                I2pLogLevel.DEBUG,
                false,
                (int) TimeUnit.SECONDS.toMillis(300),
                512,
                512,
                50);
    }

    public I2pRouter(String i2pDirPath,
                     String i2cpHost,
                     int i2cpPort,
                     I2pLogLevel i2pLogLevel,
                     boolean isEmbedded,
                     long routerStartupTimeout,
                     int inboundKBytesPerSecond,
                     int outboundKBytesPerSecond,
                     int bandwidthSharePercentage) {
        this.dirPath = i2pDirPath;
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.routerStartupTimeout = routerStartupTimeout;

        log.info("I2CP listening on: {}:{}", i2cpHost, i2cpPort);

        routerStateObserver = new RouterStateObserver();
        routerSetup = new RouterSetup(i2pDirPath,
                i2cpHost,
                i2cpPort,
                i2pLogLevel,
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
                        router.getContext().logManager().setDefaultLimit(routerSetup.getI2pLogLevel().name());

                        router.setKillVMOnEnd(false);
                        log.info("Router created");

                        CountDownLatch latch = new CountDownLatch(1);
                        routerStateObserver.getProcessState().addObserver(state -> {
                            if (state == RouterStateObserver.ProcessState.RUNNING) {
                                latch.countDown();
                            }
                        });
                        routerStateObserver.start(router);

                        router.runRouter();
                        latch.await(routerStartupTimeout, TimeUnit.MILLISECONDS);

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
                    routerStateObserver.startShutdown();
                    router.shutdown(1);
                    log.info("I2P router shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
                    return true;
                }, executor)
                .whenComplete((result, throwable) -> {
                    routerStateObserver.shutdown();
                    ExecutorFactory.shutdownAndAwaitTermination(executor);
                });
    }

    public ReadOnlyObservable<RouterStateObserver.ProcessState> getProcessState() {
        return routerStateObserver.getProcessState();
    }

    public ReadOnlyObservable<RouterStateObserver.NetworkState> getNetworkState() {
        return routerStateObserver.getNetworkState();
    }

    public ReadOnlyObservable<RouterStateObserver.RouterState> getRouterState() {
        return routerStateObserver.getRouterState();
    }

    public ReadOnlyObservable<RouterStateObserver.TunnelInfo> getTunnelInfo() {
        return routerStateObserver.getTunnelInfo();
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
