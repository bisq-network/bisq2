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

package network.misq.desktop.main.content.networkinfo;

import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.networkinfo.transport.TransportTypeController;
import network.misq.network.p2p.node.transport.Transport;

import java.util.Optional;

public class NetworkInfoController implements Controller {
    private final NetworkInfoModel model;
    @Getter
    private final NetworkInfoView view;
    private final DefaultServiceProvider serviceProvider;
    private Optional<TransportTypeController> selectedTransportTypeController;

    public NetworkInfoController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        model = new NetworkInfoModel(serviceProvider);
        view = new NetworkInfoView(model, this);
    }

    @Override
    public void activate() {
        onTabSelected(Optional.of(Transport.Type.CLEAR));
    }

    @Override
    public void deactivate() {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onTabSelected(Optional<Transport.Type> transportTypeOptional) {
        selectedTransportTypeController = transportTypeOptional.map(transportType ->
                new TransportTypeController(serviceProvider, transportType));
        model.setSelectedTransportType(transportTypeOptional);
        model.getTransportTypeView().set(selectedTransportTypeController.map(TransportTypeController::getView)
                .or(Optional::empty));
    }
}
