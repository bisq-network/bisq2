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
import bisq.common.asset.Asset;
import bisq.common.asset.FiatCurrency;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

import java.util.stream.Collectors;

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
        model.getSelectedCurrenciesErrorVisible().set(false);
    }

    @Override
    public boolean validate() {
        boolean selectedCurrenciesValid = !model.getSelectedCurrencies().isEmpty();
        model.getSelectedCurrenciesErrorVisible().set(!selectedCurrenciesValid);
        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean mobileValid = model.getMobileNrValidator().validateAndGet();
        model.getRunValidation().set(true);
        return selectedCurrenciesValid && holderNameValid && mobileValid;
    }

    @Override
    public MoneseAccountPayload createAccountPayload() {
        return new MoneseAccountPayload(model.getId(),
                model.getSelectedCurrencies().stream()
                        .map(Asset::getCode)
                        .collect(Collectors.toList()),
                model.getHolderName().get(),
                model.getMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCurrency(FiatCurrency currency, boolean selected) {
        if (selected) {
            model.getSelectedCurrencies().add(currency);
        } else {
            model.getSelectedCurrencies().remove(currency);
        }
        model.getSelectedCurrenciesErrorVisible().set(model.getSelectedCurrencies().isEmpty());
    }
}
