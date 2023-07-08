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

import bisq.bonded_roles.AuthorizedBondedRole;
import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
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
    protected Pin bondedRoleDataPin;

    public BondedRolesController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        userService = serviceProvider.getUserService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();

        model = createAndGetModel();
        view = createAndGetView();
    }

    protected abstract BondedRolesModel createAndGetModel();

    protected abstract BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> createAndGetView();

    protected abstract Predicate<? super BondedRolesListItem> getPredicate();

    @Override
    public void onActivate() {
        bondedRoleDataPin = FxBindings.<AuthorizedBondedRole, BondedRolesListItem>bind(model.getBondedRolesListItems())
                .map(data -> new BondedRolesListItem(data, userService))
                .to(authorizedBondedRolesService.getAuthorizedBondedRoleSet());

        model.getFilteredList().setPredicate(getPredicate());
    }

    @Override
    public void onDeactivate() {
        bondedRoleDataPin.unbind();
    }

    public void onCopyPublicKeyAsHex(String publicKeyAsHex) {
        ClipboardUtil.copyToClipboard(publicKeyAsHex);
    }
}
