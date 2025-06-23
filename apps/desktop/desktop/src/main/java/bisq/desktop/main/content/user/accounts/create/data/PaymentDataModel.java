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

package bisq.desktop.main.content.user.accounts.create.data;

import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.view.Model;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class PaymentDataModel implements Model {
    @Getter
    private final ObjectProperty<PaymentMethod<?>> paymentMethod = new SimpleObjectProperty<>();
    @Getter
    private final ObjectProperty<Map<String, Object>> accountData = new SimpleObjectProperty<>(new HashMap<>());

    public ObjectProperty<PaymentMethod<?>> paymentMethodProperty() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod<?> paymentMethod) {
        this.paymentMethod.set(paymentMethod);
    }
}