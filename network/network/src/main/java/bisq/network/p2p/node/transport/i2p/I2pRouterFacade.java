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

package bisq.network.p2p.node.transport.i2p;

import bisq.common.observable.Pin;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.i2p.grpc.client.GrpcRouterMonitorService;
import bisq.network.i2p.router.I2pRouter;
import bisq.network.i2p.router.log.I2pLogLevel;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterObserver;
import bisq.network.p2p.node.transport.I2PTransportService;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.Destination;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class I2pRouterFacade {
    private static final int DEFAUL_I2CP_PORT = 7654;

    private final I2PTransportService.Config config;
    private final Path i2pDirPath;
    private final String i2cpHost;
    private final int i2cpPort;

    private volatile GrpcRouterMonitorService grpcRouterMonitorService;
    private volatile I2pRouter i2pRouter;
    private volatile I2pRouterProcessLauncher i2pRouterProcessLauncher;

    private volatile Pin processStatePin;

    public I2pRouterFacade(I2PTransportService.Config config) {
        this.config = config;
        i2pDirPath = config.getDataDir();
        i2cpHost = this.config.getI2cpHost();
        i2cpPort = this.config.getI2cpPort();
    }

    public RouterMode detectRouterMode() {
        boolean defaultI2pDetected = isDefaultI2pDetected();
        boolean bisqI2PRouterDetected = isBisqI2PRouterDetected();

        if (config.isEmbeddedRouter() && !defaultI2pDetected && !bisqI2PRouterDetected) {
            return RouterMode.START_EMBEDDED;
        } else if (defaultI2pDetected) {
            return RouterMode.USE_DEFAULT_EXTERNAL;
        } else if (bisqI2PRouterDetected) {
            return RouterMode.USE_BISQ_EXTERNAL;
        } else {
            return RouterMode.LAUNCH_BISQ_EXTERNAL;
        }
    }

    public CompletableFuture<Void> initialize(RouterMode routerMode) {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouterFacade");
        return CompletableFuture.runAsync(() -> {
            try {
                switch (routerMode) {
                    case START_EMBEDDED -> startEmbeddedRouter();
                    case USE_DEFAULT_EXTERNAL -> useDefaultRouter();
                    case USE_BISQ_EXTERNAL -> useBisqExternalRouter();
                    case LAUNCH_BISQ_EXTERNAL -> launchBisqRouterProcess();
                    default -> throw new IllegalStateException("Unknown router type");
                }
            } catch (Exception e) {
                log.error("Initialization failed", e);
                shutdown();
                throw new RuntimeException(e);
            }
        }, executor).whenComplete((r, t) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
    }

    public CompletableFuture<Boolean> shutdown() {
        if (processStatePin != null) {
            processStatePin.unbind();
            processStatePin = null;
        }
        Set<CompletableFuture<Boolean>> futures = new HashSet<>();
        if (i2pRouter != null) {
            futures.add(i2pRouter.shutdown().orTimeout(3, TimeUnit.SECONDS));
        }
        if (grpcRouterMonitorService != null) {
            futures.add(grpcRouterMonitorService.shutdown().orTimeout(3, TimeUnit.SECONDS));
        }
        if (i2pRouterProcessLauncher != null) {
            futures.add(i2pRouterProcessLauncher.shutdown().orTimeout(3, TimeUnit.SECONDS));
        }
        return CompletableFutureUtils.failureTolerantAllOf(futures)
                .thenApply(list -> list.size() == futures.size());
    }

    public Optional<Boolean> isPeerOnlineAsync(Destination peersDestination, String nodeId) {
        if (i2pRouter != null) {
            return Optional.of(i2pRouter.wasUnreachable(peersDestination));
        }
        if (grpcRouterMonitorService != null) {
            return Optional.of(grpcRouterMonitorService.isPeerOnlineAsync(peersDestination, nodeId));
        } else {
            // With default I2P router we cannot request the state
            return Optional.empty();
        }
    }

    private void launchBisqRouterProcess() throws TimeoutException {
        log.info("No external router detected. We start our Bisq I2P router in a new process.");
        i2pRouterProcessLauncher = new I2pRouterProcessLauncher(i2cpHost, i2cpPort, i2pDirPath);
        i2pRouterProcessLauncher.initialize().join();
        log.info("Bisq I2P router launched. Awaiting running state.");
        awaitGrpcMonitorServerAvailable();
        awaitRouterRunningUsingGrpcMonitor();
    }

    private void useBisqExternalRouter() {
        log.info("Bisq I2P router is used as external router.");
        try {
            if (!isGrpcMonitorServerDetected()) {
                // Let's give it a bit of time to detect grpc server
                awaitGrpcMonitorServerAvailable();
            }
            log.info("Grpc monitor server detected. Awaiting running state.");
            awaitRouterRunningUsingGrpcMonitor();
        } catch (TimeoutException e) {
            log.warn("No Grpc monitor server detected. Trying to use that router but not knowing it's state. " +
                    "This should not happen in normal circumstances.");
        }
    }

    private static void useDefaultRouter() {
        // We assume the router is already in a running state
        log.info("Default I2P router is used as external router");
    }

    private void startEmbeddedRouter() throws TimeoutException {
        log.info("Embedded I2P router is used. No external router detected.");
        i2pRouter = new I2pRouter(i2pDirPath,
                i2cpHost,
                i2cpPort,
                I2pLogLevel.INFO,
                true,
                config.getRouterStartupTimeout(),
                config.getInboundKBytesPerSecond(),
                config.getOutboundKBytesPerSecond(),
                config.getBandwidthSharePercentage());
        i2pRouter.startRouter().join();
        log.info("Embedded I2P router started.");

        awaitRouterRunning(i2pRouter.getRouterMonitor()).join();
    }

    private CompletableFuture<Void> awaitGrpcMonitorServerAvailable() {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("awaitGrpcMonitorServerAvailable");
        return CompletableFuture.runAsync(() -> {
                    while (!isGrpcMonitorServerDetected()) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore interrupted state
                        }
                    }
                }, executor)
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((r, t) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
    }

    private void awaitRouterRunningUsingGrpcMonitor() throws TimeoutException {
        // We use our grpc monitor to listen on the running state.
        grpcRouterMonitorService = new GrpcRouterMonitorService(config.getGrpcMonitorHost(), config.getGrpcMonitorPort());
        grpcRouterMonitorService.initialize();
        grpcRouterMonitorService.subscribeAll();
        awaitRouterRunning(grpcRouterMonitorService).join();
    }

    private CompletableFuture<Boolean> awaitRouterRunning(RouterObserver routerObserver) throws TimeoutException {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        processStatePin = routerObserver.getProcessState().addObserver(processState -> {
            log.error("processState {}", processState);
            if (processState == ProcessState.RUNNING) {
                log.error("Router in RUNNING state");
                future.complete(true);
            } else if (processState == ProcessState.STOPPING
                    || processState == ProcessState.STOPPED
                    || processState == ProcessState.FAILED) {
                future.completeExceptionally(new RuntimeException("Router startup failed. processState=" + processState));
            }
        });

        return future.orTimeout(config.getRouterStartupTimeout(), TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    processStatePin.unbind();
                    processStatePin = null;
                });
    }

    private static boolean isDefaultI2pDetected() {
        return NetworkUtils.isPortInUse("127.0.0.1", DEFAUL_I2CP_PORT);
    }

    private boolean isBisqI2PRouterDetected() {
        return NetworkUtils.isPortInUse(i2cpHost, i2cpPort);
    }

    private boolean isGrpcMonitorServerDetected() {
        return NetworkUtils.isPortInUse(config.getGrpcMonitorHost(), config.getGrpcMonitorPort());
    }
}
