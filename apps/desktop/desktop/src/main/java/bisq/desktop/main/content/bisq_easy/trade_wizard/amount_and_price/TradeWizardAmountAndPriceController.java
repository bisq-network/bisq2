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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount_and_price;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeWizardAmountAndPriceController implements Controller {

    private final TradeWizardAmountAndPriceModel model;
    @Getter
    private final TradeWizardAmountAndPriceView view;
    private final Region owner;

    public TradeWizardAmountAndPriceController(ServiceProvider serviceProvider, Region owner) {
        this.owner = owner;
        model = new TradeWizardAmountAndPriceModel();
        view = new TradeWizardAmountAndPriceView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void reset() {
        model.reset();
    }
}
