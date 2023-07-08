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

package bisq.desktop.main.content.user.bonded_roles.tabs;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BondedRolesTabController<M extends BondedRolesTabModel> extends TabController<M> {
    @Getter
    protected final BondedRolesTabView<? extends M, ? extends BondedRolesTabController<M>> view;
    protected final ServiceProvider serviceProvider;

    public BondedRolesTabController(M model, NavigationTarget navigationTarget, ServiceProvider serviceProvider) {
        super(model, navigationTarget);

        this.serviceProvider = serviceProvider;
        view = createAndGetView();
    }

    protected abstract BondedRolesTabView<? extends M, ? extends BondedRolesTabController<M>> createAndGetView();

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}
