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

import bisq.bonded_roles.AuthorizedBondedRole;
import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.bonded_roles.BondedRoleType;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.authorized_role.explorer.ExplorerNodeController;
import bisq.desktop.main.content.authorized_role.market_price.MarketPriceNodeController;
import bisq.desktop.main.content.authorized_role.mediator.MediatorController;
import bisq.desktop.main.content.authorized_role.moderator.ModeratorController;
import bisq.desktop.main.content.authorized_role.oracle.OracleNodeController;
import bisq.desktop.main.content.authorized_role.release_manager.ReleaseManagerController;
import bisq.desktop.main.content.authorized_role.security_manager.SecurityManagerController;
import bisq.desktop.main.content.authorized_role.seed_node.SeedNodeController;
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
    private Pin bondedRoleSetPin, selectedUserIdentityPin;

    public AuthorizedRoleController(ServiceProvider serviceProvider) {
        super(new AuthorizedRoleModel(List.of(BondedRoleType.values())), NavigationTarget.AUTHORIZED_ROLE);

        this.serviceProvider = serviceProvider;
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        view = new AuthorizedRoleView(model, this);

        updateAuthorizedBondedRoles();
    }

    @Override
    public void onActivate() {
        bondedRoleSetPin = authorizedBondedRolesService.getAuthorizedBondedRoleSet().addListener(this::updateAuthorizedBondedRoles);
        selectedUserIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(e -> updateAuthorizedBondedRoles());
    }

    @Override
    public void onDeactivate() {
        bondedRoleSetPin.unbind();
        selectedUserIdentityPin.unbind();
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case MEDIATOR: {
                return Optional.of(new MediatorController(serviceProvider));
            }
           /* case ARBITRATOR: {
                return Optional.of(new SecurityManagerController(serviceProvider)); //todo
            }*/
            case MODERATOR: {
                return Optional.of(new ModeratorController(serviceProvider));
            }
            case SECURITY_MANAGER: {
                return Optional.of(new SecurityManagerController(serviceProvider));
            }
            case RELEASE_MANAGER: {
                return Optional.of(new ReleaseManagerController(serviceProvider));
            }
            case SEED_NODE: {
                return Optional.of(new SeedNodeController(serviceProvider));
            }
            case ORACLE_NODE: {
                return Optional.of(new OracleNodeController(serviceProvider));
            }
            case EXPLORER_NODE: {
                return Optional.of(new ExplorerNodeController(serviceProvider));
            }
            case MARKET_PRICE_NODE: {
                return Optional.of(new MarketPriceNodeController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    private void updateAuthorizedBondedRoles() {
        UIThread.run(() -> {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
            model.getAuthorizedBondedRoles().setAll(authorizedBondedRolesService.getAuthorizedBondedRoleSet().stream()
                    .filter(bondedRole -> selectedUserIdentity != null)
                    .filter(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()))
                    .map(AuthorizedBondedRole::getBondedRoleType)
                    .collect(Collectors.toSet()));
        });
    }
}
