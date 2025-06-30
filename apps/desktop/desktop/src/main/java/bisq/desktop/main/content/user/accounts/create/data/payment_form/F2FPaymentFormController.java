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

package bisq.desktop.main.content.user.accounts.create.data.payment_form;

import bisq.account.accounts.F2FAccountPayload;
import bisq.common.currency.FiatCurrency;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class F2FPaymentFormController extends PaymentFormController<F2FPaymentFormView, F2FPaymentFormModel, F2FAccountPayload> {
    public F2FPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected F2FPaymentFormView createView() {
        return new F2FPaymentFormView(model, this);
    }

    @Override
    protected F2FPaymentFormModel createModel() {
        return new F2FPaymentFormModel(UUID.randomUUID().toString(),
                CountryRepository.getAllCountries(),
                FiatCurrencyRepository.getAllCurrencies());
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
        model.getCountryErrorVisible().set(false);
        model.getCurrencyErrorVisible().set(false);
    }

    @Override
    public void onDeactivate() {
    }

    void onValidationDone() {
        model.getRequireValidation().set(false);
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
        model.getRequireValidation().set(true);
        return isValid;
    }

    @Override
    public F2FAccountPayload getAccountPayload() {
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