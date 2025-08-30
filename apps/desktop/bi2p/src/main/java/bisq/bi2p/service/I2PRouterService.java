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

package bisq.bi2p.service;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.platform.PlatformUtils;
import bisq.network.i2p.grpc.server.Bi2pGrpcServer;
import bisq.network.i2p.grpc.server.Bi2pGrpcService;
import bisq.network.i2p.router.I2pRouter;
import bisq.network.i2p.router.RouterSetup;
import bisq.network.i2p.router.utils.I2PLogLevel;
import bisq.network.i2p.router.state.NetworkState;
import bisq.network.i2p.router.state.ProcessState;
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
public class I2PRouterService implements Service {

    private final String i2cpHost;
    private final int i2cpPort;
    private final String bi2pGrpcHost;
    private final Integer bi2pGrpcPort;
    @Getter
    private final Path i2pDirPath;
    @Getter
    private final Observable<Throwable> throwable = new Observable<>();
    private final I2pRouter router;
    private final Bi2pGrpcServer monitorServer;
    private final Bi2pGrpcService monitorService;
    private volatile boolean shutdownInProgress;

    public I2PRouterService(Application.Parameters parameters, String bi2pDir) {
        i2cpHost = Optional.ofNullable(parameters.getNamed().get("i2cpHost")).orElse("127.0.0.1");
        i2cpPort = Optional.ofNullable(parameters.getNamed().get("i2cpPort")).map(Integer::parseInt).orElse(RouterSetup.DEFAULT_I2CP_PORT);
        bi2pGrpcHost = Optional.ofNullable(parameters.getNamed().get("bi2pGrpcHost")).orElse("127.0.0.1");
        bi2pGrpcPort = Optional.ofNullable(parameters.getNamed().get("bi2pGrpcPort")).map(Integer::parseInt).orElse(RouterSetup.DEFAULT_BI2P_I2CP_PORT);
        i2pDirPath = PlatformUtils.getUserDataDir().resolve(bi2pDir);
        log.info("I2CP {}:{}; Grpc server listening at: {}:{}", i2cpHost, i2cpPort, bi2pGrpcHost, bi2pGrpcPort);

        router = new I2pRouter(i2pDirPath,
                i2cpHost,
                i2cpPort,
                I2PLogLevel.INFO,
                false,
                (int) TimeUnit.MINUTES.toMillis(10),
                512,
                512,
                80);

        monitorService = new Bi2pGrpcService(router);
        monitorServer = new Bi2pGrpcServer(bi2pGrpcPort, monitorService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return monitorServer.initialize()
                .thenCompose(result -> monitorService.initialize())
                .thenCompose(result -> router.startRouter())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("I2P router failed to start, exiting.");
                        this.throwable.set(throwable);
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
                .map(Bi2pGrpcServer::shutdown)
                .orElse(CompletableFuture.completedFuture(true))
                .handle((r, t) -> t == null)
                .thenCompose(result ->
                        Optional.ofNullable(monitorService)
                                .map(Bi2pGrpcService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true))
                                .handle((r, t) -> t == null))
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
