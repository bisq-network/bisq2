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

package bisq.desktop.main.content.user.bonded_roles.roles;

import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.user.bonded_roles.BondedRolesController;
import bisq.desktop.main.content.user.bonded_roles.BondedRolesListItem;
import bisq.desktop.main.content.user.bonded_roles.BondedRolesModel;
import bisq.desktop.main.content.user.bonded_roles.BondedRolesView;
import bisq.desktop.main.content.user.bonded_roles.roles.tabs.RolesTabController;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
public class RolesController extends BondedRolesController {

    public RolesController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected BondedRolesModel createAndGetModel() {
        return new RolesModel();
    }

    @Override
    protected BondedRolesView<? extends BondedRolesModel, ? extends BondedRolesController> createAndGetView() {
        return new RolesView((RolesModel) model, this, new RolesTabController(serviceProvider).getView().getRoot());
    }

    @Override
    protected Predicate<? super BondedRolesListItem> getPredicate() {
        return (Predicate<BondedRolesListItem>) bondedRoleListItem -> bondedRoleListItem.getBondedRoleType().isRole();
    }
}
