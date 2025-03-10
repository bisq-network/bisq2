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

package bisq.desktop.splash;

import bisq.application.State;
import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileUtils;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.util.MathUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIClock;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SplashController implements Controller {
    private final SplashModel model;
    @Getter
    private final SplashView view;
    private final Observable<State> applicationServiceState;
    private final NetworkService networkService;
    private final ServiceProvider serviceProvider;
    private Pin applicationServiceStatePin;

    public SplashController(Observable<State> applicationServiceState, ServiceProvider serviceProvider) {
        this.applicationServiceState = applicationServiceState;
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();

        model = new SplashModel(ApplicationVersion.getVersion());
        view = new SplashView(model, this);
    }

    @Override
    public void onActivate() {
        applicationServiceStatePin = applicationServiceState.addObserver(state ->
                UIThread.run(() -> model.getApplicationServiceState().set(Res.get("splash.applicationServiceState." + state.name())))
        );

        if (networkService.getSupportedTransportTypes().contains(TransportType.CLEAR)) {
            BootstrapElementsPerTransport bootstrapElementsPerTransport = new BootstrapElementsPerTransport(TransportType.CLEAR, serviceProvider);
            model.getBootstrapElementsPerTransports().add(bootstrapElementsPerTransport);
        }
        if (networkService.getSupportedTransportTypes().contains(TransportType.TOR)) {
            BootstrapElementsPerTransport bootstrapElementsPerTransport = new BootstrapElementsPerTransport(TransportType.TOR, serviceProvider);
            model.getBootstrapElementsPerTransports().add(bootstrapElementsPerTransport);
        }
        if (networkService.getSupportedTransportTypes().contains(TransportType.I2P)) {
            BootstrapElementsPerTransport bootstrapElementsPerTransport = new BootstrapElementsPerTransport(TransportType.I2P, serviceProvider);
            model.getBootstrapElementsPerTransports().add(bootstrapElementsPerTransport);
        }

        long startTime = System.currentTimeMillis();
        long maxExpectedStartupTime = TimeUnit.SECONDS.toMillis(model.getMaxExpectedStartupTime());
        UIClock.addOnSecondTickListener(() -> {
            double passed = System.currentTimeMillis() - startTime;
            if (passed > maxExpectedStartupTime) {
                model.getIsSlowStartup().set(true);
            }
            double progress = Math.min(1, passed / maxExpectedStartupTime);
            model.getProgress().set(progress);
            model.getDuration().set(MathUtils.roundDoubleToInt(passed / 1000d) + " sec.");
        });
    }

    @Override
    public void onDeactivate() {
        applicationServiceStatePin.unbind();
        model.getBootstrapElementsPerTransports().clear();
        model.getProgress().set(0);
    }

    public void startAnimation() {
        model.getProgress().set(-1);
    }

    public void stopAnimation() {
        model.getProgress().set(1);
    }

    public void onDeleteTor() {
        var conf = serviceProvider.getConfig();
        File torDir = conf.getBaseDir().resolve("tor").toFile();
        if (torDir.exists()) {
            FileUtils.deleteOnExit(torDir);
            serviceProvider.getShutDownHandler().shutdown();
        }
    }
}
