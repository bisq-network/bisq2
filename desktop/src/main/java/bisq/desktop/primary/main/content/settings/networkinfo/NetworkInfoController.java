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

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.networkinfo.transport.TransportTypeController;
import bisq.network.p2p.node.transport.Transport;
import lombok.Getter;

import java.util.Optional;

public class NetworkInfoController extends TabController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final NetworkInfoModel model;
    @Getter
    private final NetworkInfoView view;

    public NetworkInfoController(DefaultApplicationService applicationService) {
        super(NavigationTarget.NETWORK_INFO);

        this.applicationService = applicationService;
        model = new NetworkInfoModel(applicationService);
        view = new NetworkInfoView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CLEAR_NET -> {
                return Optional.of(new TransportTypeController(applicationService, Transport.Type.CLEAR));
            }
            case TOR -> {
                return Optional.of(new TransportTypeController(applicationService, Transport.Type.TOR));
            }
            case I2P -> {
                return Optional.of(new TransportTypeController(applicationService, Transport.Type.I2P));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
