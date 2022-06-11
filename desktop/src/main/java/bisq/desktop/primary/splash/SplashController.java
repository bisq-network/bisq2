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

package bisq.desktop.primary.splash;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.transport.Transport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SplashController implements Controller {
    private final SplashModel model;
    @Getter
    private final SplashView view;
    private final DefaultApplicationService applicationService;
    private Pin pinApplicationStatus;
    private Pin pinClearnetStatus;
    private Pin pinTorStatus;
    private Pin pinI2pStatus;

    public SplashController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new SplashModel();
        view = new SplashView(model, this);
    }

    @Override
    public void onActivate() {
        pinApplicationStatus = FxBindings.bind(model.getApplicationState()).to(applicationService.getState());

        Map<Transport.Type, ServiceNode> map = applicationService.getNetworkService().getServiceNodesByTransport().getMap();
        ServiceNode clearnetNode = map.get(Transport.Type.CLEAR);
        if (clearnetNode != null) {
            pinClearnetStatus = FxBindings.bind(model.getClearServiceNodeState()).to(clearnetNode.getState());
        }
        ServiceNode torNode = map.get(Transport.Type.TOR);
        if (torNode != null) {
            pinTorStatus = FxBindings.bind(model.getTorServiceNodeState()).to(torNode.getState());
        }
        ServiceNode i2pNode = map.get(Transport.Type.I2P);
        if (i2pNode != null) {
            pinI2pStatus = FxBindings.bind(model.getI2pServiceNodeState()).to(i2pNode.getState());
        }
    }

    @Override
    public void onDeactivate() {
        pinApplicationStatus.unbind();
        if (pinClearnetStatus != null) {
            pinClearnetStatus.unbind();
        }
        if (pinTorStatus != null) {
            pinTorStatus.unbind();
        }
        if (pinI2pStatus != null) {
            pinI2pStatus.unbind();
        }
    }

    public void stopAnimation() {
        model.getProgress().set(0);
    }
}
