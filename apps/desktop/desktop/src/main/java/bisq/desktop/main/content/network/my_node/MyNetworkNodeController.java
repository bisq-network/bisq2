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

package bisq.desktop.main.content.network.my_node;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.network.my_node.transport.TransportController;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import javafx.scene.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class MyNetworkNodeController implements Controller {
    @Getter
    private final MyNetworkNodeModel model;
    @Getter
    private final MyNetworkNodeView view;
    private final ServiceProvider serviceProvider;
    @Getter
    private final Optional<TransportController> clearNetController = Optional.empty();
    @Getter
    private final Optional<TransportController> torController = Optional.empty();
    @Getter
    private final Optional<TransportController> i2pController = Optional.empty();

    public MyNetworkNodeController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        NetworkService networkService = serviceProvider.getNetworkService();

        model = new MyNetworkNodeModel(networkService.getSupportedTransportTypes(),
                !networkService.isTransportTypeSupported(TransportType.CLEAR),
                !networkService.isTransportTypeSupported(TransportType.TOR),
                !networkService.isTransportTypeSupported(TransportType.I2P));

        Set<TransportType> supportedTransportTypes = serviceProvider.getNetworkService().getSupportedTransportTypes();
        view = new MyNetworkNodeView(model, this,
                getTransportTypeViewRoot(supportedTransportTypes, TransportType.CLEAR),
                getTransportTypeViewRoot(supportedTransportTypes, TransportType.TOR),
                getTransportTypeViewRoot(supportedTransportTypes, TransportType.I2P));
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    private Optional<Node> getTransportTypeViewRoot(Set<TransportType> supportedTransportTypes, TransportType type) {
        if (supportedTransportTypes.contains(type)) {
            return Optional.of(new TransportController(serviceProvider, type)).map(e -> e.getView().getRoot());
        } else {
            return Optional.empty();
        }
    }
}
