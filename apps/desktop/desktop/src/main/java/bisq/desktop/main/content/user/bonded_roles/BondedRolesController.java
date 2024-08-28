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

package bisq.desktop.main.content.user.bonded_roles;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.network.NetworkService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
    protected Pin bondedRolesPin;

    public BondedRolesController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        userService = serviceProvider.getUserService();
        networkService = serviceProvider.getNetworkService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();

        model = createAndGetModel();
        view = createAndGetView();
    }

    protected abstract BondedRolesModel createAndGetModel();

    protected abstract BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> createAndGetView();

    protected abstract Predicate<? super BondedRolesListItem> getPredicate();

    @Override
    public void onActivate() {
        bondedRolesPin = FxBindings.<BondedRole, BondedRolesListItem>bind(model.getBondedRolesListItems())
                .map(data -> new BondedRolesListItem(data, userService, networkService))
                .to(authorizedBondedRolesService.getBondedRoles());

        model.getFilteredList().setPredicate(getPredicate());
    }

    @Override
    public void onDeactivate() {
        bondedRolesPin.unbind();
    }

    void onCopyPublicKeyAsHex(String publicKeyAsHex) {
        ClipboardUtil.copyToClipboard(publicKeyAsHex);
    }

    void applySearchPredicate(String searchText) {
        String string = searchText.toLowerCase();
        model.getFilteredList().setPredicate(item ->
                StringUtils.isEmpty(string) ||
                        item.getUserName().toLowerCase().contains(string) ||
                        item.getUserProfileId().contains(string) ||
                        item.getBondUserName().contains(string) ||
                        item.getAddress().toLowerCase().contains(string) ||
                        item.getRoleTypeString().toLowerCase().contains(string));
    }
}
