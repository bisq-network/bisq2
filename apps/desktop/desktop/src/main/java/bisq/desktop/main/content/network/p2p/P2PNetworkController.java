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

package bisq.desktop.main.content.network.p2p;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.network.p2p.version.VersionDistributionController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class P2PNetworkController implements Controller {
    @Getter
    private final P2PNetworkView view;
    private final P2PNetworkModel model;

    public P2PNetworkController(ServiceProvider serviceProvider) {
        VersionDistributionController versionDistributionController = new VersionDistributionController(serviceProvider);
        model = new P2PNetworkModel();
        view = new P2PNetworkView(model, this, versionDistributionController.getView().getRoot());
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}
