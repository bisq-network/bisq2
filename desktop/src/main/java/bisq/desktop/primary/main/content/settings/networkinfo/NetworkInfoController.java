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
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.content.settings.networkinfo.transport.TransportTypeController;
import bisq.network.p2p.node.transport.Transport;
import lombok.Getter;

public class NetworkInfoController extends TabController<NetworkInfoModel> {
    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final NetworkInfoModel model;
    @Getter
    private final NetworkInfoView view;

    public NetworkInfoController(DefaultServiceProvider serviceProvider,
                                 ContentController contentController,
                                 OverlayController overlayController) {
        super(contentController, overlayController);

        this.serviceProvider = serviceProvider;
        model = new NetworkInfoModel(serviceProvider);
        view = new NetworkInfoView(model, this);
    }

    @Override
    protected NavigationTarget resolveLocalTarget(NavigationTarget navigationTarget) {
        return resolveAsLevel2Host(navigationTarget);
    }

    @Override
    protected Controller getController(NavigationTarget localTarget, NavigationTarget navigationTarget) {
        switch (localTarget) {
            case CLEAR_NET -> {
                return new TransportTypeController(serviceProvider, Transport.Type.CLEAR);
            }
            case TOR -> {
                return new TransportTypeController(serviceProvider, Transport.Type.TOR);
            }
            case I2P -> {
                return new TransportTypeController(serviceProvider, Transport.Type.I2P);
            }
            default -> throw new IllegalArgumentException("Invalid navigationTarget for this host. NavigationTarget=" + localTarget);
        }
    }

    @Override
    public void onViewAttached() {
        model.getSupportedTransportTypes().stream()
                .min(Enum::compareTo)
                .map(model::getNavigationTargetFromTransportType)
                .ifPresent(this::navigateTo);
    }
}
