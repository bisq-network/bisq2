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
import bisq.network.i2p.grpc.client.Bi2pGrpcClientService;
import bisq.network.i2p.grpc.messages.PeerAvailabilityResponse;
import bisq.network.i2p.router.I2PRouter;
import bisq.network.i2p.router.RouterSetup;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterObserver;
import bisq.network.i2p.router.utils.I2PLogLevel;
import bisq.network.p2p.node.transport.I2PTransportService;
import lombok.extern.slf4j.Slf4j;
import net.i2p.data.Destination;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class I2PRouterFacade {
    private final I2PTransportService.Config config;
    private final String i2cpHost;
    private final int i2cpPort;
    private final String bi2pGrpcHost;
    private final int bi2pGrpcPort;
    private final Path i2pRouterDir;

    private volatile Bi2pGrpcClientService grpcRouterMonitorService;
    private volatile I2PRouter i2pRouter;
    private volatile Bi2pProcessLauncher i2pRouterProcessLauncher;

    private volatile Pin processStatePin;

    public I2PRouterFacade(I2PTransportService.Config config) {
        this.config = config;
        i2cpHost = this.config.getI2cpHost();
        i2cpPort = this.config.getI2cpPort();
        bi2pGrpcHost = this.config.getBi2pGrpcHost();
        bi2pGrpcPort = this.config.getBi2pGrpcPort();
        Path i2pDirPath = config.getDataDir();
        i2pRouterDir = i2pDirPath.resolve("router").toAbsolutePath();
    }

    public RouterMode detectRouterMode() {
        boolean defaultI2pDetected = isDefaultI2pDetected();
        boolean bisqI2PRouterDetected = isBi2pRouterDetected();

        if (config.isEmbeddedRouter() && !defaultI2pDetected && !bisqI2PRouterDetected) {
            return RouterMode.START_EMBEDDED;
        } else if (defaultI2pDetected) {
            return RouterMode.USE_DEFAULT_EXTERNAL;
        } else if (bisqI2PRouterDetected) {
            return RouterMode.USE_BI2P;
        } else {
            return RouterMode.LAUNCH_BI2P;
        }
    }

    public CompletableFuture<Void> initialize(RouterMode routerMode) {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2pRouterFacade");
        return CompletableFuture.runAsync(() -> {
            try {
                switch (routerMode) {
                    case START_EMBEDDED -> startEmbeddedRouter();
                    case USE_DEFAULT_EXTERNAL -> useDefaultRouter();
                    case USE_BI2P -> useBi2pRouter();
                    case LAUNCH_BI2P -> launchBi2pRouter();
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

    public Optional<Boolean> isPeerOnline(Destination peersDestination, String nodeId) {
        if (i2pRouter != null) {
            // TODO: wasUnreachable is a weak indicator. Maybe we should only use isEstablished?
            boolean wasUnreachable = i2pRouter.wasUnreachable(peersDestination);
            boolean isEstablished = i2pRouter.isEstablished(peersDestination);
            boolean isLikelyOnline = isEstablished || !wasUnreachable;
            return Optional.of(isLikelyOnline);
        }
        if (grpcRouterMonitorService != null) {
            PeerAvailabilityResponse peerAvailabilityResponse = grpcRouterMonitorService.requestPeerAvailability(peersDestination);
            boolean wasUnreachable = peerAvailabilityResponse.isWasUnreachable();
            boolean isEstablished = peerAvailabilityResponse.isEstablished();
            boolean isLikelyOnline = isEstablished || !wasUnreachable;
            return Optional.of(isLikelyOnline);
        } else {
            // With default I2P router we cannot request the state
            return Optional.empty();
        }
    }

    private void launchBi2pRouter() throws TimeoutException {
        try {
            log.info("No external router detected. We start our Bisq I2P router in a new process. I2CP address: {}:{}, Grpc router monitor: {}:{}",
                    i2cpHost, i2cpPort, bi2pGrpcHost, bi2pGrpcPort);
            i2pRouterProcessLauncher = new Bi2pProcessLauncher(i2cpHost, i2cpPort, bi2pGrpcHost, bi2pGrpcPort, i2pRouterDir);
            i2pRouterProcessLauncher.initialize().get();
            log.info("Bisq I2P router launched. Awaiting running state.");
            awaitGrpcMonitorServerAvailable();
            awaitRouterRunningUsingGrpcMonitor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("No Grpc monitor server detected (interrupted). Proceeding without monitor state.", e);
        } catch (TimeoutException | ExecutionException e) {
            log.warn("No Grpc monitor server detected. Proceeding without monitor state.", e);
        }
    }

    private void useBi2pRouter() {
        log.info("Bisq I2P router is used as external router.");
        try {
            if (!isGrpcMonitorServerDetected()) {
                // Let's give it a bit of time to detect grpc server
                awaitGrpcMonitorServerAvailable();
            }
            log.info("Grpc monitor server detected. Awaiting running state.");
            awaitRouterRunningUsingGrpcMonitor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("No Grpc monitor server detected (interrupted). Proceeding without monitor state.", e);
        } catch (TimeoutException | ExecutionException e) {
            log.warn("No Grpc monitor server detected. Proceeding without monitor state.", e);
        }
    }

    private static void useDefaultRouter() {
        // We assume the router is already in a running state
        log.info("Default I2P router is used as external router");
    }

    private void startEmbeddedRouter() throws TimeoutException, ExecutionException, InterruptedException, IOException, URISyntaxException {
        log.info("Embedded I2P router is used. No external router detected.");

        i2pRouter = new I2PRouter(i2pRouterDir,
                i2cpHost,
                i2cpPort,
                I2PLogLevel.INFO,
                true,
                config.getRouterStartupTimeout(),
                config.getInboundKBytesPerSecond(),
                config.getOutboundKBytesPerSecond(),
                config.getBandwidthSharePercentage());
        i2pRouter.startRouter().get();
        log.info("Embedded I2P router started.");

        awaitRouterRunning(i2pRouter.getRouterMonitor()).get();
    }

    private void awaitGrpcMonitorServerAvailable() throws ExecutionException, InterruptedException {
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("awaitGrpcMonitorServerAvailable");
        CompletableFuture.runAsync(() -> {
                    while (!isGrpcMonitorServerDetected() && !Thread.currentThread().isInterrupted()) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore interrupted state
                        }
                    }
                }, executor)
                .orTimeout(config.getRouterStartupTimeout(), TimeUnit.MILLISECONDS)
                .whenComplete((r, t) -> ExecutorFactory.shutdownAndAwaitTermination(executor))
                .get();
    }

    private void awaitRouterRunningUsingGrpcMonitor() throws TimeoutException, ExecutionException, InterruptedException {
        // We use our grpc monitor to listen on the running state.
        grpcRouterMonitorService = new Bi2pGrpcClientService(bi2pGrpcHost, bi2pGrpcPort);

        grpcRouterMonitorService.initialize()
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        grpcRouterMonitorService.requestRouterState();
                        grpcRouterMonitorService.subscribeAll();
                    }
                }).get();

        awaitRouterRunning(grpcRouterMonitorService).get();
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
        return NetworkUtils.isPortInUse("127.0.0.1", RouterSetup.DEFAULT_I2CP_PORT);
    }

    private boolean isBi2pRouterDetected() {
        return NetworkUtils.isPortInUse(i2cpHost, i2cpPort);
    }

    private boolean isGrpcMonitorServerDetected() {
        return NetworkUtils.isPortInUse(bi2pGrpcHost, bi2pGrpcPort);
    }
}
