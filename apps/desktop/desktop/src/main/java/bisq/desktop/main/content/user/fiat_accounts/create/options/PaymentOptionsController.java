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

package bisq.desktop.main.content.user.fiat_accounts.create.options;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * STUB IMPLEMENTATION - Options step controller
 */
@Slf4j
public class PaymentOptionsController implements Controller {
    private final PaymentOptionsModel model;
    @Getter
    private final PaymentOptionsView view;

    public PaymentOptionsController(ServiceProvider serviceProvider) {
        model = new PaymentOptionsModel();
        view = new PaymentOptionsView(model, this);
        log.debug("CreateAccountOptionsController initialized (stub implementation)");
    }

    @Override
    public void onActivate() {
        log.debug("CreateAccountOptionsController activated");
    }

    @Override
    public void onDeactivate() {
        log.debug("CreateAccountOptionsController deactivated");
    }

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        model.setPaymentMethod(paymentMethod);
        log.debug("Payment method set: {}", paymentMethod != null ? paymentMethod.getDisplayString() : "null");
    }

    public boolean validate() {
        // TODO: Implement validation logic for payment options
        log.debug("Validating options (stub - always valid)");
        return true;
    }
}