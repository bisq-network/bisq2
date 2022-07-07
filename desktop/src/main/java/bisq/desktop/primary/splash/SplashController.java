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
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.util.ArrayList;
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
    private Subscription statePin;
    private MonadicBinding<String> binding;

    public SplashController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new SplashModel();
        view = new SplashView(model, this);
    }

    @Override
    public void onActivate() {
        pinApplicationStatus = FxBindings.bind(model.getApplicationState()).to(applicationService.getState());

        Map<Transport.Type, Observable<Node.State>> map = applicationService.getNetworkService().getNodeStateByTransportType();
        if (map.containsKey(Transport.Type.CLEAR)) {
            pinClearnetStatus = FxBindings.bind(model.getClearServiceNodeState()).to(map.get(Transport.Type.CLEAR));
        }
        if (map.containsKey(Transport.Type.TOR)) {
            pinTorStatus = FxBindings.bind(model.getTorServiceNodeState()).to(map.get(Transport.Type.TOR));
        }
        if (map.containsKey(Transport.Type.I2P)) {
            pinI2pStatus = FxBindings.bind(model.getI2pServiceNodeState()).to(map.get(Transport.Type.I2P));
        }

        binding = EasyBind.combine(model.getClearServiceNodeState(),
                model.getTorServiceNodeState(),
                model.getI2pServiceNodeState(),
                this::getStatus);

        statePin = EasyBind.subscribe(binding, state -> model.getTransportState().set(state));

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
        statePin.unsubscribe();
    }

    public void stopAnimation() {
        model.getProgress().set(0);
    }

    private String getStatus(Node.State clearnetState, Node.State torState, Node.State i2pState) {
        ArrayList<String> networkStatuses = new ArrayList<>();
        if (clearnetState != null) {
            networkStatuses.add(String.format("Clear %s%%", mapState(clearnetState)));
        }
        if (torState != null) {
            networkStatuses.add(String.format("Tor %s%%", mapState(torState)));
        }
        if (i2pState != null) {
            networkStatuses.add(String.format("I2P %s%%", mapState(i2pState)));
        }
        return Joiner.on(" | ").join(networkStatuses).toUpperCase();
    }

    private String mapState(Node.State state) {
        switch (state) {
            case NEW:
                return "0";
            case STARTING:
                return "50";
            case RUNNING:
                return "100";
            default:
                return "";
        }
    }
}
