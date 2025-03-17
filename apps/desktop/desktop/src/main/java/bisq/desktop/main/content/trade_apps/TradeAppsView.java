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

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeAppsView extends ContentTabView<TradeAppsModel, TradeAppsController> {
    private final TabButton more;

    public TradeAppsView(TradeAppsModel model, TradeAppsController controller) {
        super(model, controller);

        addTab(Res.get("tradeApps.compare"), NavigationTarget.TRADE_PROTOCOLS_OVERVIEW);
        addTab(Res.get("tradeApps.bisqMuSig"), NavigationTarget.BISQ_MU_SIG);
        addTab(Res.get("tradeApps.subMarine"), NavigationTarget.SUBMARINE);
        addTab(Res.get("tradeApps.bisqLightning"), NavigationTarget.BISQ_LIGHTNING);
        more = addTab(Res.get("tradeApps.more"), NavigationTarget.MORE_TRADE_PROTOCOLS);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        more.visibleProperty().bind(model.getMoreTabVisible());
        more.managedProperty().bind(model.getMoreTabVisible());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        more.visibleProperty().unbind();
        more.managedProperty().unbind();
    }
}
