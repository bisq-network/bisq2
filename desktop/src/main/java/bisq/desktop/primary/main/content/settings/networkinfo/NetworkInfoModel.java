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

package bisq.desktop.primary.main.content.settings.networkinfo;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.TabModel;
import bisq.desktop.primary.main.content.settings.networkinfo.transport.TransportTypeView;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.NetworkId;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.KeyPairService;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Getter
public class NetworkInfoModel extends TabModel {
    private final NetworkService networkService;
    @Getter
    private final Transport.Type defaultTransportType = Transport.Type.CLEAR;

    private final BooleanProperty clearNetDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty torDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty i2pDisabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<Optional<TransportTypeView>> transportTypeView = new SimpleObjectProperty<>();
    private final Set<Transport.Type> supportedTransportTypes;
    private final IdentityService identityService;
    @Setter
    private Optional<Transport.Type> selectedTransportType = Optional.empty();

    private final KeyPairService keyPairService;
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.common.get("na"));
    private Optional<NetworkId> selectedNetworkId = Optional.empty();

    public NetworkInfoModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();

        supportedTransportTypes = networkService.getSupportedTransportTypes();

        clearNetDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.CLEAR));
        torDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.TOR));
        i2pDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.I2P));

        keyPairService = serviceProvider.getKeyPairService();
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.CLEAR_NET;
    }

    public void onViewAttached() {
        super.onViewAttached();
    }

    public void onViewDetached() {
        super.onViewDetached();
    }

    public NavigationTarget getNavigationTargetFromTransportType(Transport.Type type) {
        return switch (type) {
            case CLEAR -> NavigationTarget.CLEAR_NET;
            case TOR -> NavigationTarget.TOR;
            case I2P -> NavigationTarget.I2P;
        };
    }
}
