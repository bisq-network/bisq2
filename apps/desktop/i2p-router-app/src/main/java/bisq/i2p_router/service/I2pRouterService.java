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

package bisq.i2p_router.service;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.platform.PlatformUtils;
import bisq.network.i2p.grpc.server.GrpcRouterMonitorServer;
import bisq.network.i2p.grpc.server.GrpcRouterMonitorService;
import bisq.network.i2p.router.I2pRouter;
import bisq.network.i2p.router.log.I2pLogLevel;
import bisq.network.i2p.router.state.NetworkState;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterMonitor;
import bisq.network.i2p.router.state.RouterState;
import bisq.network.i2p.router.state.TunnelInfo;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Slf4j
public class I2pRouterService implements Service {
    private final String i2cpHost;
    private final int i2cpPort;
    @Getter
    private final Path i2pDirPath;

    @Getter
    private final Observable<Throwable> throwable = new Observable<>();

    private volatile I2pRouter router;
    private GrpcRouterMonitorServer monitorServer;
    private GrpcRouterMonitorService monitorService;
    private volatile RouterMonitor routerMonitor;
    private volatile boolean shutdownInProgress;

    public I2pRouterService(Application.Parameters parameters, String i2pRouterDir) {
        i2cpHost = Optional.ofNullable(parameters.getNamed().get("i2cpHost")).orElse("127.0.0.1");
        i2cpPort = Optional.ofNullable(parameters.getNamed().get("i2cpPort")).map(Integer::parseInt).orElse(7654);
        i2pDirPath = PlatformUtils.getUserDataDir().resolve(i2pRouterDir);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        router = new I2pRouter(i2pDirPath,
                i2cpHost,
                i2cpPort,
                I2pLogLevel.DEBUG,
                false,
                (int) TimeUnit.SECONDS.toMillis(10),
                512,
                512,
                50);

        routerMonitor = router.getRouterMonitor();

        monitorService = new GrpcRouterMonitorService(router);
        monitorServer = new GrpcRouterMonitorServer(7777, monitorService);

        return monitorServer.initialize()
                .thenCompose(result -> monitorService.initialize())
                .thenCompose(result -> router.startRouter())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("I2P router failed to start, exiting.");
                        shutdown();
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (shutdownInProgress || router == null) {
            return CompletableFuture.completedFuture(true);
        }
        shutdownInProgress = true;

        return Optional.ofNullable(monitorServer)
                .map(GrpcRouterMonitorServer::shutdown)
                .orElse(CompletableFuture.completedFuture(true))
                .thenCompose(result ->
                        Optional.ofNullable(monitorService)
                                .map(GrpcRouterMonitorService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)))
                .thenCompose(result -> router.shutdown());
    }

    // Delegates
    public String getI2cpHost() {
        return router.getI2cpHost();
    }

    public int getI2cpPort() {
        return router.getI2cpPort();
    }

    public ReadOnlyObservable<ProcessState> getProcessState() {
        return router.getRouterMonitor().getProcessState();
    }

    public ReadOnlyObservable<NetworkState> getNetworkState() {
        return router.getRouterMonitor().getNetworkState();
    }

    public ReadOnlyObservable<RouterState> getState() {
        return router.getRouterMonitor().getRouterState();
    }

    public ReadOnlyObservable<TunnelInfo> getTunnelInfo() {
        return router.getRouterMonitor().getTunnelInfo();
    }
}
