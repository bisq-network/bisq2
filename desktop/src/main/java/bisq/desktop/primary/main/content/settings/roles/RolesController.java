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

package bisq.desktop.primary.main.content.settings.roles;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.settings.roles.registration.RolesTabController;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.user.UserService;
import bisq.user.reputation.ReputationService;
import bisq.user.role.RoleRegistrationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RolesController implements Controller {
    @Getter
    private final RolesView view;
    private final ReputationService reputationService;
    private final RolesModel model;
    private final RolesTabController rolesTabController;
    private final RoleRegistrationService roleRegistrationService;
    private Pin roleRegistrationDataSetPin;

    public RolesController(DefaultApplicationService applicationService) {
        UserService userService = applicationService.getUserService();
        reputationService = userService.getReputationService();
        roleRegistrationService = userService.getRoleRegistrationService();

        rolesTabController = new RolesTabController(applicationService);
        model = new RolesModel();
        view = new RolesView(model, this, rolesTabController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        roleRegistrationDataSetPin = FxBindings.<AuthorizedData, RolesView.ListItem>bind(model.getListItems())
                .map(data -> new RolesView.ListItem(data, reputationService.getProfileAgeService()))
                .to(roleRegistrationService.getAuthorizedDataSet());
    }

    @Override
    public void onDeactivate() {
        roleRegistrationDataSetPin.unbind();
    }


    public void onCopyPublicKeyAsHex(String publicKeyAsHex) {
        ClipboardUtil.copyToClipboard(publicKeyAsHex);
    }
}
