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
import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.platform.PlatformUtils;
import bisq.network.i2p.router.I2pRouter;
import bisq.network.i2p.router.RouterStateObserver;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Slf4j
public class I2pRouterService implements Service {

    private volatile boolean shutdownInProgress;
    @Getter
    private final Observable<Throwable> throwable = new Observable<>();

    @Getter
    private final Observable<String> statusMessage = new Observable<>();
    private Application.Parameters parameters;
    private volatile I2pRouter router;
    private Pin statusPin;
    private int i2cpPort = 7654;
    private String dataDir = "bisq2_i2p_router";

    public I2pRouterService() {
    }

    public void onApplicationReady(Application.Parameters parameters) {
        String portParam = parameters.getNamed().get("i2cpPort");
        if (portParam != null) {
            i2cpPort = Integer.parseInt(portParam);
        }

        String dataDirParam = parameters.getNamed().get("dataDir");
        if (dataDirParam != null) {
            dataDir = dataDirParam;
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        String i2pDirPath = PlatformUtils.getUserDataDir().resolve(dataDir).toString();
        router = new I2pRouter(i2pDirPath, i2cpPort);

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

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (shutdownInProgress || router == null) {
            return CompletableFuture.completedFuture(true);
        }
        shutdownInProgress = true;

        if (statusPin != null) {
            statusPin.unbind();
        }
        return router.shutdown();
    }

    public String getDirPath() {
        return router.getDirPath();
    }

    public String getI2cpHost() {
        return router.getI2cpHost();
    }

    public int getI2cpPort() {
        return router.getI2cpPort();
    }

    public ReadOnlyObservable<RouterStateObserver.ProcessState> getProcessState() {
        return router.getProcessState();
    }

    public ReadOnlyObservable<RouterStateObserver.NetworkState> getNetworkState() {
        return router.getNetworkState();
    }

    public ReadOnlyObservable<RouterStateObserver.RouterState> getState() {
        return router.getRouterState();
    }

    public ReadOnlyObservable<RouterStateObserver.TunnelInfo> getTunnelInfo() {
        return router.getTunnelInfo();
    }
}
