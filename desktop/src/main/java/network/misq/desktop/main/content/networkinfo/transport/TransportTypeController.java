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

package network.misq.desktop.main.content.networkinfo.transport;

import javafx.beans.property.StringProperty;
import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Controller;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.security.KeyPairService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TransportTypeController implements Controller {
    private final Transport.Type transportType;
    private final TransportTypeModel model;
    @Getter
    private final TransportTypeView view;
    private final NetworkService networkService;
    private final KeyPairService keyPairService;

    public TransportTypeController(DefaultServiceProvider serviceProvider, Transport.Type transportType) {
        networkService = serviceProvider.getNetworkService();

        keyPairService = serviceProvider.getKeyPairService();
        this.transportType = transportType;

        model = new TransportTypeModel(serviceProvider, transportType);
        view = new TransportTypeView(model, this);
    }

    CompletableFuture<String> sendMessage(String message) {
        return model.sendMessage(message);
    }

    public StringProperty addData(String dataText, String id) {
        return model.addData(dataText, id);
    }

    public void onSelectNetworkId(Optional<NetworkId> networkId) {
        model.applyNetworkId(networkId);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

}
