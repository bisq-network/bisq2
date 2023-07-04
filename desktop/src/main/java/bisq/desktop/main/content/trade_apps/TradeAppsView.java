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

package bisq.desktop.main.content.trade_apps;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeAppsView extends TabView<TradeAppsModel, TradeAppsController> {

    public TradeAppsView(TradeAppsModel model, TradeAppsController controller) {
        super(model, controller);

        headLine.setText(Res.get("tradeApps.headline"));

        addTab(Res.get("tradeApps.gridView"), NavigationTarget.TRADE_OVERVIEW_GRID, "nav-grid");
        addTab(Res.get("tradeApps.listView"), NavigationTarget.TRADE_OVERVIEW_LIST, "nav-list");
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
