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

import bisq.account.accounts.fiat.MoneyGramAccountPayload;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MoneyGramFormController extends FormController<MoneyGramFormView, MoneyGramFormModel, MoneyGramAccountPayload> {
    private Subscription selectedCountryPin;

    public MoneyGramFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected MoneyGramFormView createView() {
        return new MoneyGramFormView(model, this);
    }

    @Override
    protected MoneyGramFormModel createModel() {
        List<Country> countries = FiatPaymentRailUtil.getMoneyGramCountries().stream()
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toList());
        List<FiatCurrency> currencies = FiatPaymentRailUtil.getMoneyGramCurrencies().stream()
                .sorted(Comparator.comparing(FiatCurrency::getName))
                .collect(Collectors.toList());
        return new MoneyGramFormModel(StringUtils.createUid(), countries, currencies);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getCountryErrorVisible().set(false);
        model.getSelectedCurrenciesErrorVisible().set(false);

        Country defaultCountry = CountryRepository.getDefaultCountry();
        if (model.getSelectedCountry().get() == null && model.getCountries().contains(defaultCountry)) {
            model.getSelectedCountry().set(defaultCountry);
        }

        selectedCountryPin = EasyBind.subscribe(model.getSelectedCountry(),
                country -> {
                    if (country != null) {
                        model.getStateVisible().set(AccountUtils.isStateRequired(country.getCode()));
                    }
                });
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        selectedCountryPin.unsubscribe();
    }

    @Override
    public boolean validate() {
        boolean isCountrySet = model.getSelectedCountry().get() != null;
        model.getCountryErrorVisible().set(!isCountrySet);

        boolean hasSelectedCurrencies = !model.getSelectedCurrencies().isEmpty();
        model.getSelectedCurrenciesErrorVisible().set(!hasSelectedCurrencies);

        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean emailValid = model.getEmailValidator().validateAndGet();
        boolean stateValid = model.getStateValidator().validateAndGet();

        boolean isValid = isCountrySet && hasSelectedCurrencies && holderNameValid && emailValid && stateValid;
        model.getRunValidation().set(true);
        return isValid;
    }

    @Override
    public MoneyGramAccountPayload createAccountPayload() {
        List<String> selectedCurrencyCodes = model.getSelectedCurrencies().stream()
                .map(FiatCurrency::getCode)
                .collect(Collectors.toList());
        return new MoneyGramAccountPayload(model.getId(),
                model.getSelectedCountry().get().getCode(),
                selectedCurrencyCodes,
                model.getHolderName().get(),
                model.getEmail().get(),
                model.getState().get());
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectCountry(Country selectedCountry) {
        model.getSelectedCountry().set(selectedCountry);
        model.getCountryErrorVisible().set(false);
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
