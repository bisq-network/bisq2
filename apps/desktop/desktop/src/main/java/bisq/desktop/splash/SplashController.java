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

import bisq.common.application.ApplicationVersion;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.application.State;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.common.network.TransportType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
            model.getBootstrapStateDisplays().add(new BootstrapStateDisplay(TransportType.CLEAR, serviceProvider));
            networkService.getBootstrapInfoByTransportType().get(TransportType.CLEAR).getBootstrapProgress().addObserver(progress ->
                    UIThread.run(() -> applyMaxProgress(progress)));
        }
        if (networkService.getSupportedTransportTypes().contains(TransportType.TOR)) {
            model.getBootstrapStateDisplays().add(new BootstrapStateDisplay(TransportType.TOR, serviceProvider));
            networkService.getBootstrapInfoByTransportType().get(TransportType.TOR).getBootstrapProgress().addObserver(progress ->
                    UIThread.run(() -> applyMaxProgress(progress)));
        }
        if (networkService.getSupportedTransportTypes().contains(TransportType.I2P)) {
            model.getBootstrapStateDisplays().add(new BootstrapStateDisplay(TransportType.I2P, serviceProvider));
            networkService.getBootstrapInfoByTransportType().get(TransportType.I2P).getBootstrapProgress().addObserver(progress ->
                    UIThread.run(() -> applyMaxProgress(progress)));
        }
    }

    @Override
    public void onDeactivate() {
        applicationServiceStatePin.unbind();
        model.getBootstrapStateDisplays().clear();
        model.getProgress().set(0);
    }

    public void startAnimation() {
        applyMaxProgress(-1);
    }

    public void stopAnimation() {
        applyMaxProgress(0);
    }

    private void applyMaxProgress(double progress) {
        if (model.getProgress().get() < progress) {
            model.getProgress().set(progress);
        }
    }
}
