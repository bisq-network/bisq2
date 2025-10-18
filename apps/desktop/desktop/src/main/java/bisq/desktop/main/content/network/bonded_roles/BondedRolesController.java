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

package bisq.desktop.main.content.network.bonded_roles;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.network.bonded_roles.tabs.BondedRolesTabController;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.network.NetworkService;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.function.Predicate;

@Slf4j
public abstract class BondedRolesController implements Controller {
    @Getter
    protected final BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> view;
    protected final BondedRolesModel model;
    protected final AuthorizedBondedRolesService authorizedBondedRolesService;
    protected final ServiceProvider serviceProvider;
    protected final UserService userService;
    private final NetworkService networkService;
    protected final BondedRolesTabController<?> bondedRolesTabController;
    protected Pin bondedRolesPin;
    private Subscription selectedTabButtonPin;
    private Subscription selectedBondedRoleTypePin;

    public BondedRolesController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        userService = serviceProvider.getUserService();
        networkService = serviceProvider.getNetworkService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();

        bondedRolesTabController = createAndGetNodesTabController();
        model = createAndGetModel();
        view = createAndGetView();
    }


    @Override
    public void onActivate() {
        bondedRolesPin = FxBindings.<BondedRole, BondedRolesListItem>bind(model.getBondedRolesListItems())
                .map(data -> new BondedRolesListItem(data, userService, networkService))
                .to(authorizedBondedRolesService.getBondedRoles());

        selectedTabButtonPin = EasyBind.subscribe(bondedRolesTabController.getModel().getSelectedTabButton(),
                selectedTabButton -> {
                    if (selectedTabButton != null) {
                        handleNavigationTargetChange(selectedTabButton.getNavigationTarget());
                    }
                });

        selectedBondedRoleTypePin = EasyBind.subscribe(model.getSelectedBondedRoleType(),
                selectedBondedRoleType -> model.getFilteredList().setPredicate(getPredicate()));
    }

    @Override
    public void onDeactivate() {
        bondedRolesPin.unbind();
        selectedBondedRoleTypePin.unsubscribe();
        selectedTabButtonPin.unsubscribe();
    }

    void onCopyPublicKeyAsHex(String publicKeyAsHex) {
        ClipboardUtil.copyToClipboard(publicKeyAsHex);
    }

    void onOpenProfileCard(UserProfile userProfile) {
        Navigation.navigateTo(NavigationTarget.PROFILE_CARD, new ProfileCardController.InitData(userProfile));
    }

    protected abstract BondedRolesTabController<?> createAndGetNodesTabController();

    protected abstract BondedRolesModel createAndGetModel();

    protected abstract BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> createAndGetView();

    protected abstract void handleNavigationTargetChange(NavigationTarget navigationTarget);

    protected Predicate<? super BondedRolesListItem> getPredicate() {
        return (Predicate<BondedRolesListItem>) bondedRoleListItem ->
        {
            BondedRoleType selected = model.getSelectedBondedRoleType().get();
            return selected == null || bondedRoleListItem.getBondedRoleType() == selected;
        };
    }
}
