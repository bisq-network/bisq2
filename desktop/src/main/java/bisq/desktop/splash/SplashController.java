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

import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.State;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.TransportType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.util.Map;

@Slf4j
public class SplashController implements Controller {
    private final SplashModel model;
    @Getter
    private final SplashView view;
    private final ServiceProvider serviceProvider;
    private final Observable<State> applicationServiceState;
    private Pin pinApplicationStatus;
    private Pin pinClearNetStatus;
    private Pin pinTorStatus;
    private Pin pinI2pStatus;
    private Subscription clearState;
    private Subscription torState;
    private Subscription i2pState;

    public SplashController(Observable<State> applicationServiceState, ServiceProvider serviceProvider) {
        this.applicationServiceState = applicationServiceState;
        this.serviceProvider = serviceProvider;
        model = new SplashModel();
        view = new SplashView(model, this);
    }

    @Override
    public void onActivate() {
        pinApplicationStatus = FxBindings.bind(model.getApplicationState()).to(applicationServiceState);

        Map<TransportType, Observable<Node.State>> map = serviceProvider.getNetworkService().getNodeStateByTransportType();
        if (map.containsKey(TransportType.CLEAR)) {
            pinClearNetStatus = FxBindings.bind(model.getClearServiceNodeState()).to(map.get(TransportType.CLEAR));
        }
        if (map.containsKey(TransportType.TOR)) {
            pinTorStatus = FxBindings.bind(model.getTorServiceNodeState()).to(map.get(TransportType.TOR));
        }
        if (map.containsKey(TransportType.I2P)) {
            pinI2pStatus = FxBindings.bind(model.getI2pServiceNodeState()).to(map.get(TransportType.I2P));
        }

        clearState = createNetworkSubscription(model.getClearServiceNodeState(), model.getClearState());
        torState = createNetworkSubscription(model.getTorServiceNodeState(), model.getTorState());
        i2pState = createNetworkSubscription(model.getI2pServiceNodeState(), model.getI2pState());
    }

    @Override
    public void onDeactivate() {
        pinApplicationStatus.unbind();
        if (pinClearNetStatus != null) {
            pinClearNetStatus.unbind();
        }
        if (pinTorStatus != null) {
            pinTorStatus.unbind();
        }
        if (pinI2pStatus != null) {
            pinI2pStatus.unbind();
        }

        clearState.unsubscribe();
        torState.unsubscribe();
        i2pState.unsubscribe();
    }

    public void startAnimation() {
        model.getProgress().set(-1);
    }

    public void stopAnimation() {
        model.getProgress().set(0);
    }

    private Subscription createNetworkSubscription(
            ObjectProperty<Node.State> stateProperty,
            StringProperty targetProperty) {
        MonadicBinding<String> binding = EasyBind.map(stateProperty, this::getStatus);
        return EasyBind.subscribe(binding, targetProperty::set);
    }

    private String getStatus(Node.State state) {
        return state == null ? "" : String.format("%s%%", mapState(state));
    }

    private String mapState(Node.State state) {
        switch (state) {
            case NEW:
                return "0";
            case STARTING:
                return "50";
            case RUNNING:
                return "75";
            default:
                return "";
        }
    }
}
