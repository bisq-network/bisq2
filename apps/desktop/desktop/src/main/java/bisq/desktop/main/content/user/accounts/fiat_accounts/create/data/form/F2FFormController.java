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

import bisq.account.accounts.fiat.F2FAccountPayload;
import bisq.common.asset.FiatCurrency;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class F2FFormController extends FormController<F2FFormView, F2FFormModel, F2FAccountPayload> {
    public F2FFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected F2FFormView createView() {
        return new F2FFormView(model, this);
    }

    @Override
    protected F2FFormModel createModel() {
        return new F2FFormModel(StringUtils.createUid(),
                CountryRepository.getAllCountries(),
                FiatCurrencyRepository.getAllCurrencies());
    }

    @Override
    public void onActivate() {
        model.getRunValidation().set(false);
        model.getCountryErrorVisible().set(false);
        model.getCurrencyErrorVisible().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    @Override
    public boolean validate() {
        boolean isCountrySet = model.getSelectedCountry().get() != null;
        model.getCountryErrorVisible().set(!isCountrySet);
        boolean isCurrencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!isCurrencySet);
        boolean isValid = isCountrySet &&
                isCurrencySet &&
                model.getCityValidator().validateAndGet() &&
                model.getContactValidator().validateAndGet() &&
                model.getExtraInfoValidator().validateAndGet();
        model.getRunValidation().set(true);
        return isValid;
    }

    @Override
    public F2FAccountPayload createAccountPayload() {
        return new F2FAccountPayload(model.getId(),
                model.getSelectedCountry().get().getCode(),
                model.getSelectedCurrency().get().getCode(),
                model.getCity().get(),
                model.getContact().get(),
                model.getExtraInfo().get());
    }

    void onSelectCountry(Country selectedCountry) {
        model.getSelectedCountry().set(selectedCountry);
        model.getCountryErrorVisible().set(false);
        model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(selectedCountry.getCode()));
        checkCurrencyCountryMatch();
    }

    void onSelectCurrency(FiatCurrency selectedCurrency) {
        model.getSelectedCurrency().set(selectedCurrency);
        model.getCurrencyErrorVisible().set(false);
        checkCurrencyCountryMatch();
    }

    void onCurrencyCountryMisMatchPopupClosed(boolean applyMatchingCurrency) {
        model.getCurrencyCountryMismatch().set(false);
        if (applyMatchingCurrency) {
            model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(model.getSelectedCountry().get().getCode()));
        }
    }

    private void checkCurrencyCountryMatch() {
        if (model.getSelectedCountry().get() != null &&
                model.getSelectedCurrency().get() != null &&
                !FiatCurrencyRepository.getCurrencyByCountryCode(model.getSelectedCountry().get().getCode()).equals(model.getSelectedCurrency().get())) {
            model.getCurrencyCountryMismatch().set(true);
        } else {
            model.getCurrencyCountryMismatch().set(false);
        }
    }
}