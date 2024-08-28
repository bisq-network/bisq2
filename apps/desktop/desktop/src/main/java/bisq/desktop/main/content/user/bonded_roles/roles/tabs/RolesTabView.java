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

package bisq.desktop.main.content.user.bonded_roles.roles.tabs;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.Styles;
import bisq.desktop.main.content.user.bonded_roles.tabs.BondedRolesTabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RolesTabView extends BondedRolesTabView<RolesTabModel, RolesTabController> {
    public RolesTabView(RolesTabModel model, RolesTabController controller) {
        super(model, controller);
    }

    @Override
    protected void addTabs() {
        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("user.bondedRoles.type.MEDIATOR"),
                NavigationTarget.REGISTER_MEDIATOR,
                styles);
        addTab(Res.get("user.bondedRoles.type.MODERATOR"),
                NavigationTarget.REGISTER_MODERATOR,
                styles);
        addTab(Res.get("user.bondedRoles.type.SECURITY_MANAGER"),
                NavigationTarget.REGISTER_SECURITY_MANAGER,
                styles);
        addTab(Res.get("user.bondedRoles.type.RELEASE_MANAGER"),
                NavigationTarget.REGISTER_RELEASE_MANAGER,
                styles);
    }

    @Override
    protected String getHeadline() {
        return Res.get("user.bondedRoles.headline.roles");
    }
}
