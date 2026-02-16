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

import bisq.account.accounts.fiat.WiseAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WiseFormController extends FormController<WiseFormView, WiseFormModel, WiseAccountPayload> {
    public WiseFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected WiseFormView createView() {
        return new WiseFormView(model, this);
    }

    @Override
    protected WiseFormModel createModel() {
        List<Country> countries = FiatPaymentRailUtil.getWiseCountries().stream()
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toList());
        List<FiatCurrency> currencies = FiatPaymentRailUtil.getWiseCurrencies().stream()
                .sorted(Comparator.comparing(FiatCurrency::getName))
                .collect(Collectors.toList());
        return new WiseFormModel(StringUtils.createUid(), countries, currencies);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getSelectedCurrenciesErrorVisible().set(false);
    }

    @Override
    public boolean validate() {
        boolean hasSelectedCurrencies = !model.getSelectedCurrencies().isEmpty();
        model.getSelectedCurrenciesErrorVisible().set(!hasSelectedCurrencies);

        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean emailValid = model.getEmailValidator().validateAndGet();

        boolean isValid = hasSelectedCurrencies && holderNameValid && emailValid;
        model.getRunValidation().set(true);
        return isValid;
    }

    @Override
    public WiseAccountPayload createAccountPayload() {
        List<String> selectedCurrencyCodes = model.getSelectedCurrencies().stream()
                .map(FiatCurrency::getCode)
                .collect(Collectors.toList());
        return new WiseAccountPayload(model.getId(),
                selectedCurrencyCodes,
                model.getHolderName().get(),
                model.getEmail().get());
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
