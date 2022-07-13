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
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.KeyPairService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@Getter
public class NetworkInfoModel implements Model {
    private final NetworkService networkService;
    private final BooleanProperty clearNetDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty torDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty i2pDisabled = new SimpleBooleanProperty(false);
    private final Set<Transport.Type> supportedTransportTypes;

    private final KeyPairService keyPairService;
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.get("na"));

    public NetworkInfoModel(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        supportedTransportTypes = networkService.getSupportedTransportTypes();
        clearNetDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.CLEAR));
        torDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.TOR));
        i2pDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.I2P));

        keyPairService = applicationService.getKeyPairService();
    }
}
