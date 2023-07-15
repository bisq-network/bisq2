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

package bisq.desktop.main.content.authorized_role.moderator;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModeratorController implements Controller {
    @Getter
    private final ModeratorView view;
    private final ModeratorModel model;
    private final UserIdentityService userIdentityService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private Pin userIdentityPin;

    public ModeratorController(ServiceProvider serviceProvider) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        RoleInfo roleInfo = new RoleInfo(serviceProvider);
        model = new ModeratorModel();
        view = new ModeratorView(model, this, roleInfo.getRoot());
    }

    @Override
    public void onActivate() {
        userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> UIThread.run(this::onUserIdentity));
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
    }

    private void onUserIdentity() {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        authorizedBondedRolesService.getBondedRoles().stream()
                .filter(bondedRole -> selectedUserIdentity != null && selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getAuthorizedBondedRole().getProfileId()))
                .findAny()
                .ifPresent(bondedRole -> {
                    model.setBondedRole(bondedRole);
                });
    }

}
