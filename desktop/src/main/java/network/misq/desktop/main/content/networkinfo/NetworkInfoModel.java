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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Model;
import network.misq.desktop.main.content.networkinfo.transport.TransportTypeView;
import network.misq.network.NetworkService;
import network.misq.network.p2p.node.transport.Transport;

import java.util.Optional;

@Getter
public class NetworkInfoModel implements Model {
    private final NetworkService networkService;

    private final BooleanProperty clearNetDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty torDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty i2pDisabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<Optional<TransportTypeView>> transportTypeView = new SimpleObjectProperty<>();
    @Setter
    private Optional<Transport.Type> selectedTransportType = Optional.empty();

    public NetworkInfoModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();

        clearNetDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.CLEAR));
        torDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.TOR));
        i2pDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.I2P));
    }

    public void activate() {
    }

    public void deactivate() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}
