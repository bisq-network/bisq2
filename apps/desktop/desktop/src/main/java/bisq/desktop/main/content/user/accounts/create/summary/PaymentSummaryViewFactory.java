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

package bisq.desktop.main.content.user.accounts.create.summary;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.View;
import javafx.scene.Parent;

import java.util.Optional;

public class PaymentSummaryViewFactory {
    public static View<? extends Parent, PaymentSummaryModel, ? extends Controller> createView(PaymentSummaryModel model,
                                                                                               PaymentSummaryController controller) {
        Optional<PaymentMethod<?>> paymentMethodOpt = model.getPaymentMethod();

        if (paymentMethodOpt.isEmpty()) {
            throw new IllegalArgumentException("Cannot create view without a payment method");
        }

        return new PaymentSummaryView(model, controller);
    }
}