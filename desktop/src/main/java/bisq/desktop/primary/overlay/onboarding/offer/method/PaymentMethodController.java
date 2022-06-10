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

package bisq.desktop.primary.overlay.onboarding.offer.method;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.Controller;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentMethodController implements Controller {
    private final PaymentMethodModel model;
    @Getter
    private final PaymentMethodView view;

    public PaymentMethodController(DefaultApplicationService applicationService) {
        model = new PaymentMethodModel();
        view = new PaymentMethodView(model, this);
        model.getPaymentMethods().add("SEPA");
        model.getPaymentMethods().add("REVOLUT");
    }

    public ObservableList<String> getPaymentMethods() {
        return model.getPaymentMethods();
    }

    public void setMarket(Market market) {
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }
}
