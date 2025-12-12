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

package bisq.account.accounts;

import bisq.account.payment_method.PaymentMethod;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public interface SingleCurrencyAccountPayload {
    PaymentMethod<?> getPaymentMethod();

    default String getCurrencyCode() {
        List<String> supportedCurrencyCodes = getPaymentMethod().getSupportedCurrencyCodes();
        checkArgument(supportedCurrencyCodes.size() == 1,
                "SingleCurrencyAccountPayload must have exactly 1 currency code in supportedCurrencyCodes");
        return supportedCurrencyCodes.get(0);
    }
}
