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

package bisq.satoshisquareapp.primary.main.top;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.top.components.MarketPriceComponent;
import lombok.Getter;

public class TopPanelController implements Controller {
    @Getter
    private final TopPanelView view;
    private final TopPanelModel model;

    public TopPanelController(DefaultApplicationService applicationService) {
        model = new TopPanelModel();
        view = new TopPanelView(model, this, new MarketPriceComponent(applicationService.getMarketPriceService()).getRootPane());
    }

    public void setMarketPriceBoxVisible(boolean value) {
        model.getMarketPriceBoxVisible().set(value);
    }
    
    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}
