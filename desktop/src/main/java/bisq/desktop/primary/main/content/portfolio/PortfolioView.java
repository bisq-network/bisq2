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

package bisq.desktop.primary.main.content.portfolio;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortfolioView extends TabView<PortfolioModel, PortfolioController> {

    public PortfolioView(PortfolioModel model, PortfolioController controller) {
        super(model, controller);

        addTab(Res.get("portfolio.openOffers"), NavigationTarget.OPEN_OFFERS);
        addTab(Res.get("portfolio.pending"), NavigationTarget.PENDING_TRADES);
        addTab(Res.get("portfolio.closed"), NavigationTarget.CLOSED_TRADES);
   
        headlineLabel.setText(Res.get("portfolio"));
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
