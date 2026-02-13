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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form;

import bisq.account.accounts.fiat.PayseraAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class PayseraFormController extends FormController<PayseraFormView, PayseraFormModel, PayseraAccountPayload> {
    public PayseraFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected PayseraFormView createView() {
        return new PayseraFormView(model, this);
    }

    @Override
    protected PayseraFormModel createModel() {
        return new PayseraFormModel(StringUtils.createUid(), FiatPaymentRailUtil.getPayseraCurrencies());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCurrencyErrorVisible().set(false);
    }

    @Override
    public boolean validate() {
        boolean currencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!currencySet);
        boolean emailValid = model.getEmailValidator().validateAndGet();
        model.getRunValidation().set(true);
        return currencySet && emailValid;
    }

    @Override
    public PayseraAccountPayload createAccountPayload() {
        return new PayseraAccountPayload(model.getId(),
                model.getSelectedCurrency().get().getCode(),
                model.getEmail().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency() {
        model.getCurrencyErrorVisible().set(false);
    }
}
