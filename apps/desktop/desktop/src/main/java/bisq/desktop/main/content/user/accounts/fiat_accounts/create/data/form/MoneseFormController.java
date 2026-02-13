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

import bisq.account.accounts.fiat.MoneseAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

public class MoneseFormController extends FormController<MoneseFormView, MoneseFormModel, MoneseAccountPayload> {
    public MoneseFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected MoneseFormView createView() {
        return new MoneseFormView(model, this);
    }

    @Override
    protected MoneseFormModel createModel() {
        return new MoneseFormModel(StringUtils.createUid(), FiatPaymentRailUtil.getMoneseCurrencies());
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
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean mobileValid = model.getMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return currencySet && holderNameValid && mobileValid;
    }

    @Override
    public MoneseAccountPayload createAccountPayload() {
        return new MoneseAccountPayload(model.getId(),
                model.getSelectedCurrency().get().getCode(),
                model.getHolderName().get(),
                model.getMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency() {
        model.getCurrencyErrorVisible().set(false);
    }
}
