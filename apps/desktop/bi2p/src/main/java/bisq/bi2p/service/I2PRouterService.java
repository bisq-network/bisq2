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
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkPortValidation;
import bisq.network.i2p.grpc.server.Bi2pGrpcServer;
import bisq.network.i2p.grpc.server.Bi2pGrpcService;
import bisq.network.i2p.router.I2PRouter;
import bisq.network.i2p.router.RouterSetup;
import bisq.network.i2p.router.state.NetworkState;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterState;
import bisq.network.i2p.router.state.TunnelInfo;
import bisq.network.i2p.router.utils.I2PLogLevel;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Slf4j
public class I2PRouterService implements Service {
    @Getter
    private final Path i2pDirPath;
    @Getter
    private final Observable<Throwable> throwable = new Observable<>();
    private final I2PRouter router;
    private final Bi2pGrpcServer bi2pGrpcServer;
    private final Bi2pGrpcService bi2pGrpcService;
    private volatile boolean shutdownInProgress;

    public I2PRouterService(Application.Parameters parameters, String bi2pDir) {
        i2pDirPath = PlatformUtils.getUserDataDir().resolve(bi2pDir);

        String i2cpHost = Optional.ofNullable(parameters.getNamed().get("i2cpHost"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(RouterSetup.DEFAULT_I2CP_HOST);
        int i2cpPort = parsePort(parameters.getNamed().get("i2cpPort"), RouterSetup.DEFAULT_I2CP_PORT);

        String bi2pGrpcHost = Optional.ofNullable(parameters.getNamed().get("bi2pGrpcHost"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(RouterSetup.DEFAULT_BI2P_GRPC_HOST);
        int bi2pGrpcPort = parsePort(parameters.getNamed().get("bi2pGrpcPort"), RouterSetup.DEFAULT_BI2P_GRPC_PORT);

        String httpProxyHost = Optional.ofNullable(parameters.getNamed().get("httpProxyHost"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(RouterSetup.DEFAULT_HTTP_PROXY_HOST);
        int httpProxyPort = parsePort(parameters.getNamed().get("httpProxyPort"), RouterSetup.DEFAULT_HTTP_PROXY_PORT);
        boolean httpProxyEnabled = Optional.ofNullable(parameters.getNamed().get("httpProxyEnabled"))
                .map(String::trim)
                .map(e -> e.equalsIgnoreCase("true"))
                .orElse(true);

        router = new I2PRouter(i2pDirPath,
                i2cpHost,
                i2cpPort,
                httpProxyHost,
                httpProxyPort,
                httpProxyEnabled,
                I2PLogLevel.INFO,
                false,
                (int) TimeUnit.MINUTES.toMillis(10),
                512,
                512,
                80);

        log.info("I2CP {}:{}; Grpc server listening at: {}:{}", i2cpHost, i2cpPort, bi2pGrpcHost, bi2pGrpcPort);
        bi2pGrpcService = new Bi2pGrpcService(router);
        bi2pGrpcServer = new Bi2pGrpcServer(bi2pGrpcHost, bi2pGrpcPort, bi2pGrpcService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return bi2pGrpcServer.initialize()
                .thenCompose(result -> bi2pGrpcService.initialize())
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

        return Optional.ofNullable(bi2pGrpcServer)
                .map(Bi2pGrpcServer::shutdown)
                .orElse(CompletableFuture.completedFuture(true))
                .handle((r, t) -> t == null)
                .thenCompose(result ->
                        Optional.ofNullable(bi2pGrpcService)
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

    private static int parsePort(String raw, int defaultPort) {
        if (StringUtils.isEmpty(raw)) return defaultPort;
        try {
            int port = Integer.parseInt(raw.trim());
            return NetworkPortValidation.isValid(port) ? port : defaultPort;
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }
}
