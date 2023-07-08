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

package bisq.desktop.main.content.user.nodes;

import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedBondedNodeData;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.nodes.tabs.NodesTabController;
import bisq.user.UserService;
import bisq.user.node.NodeRegistrationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodesController implements Controller {
    @Getter
    private final NodesView view;
    private final NodesModel model;
    private final NodesTabController nodesTabController;
    private final NodeRegistrationService nodeRegistrationService;
    private final UserService userService;
    private Pin registrationDataSetPin;

    public NodesController(ServiceProvider serviceProvider) {
        userService = serviceProvider.getUserService();
        nodeRegistrationService = userService.getNodeRegistrationService();

        nodesTabController = new NodesTabController(serviceProvider);
        model = new NodesModel();
        view = new NodesView(model, this, nodesTabController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        registrationDataSetPin = FxBindings.<AuthorizedBondedNodeData, NodesView.ListItem>bind(model.getListItems())
                .map(data -> new NodesView.ListItem(data, userService))
                .to(nodeRegistrationService.getAuthorizedBondedNodeDataSet());
    }

    @Override
    public void onDeactivate() {
        registrationDataSetPin.unbind();
    }

    public void onCopyPublicKeyAsHex(String publicKeyAsHex) {
        ClipboardUtil.copyToClipboard(publicKeyAsHex);
    }
}
