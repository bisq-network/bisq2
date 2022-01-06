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

package bisq.desktop.main.content.networkinfo;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.networkinfo.transport.TransportTypeController;
import bisq.network.p2p.NetworkId;
import bisq.network.p2p.node.transport.Transport;
import javafx.beans.property.StringProperty;
import lombok.Getter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        model.getSupportedTransportTypes().stream()
                .min(Enum::compareTo)
                .ifPresent(e -> onTabSelected(Optional.of(e)));
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

    CompletableFuture<String> sendMessage(String message) {
        return model.sendMessage(message);
    }

    public StringProperty addData(String dataText, String domainId) {
        return model.addData(dataText, domainId);
    }

    public void onSelectNetworkId(Optional<NetworkId> networkId) {
        model.applyNetworkId(networkId);
    }

}
