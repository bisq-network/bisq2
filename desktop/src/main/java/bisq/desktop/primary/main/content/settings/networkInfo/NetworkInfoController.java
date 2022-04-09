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

package bisq.desktop.primary.main.content.settings.networkInfo;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.settings.networkInfo.transport.TransportTypeController;
import bisq.network.p2p.node.transport.Transport;
import javafx.scene.Node;
import lombok.Getter;

import java.util.Optional;
import java.util.Set;

public class NetworkInfoController implements Controller {
    private final DefaultApplicationService applicationService;
    @Getter
    private final NetworkInfoModel model;
    @Getter
    private final NetworkInfoView view;
    @Getter
    private Optional<TransportTypeController> clearNetController = Optional.empty();
    @Getter
    private Optional<TransportTypeController> torController = Optional.empty();
    @Getter
    private Optional<TransportTypeController> i2pController = Optional.empty();

    public NetworkInfoController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new NetworkInfoModel(applicationService);


        Set<Transport.Type> supportedTransportTypes = applicationService.getNetworkService().getSupportedTransportTypes();
        view = new NetworkInfoView(model, this,
                getTransportTypeViewRoot(supportedTransportTypes, Transport.Type.CLEAR),
                getTransportTypeViewRoot(supportedTransportTypes, Transport.Type.TOR),
                getTransportTypeViewRoot(supportedTransportTypes, Transport.Type.I2P));
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    private Optional<Node> getTransportTypeViewRoot(Set<Transport.Type> supportedTransportTypes, Transport.Type type) {
        if (supportedTransportTypes.contains(type)) {
            return Optional.of(new TransportTypeController(applicationService, type)).map(e -> e.getView().getRoot());
        } else {
            return Optional.empty();
        }
    }
}
