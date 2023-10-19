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

package bisq.desktop.main.content.authorized_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import bisq.desktop.main.content.authorized_role.mediator.MediatorController;
import bisq.desktop.main.content.authorized_role.moderator.ModeratorController;
import bisq.desktop.main.content.authorized_role.release_manager.ReleaseManagerController;
import bisq.desktop.main.content.authorized_role.security_manager.SecurityManagerController;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AuthorizedRoleController extends TabController<AuthorizedRoleModel> {
    private final ServiceProvider serviceProvider;
    @Getter
    private final AuthorizedRoleView view;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;
    private Pin bondedRolesPin, selectedUserIdentityPin;

    public AuthorizedRoleController(ServiceProvider serviceProvider) {
        super(new AuthorizedRoleModel(List.of(BondedRoleType.values())), NavigationTarget.AUTHORIZED_ROLE);

        this.serviceProvider = serviceProvider;
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        view = new AuthorizedRoleView(model, this);

        onBondedRolesChanged();
    }

    @Override
    public void onActivate() {
        bondedRolesPin = authorizedBondedRolesService.getBondedRoles().addObserver(this::onBondedRolesChanged);
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(e -> onBondedRolesChanged());
    }

    @Override
    public void onDeactivate() {
        bondedRolesPin.unbind();
        selectedUserIdentityPin.unbind();
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case MEDIATOR:
                return Optional.of(new MediatorController(serviceProvider));
            case MODERATOR:
                return Optional.of(new ModeratorController(serviceProvider));
            case SECURITY_MANAGER:
                return Optional.of(new SecurityManagerController(serviceProvider));
            case RELEASE_MANAGER:
                return Optional.of(new ReleaseManagerController(serviceProvider));
            case SEED_NODE:
            case ORACLE_NODE:
            case EXPLORER_NODE:
            case MARKET_PRICE_NODE:
                return Optional.of(new RoleInfo(serviceProvider).getController());
            default:
                return Optional.empty();
        }
    }

    private void onBondedRolesChanged() {
        UIThread.run(() -> {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
            model.getAuthorizedBondedRoles().setAll(authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                    .filter(bondedRole -> selectedUserIdentity != null && selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()))
                    .map(AuthorizedBondedRole::getBondedRoleType)
                    .collect(Collectors.toSet()));
        });
    }
}
