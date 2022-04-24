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
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.KeyPairService;
import bisq.settings.CookieKey;
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
    // private final ObjectProperty<Optional<TransportTypeView>> transportTypeView = new SimpleObjectProperty<>();
    private final Set<Transport.Type> supportedTransportTypes;
    //  private final Set<NavigationTarget> supportedNavigationTarget;
    // private final Optional<Transport.Type> selectedTransportType = Optional.empty();

    private final KeyPairService keyPairService;
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.get("na"));

    public NetworkInfoModel(DefaultApplicationService applicationService) {
        networkService = applicationService.getNetworkService();
        NavigationTarget persistedNavigationTarget = NavigationTarget.valueOf(
                applicationService.getSettingsService().getPersistableStore().getCookie().getValue(CookieKey.NAVIGATION_TARGET));
       /* if (persistedNavigationTarget.getParent().filter(parent -> parent == NavigationTarget.NETWORK_INFO).isPresent()) {
            navigationTarget = persistedNavigationTarget;
        }*/

        supportedTransportTypes = networkService.getSupportedTransportTypes();
      /*  supportedNavigationTarget = supportedTransportTypes.stream()
                .map(this::getNavigationTargetFromTransportType)
                .collect(Collectors.toSet());*/

        clearNetDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.CLEAR));
        torDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.TOR));
        i2pDisabled.set(!networkService.isTransportTypeSupported(Transport.Type.I2P));

        keyPairService = applicationService.getKeyPairService();
    }

   /* @Override
    public NavigationTarget getDefaultNavigationTarget() {
        if (supportedTransportTypes == null) {
            return NavigationTarget.CLEAR_NET;
        }
        return supportedTransportTypes.stream()
                .min(Enum::compareTo)
                .map(this::getNavigationTargetFromTransportType)
                .orElse(NavigationTarget.CLEAR_NET);
    }
*/
 /*   boolean isDisabled(NavigationTarget navigationTarget) {
        return !supportedNavigationTarget.contains(navigationTarget);
    }*/

/*    private NavigationTarget getNavigationTargetFromTransportType(Transport.Type type) {
        return switch (type) {
            case CLEAR -> NavigationTarget.CLEAR_NET;
            case TOR -> NavigationTarget.TOR;
            case I2P -> NavigationTarget.I2P;
        };
    }*/

}
