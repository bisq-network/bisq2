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

import bisq.common.threading.ExecutorFactory;
import bisq.network.i2p.router.log.I2pLogLevel;
import bisq.network.i2p.router.log.LogRedirector;
import bisq.network.i2p.router.state.RouterMonitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.Destination;
import net.i2p.router.Router;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class I2pRouter {
    private final String i2cpHost;
    private final int i2cpPort;
    private final long routerStartupTimeout;
    private final RouterSetup routerSetup;
    @Getter
    private final RouterMonitor routerMonitor;
    private final Router router;
    private volatile boolean isShutdownInProgress;

    public I2pRouter(Path i2pDirPath,
                     String i2cpHost,
                     int i2cpPort,
                     I2pLogLevel i2pLogLevel,
                     boolean isInProcess,
                     long routerStartupTimeout,
                     int inboundKBytesPerSecond,
                     int outboundKBytesPerSecond,
                     int bandwidthSharePercentage) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.routerStartupTimeout = routerStartupTimeout;

        LogRedirector.redirectSystemStreams();
        log.info("I2CP listening on: {}:{}", i2cpHost, i2cpPort);

        routerSetup = new RouterSetup(i2pDirPath,
                i2cpHost,
                i2cpPort,
                i2pLogLevel,
                isInProcess,
                inboundKBytesPerSecond,
                outboundKBytesPerSecond,
                bandwidthSharePercentage);
        routerSetup.initialize();

        router = new Router();
        log.info("Router created");
        router.setKillVMOnEnd(false);

        routerSetup.setI2pLogLevel(router);
        routerMonitor = new RouterMonitor(router);
    }

    public CompletableFuture<Boolean> startRouter() {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouter.runRouter");
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        long ts = System.currentTimeMillis();
                        routerMonitor.startPolling();
                        router.runRouter();
                        log.info("Starting router and connecting to I2P network took {} ms", System.currentTimeMillis() - ts);
                        return true;
                    } catch (Exception e) {
                        log.error("Starting router failed", e);
                        throw new RuntimeException(e);
                    }
                }, executor)
                .orTimeout(routerStartupTimeout, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
    }

    public CompletableFuture<Boolean> shutdown() {
        if (isShutdownInProgress) {
            return CompletableFuture.completedFuture(true);
        }
        routerMonitor.shutdown();
        isShutdownInProgress = true;

        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouter.shutdown");
        return CompletableFuture.supplyAsync(() -> {
                    long ts = System.currentTimeMillis();
                    routerMonitor.startShutdown();
                    router.shutdown(1);
                    log.info("I2P router shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
                    return true;
                }, executor)
                .orTimeout(5, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> {
                    routerMonitor.shutdown();
                    ExecutorFactory.shutdownAndAwaitTermination(executor);
                });
    }

    // Tracks recent failed connection attempts
    public boolean wasUnreachable(Destination destination) {
        return router.getContext().commSystem().wasUnreachable(destination.getHash());
    }

    // Checks whether a communication session has been successfully established with that specific destination.
    public boolean isEstablished(Destination destination) {
        return router.getContext().commSystem().isEstablished(destination.getHash());
    }
}
