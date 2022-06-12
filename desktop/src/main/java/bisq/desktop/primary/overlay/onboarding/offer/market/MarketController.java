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

package bisq.desktop.primary.overlay.onboarding.offer.market;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.common.view.Controller;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MarketController implements Controller {
    private final MarketModel model;
    @Getter
    private final MarketView view;

    public MarketController(DefaultApplicationService applicationService) {
        model = new MarketModel();
        view = new MarketView(model, this);
        model.getMarket().set(MarketRepository.getDefault());
       
       // model.getMarket().set( MarketRepository.getBsqMarket());
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getMarket();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(MarketListItem item) {

    }
}
