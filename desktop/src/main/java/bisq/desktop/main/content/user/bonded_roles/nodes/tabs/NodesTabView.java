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

package bisq.desktop.main.content.user.bonded_roles.nodes.tabs;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.Styles;
import bisq.desktop.main.content.user.bonded_roles.tabs.BondedRolesTabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodesTabView extends BondedRolesTabView<NodesTabModel, NodesTabController> {
    public NodesTabView(NodesTabModel model, NodesTabController controller) {
        super(model, controller);
    }

    @Override
    protected void addTabs() {
        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("user.bondedRoles.type.SEED_NODE"),
                NavigationTarget.REGISTER_SEED_NODE,
                styles);
        addTab(Res.get("user.bondedRoles.type.ORACLE_NODE"),
                NavigationTarget.REGISTER_ORACLE_NODE,
                styles);
        addTab(Res.get("user.bondedRoles.type.EXPLORER_NODE"),
                NavigationTarget.REGISTER_EXPLORER_NODE,
                styles);
        addTab(Res.get("user.bondedRoles.type.MARKET_PRICE_NODE"),
                NavigationTarget.REGISTER_MARKET_PRICE_NODE,
                styles);
    }

    @Override
    protected String getHeadline() {
        return Res.get("user.bondedRoles.headline.nodes");
    }
}
