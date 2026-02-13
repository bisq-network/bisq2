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

import bisq.account.accounts.fiat.MoneyBeamAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MoneyBeamFormController extends FormController<MoneyBeamFormView, MoneyBeamFormModel, MoneyBeamAccountPayload> {
    public MoneyBeamFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected MoneyBeamFormView createView() {
        return new MoneyBeamFormView(model, this);
    }

    @Override
    protected MoneyBeamFormModel createModel() {
        List<Country> countries = FiatPaymentRailUtil.getAllSepaCountries().stream()
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toList());
        List<FiatCurrency> currencies = FiatPaymentRailUtil.getMoneyBeamCurrencies().stream()
                .sorted(Comparator.comparing(FiatCurrency::getName))
                .collect(Collectors.toList());
        return new MoneyBeamFormModel(StringUtils.createUid(), countries, currencies);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCountryErrorVisible().set(false);
        model.getCurrencyErrorVisible().set(false);

        Country defaultCountry = CountryRepository.getDefaultCountry();
        if (model.getSelectedCountry().get() == null && model.getCountries().contains(defaultCountry)) {
            model.getSelectedCountry().set(defaultCountry);
        }
        if (model.getSelectedCurrency().get() == null && !model.getCurrencies().isEmpty()) {
            model.getSelectedCurrency().set(model.getCurrencies().get(0));
        }
    }

    @Override
    public boolean validate() {
        boolean isCountrySet = model.getSelectedCountry().get() != null;
        model.getCountryErrorVisible().set(!isCountrySet);

        boolean isCurrencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!isCurrencySet);

        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean emailOrMobileValid = model.getEmailOrMobileNrValidator().validateAndGet();

        boolean isValid = isCountrySet && isCurrencySet && holderNameValid && emailOrMobileValid;
        model.getRunValidation().set(true);
        return isValid;
    }

    @Override
    public MoneyBeamAccountPayload createAccountPayload() {
        return new MoneyBeamAccountPayload(model.getId(),
                model.getSelectedCountry().get().getCode(),
                model.getSelectedCurrency().get().getCode(),
                model.getHolderName().get(),
                model.getEmailOrMobileNr().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCountry(Country selectedCountry) {
        model.getSelectedCountry().set(selectedCountry);
        model.getCountryErrorVisible().set(false);
    }

    void onSelectCurrency(FiatCurrency selectedCurrency) {
        model.getSelectedCurrency().set(selectedCurrency);
        model.getCurrencyErrorVisible().set(false);
    }
}
