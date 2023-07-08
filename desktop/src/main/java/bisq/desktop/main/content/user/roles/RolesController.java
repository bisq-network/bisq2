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

package bisq.desktop.main.content.user.roles;

import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedBondedRoleData;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.user.roles.tabs.RolesTabController;
import bisq.user.UserService;
import bisq.user.role.RoleRegistrationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RolesController implements Controller {
    @Getter
    private final RolesView view;
    private final RolesModel model;
    private final RolesTabController rolesTabController;
    private final RoleRegistrationService roleRegistrationService;
    private final UserService userService;
    private Pin registrationDataSetPin;

    public RolesController(ServiceProvider serviceProvider) {
        userService = serviceProvider.getUserService();
        roleRegistrationService = userService.getRoleRegistrationService();

        rolesTabController = new RolesTabController(serviceProvider);
        model = new RolesModel();
        view = new RolesView(model, this, rolesTabController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        registrationDataSetPin = FxBindings.<AuthorizedBondedRoleData, RolesView.ListItem>bind(model.getListItems())
                .map(data -> new RolesView.ListItem(data, userService))
                .to(roleRegistrationService.getAuthorizedBondedRoleDataSet());
    }

    @Override
    public void onDeactivate() {
        registrationDataSetPin.unbind();
    }


    public void onCopyPublicKeyAsHex(String publicKeyAsHex) {
        ClipboardUtil.copyToClipboard(publicKeyAsHex);
    }
}
