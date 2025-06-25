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

import bisq.account.accounts.SepaAccountPayload;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class SepaPaymentFormController extends PaymentFormController<SepaPaymentFormView, SepaPaymentFormModel, SepaAccountPayload> {
    public SepaPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected SepaPaymentFormView createView() {
        return new SepaPaymentFormView(model, this);
    }

    @Override
    protected SepaPaymentFormModel createModel() {
        List<Country> allSepaCountries = FiatPaymentRailUtil.getAllSepaCountries().stream()
                .map(CountryRepository::getCountry)
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toList());
        List<Country> allEuroCountries = FiatPaymentRailUtil.getSepaEuroCountries().stream()
                .map(CountryRepository::getCountry)
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toList());
        List<Country> allNonEuroCountries = FiatPaymentRailUtil.getSepaNonEuroCountries().stream()
                .map(CountryRepository::getCountry)
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toList());
        return new SepaPaymentFormModel(UUID.randomUUID().toString(),
                allSepaCountries,
                allEuroCountries,
                allNonEuroCountries);
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
        model.getCountryErrorVisible().set(false);
        model.getAcceptedCountriesErrorVisible().set(false);

        Country defaultCountry = CountryRepository.getDefaultCountry();
        if (model.getSelectedCountryOfBank().get() == null &&
                model.getAllSepaCountries().contains(defaultCountry)) {
            model.getSelectedCountryOfBank().set(defaultCountry);
            model.getSepaIbanValidator().setRestrictedToCountryCode(defaultCountry.getCode());
        }
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public SepaAccountPayload getAccountPayload() {
        List<Country> acceptedCountries = getAcceptedCountries();
        List<String> acceptedCountryCodes = acceptedCountries.stream()
                .map(Country::getCode)
                .collect(Collectors.toList());
        return new SepaAccountPayload(model.getId(),
                FiatPaymentRail.SEPA.name(),
                model.getHolderName().get(),
                model.getIban().get(),
                model.getBic().get(),
                model.getSelectedCountryOfBank().get().getCode(),
                acceptedCountryCodes);
    }

    @Override
    public boolean validate() {
        boolean isCountrySet = model.getSelectedCountryOfBank().get() != null;
        model.getCountryErrorVisible().set(!isCountrySet);

        boolean acceptedCountriesValid = !getAcceptedCountries().isEmpty();
        model.getAcceptedCountriesErrorVisible().set(!acceptedCountriesValid);

        boolean holderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean ibanValid = model.getSepaIbanValidator().validateAndGet();
        boolean bicValid = model.getSepaBicValidator().validateAndGet();

        boolean isValid = isCountrySet &&
                acceptedCountriesValid &&
                holderNameValid &&
                ibanValid &&
                bicValid;
        model.getRequireValidation().set(true);
        return isValid;
    }

    void onValidationDone() {
        model.getRequireValidation().set(false);
    }

    void onCountryOfBankSelected(Country selectedCountry) {
        model.getSelectedCountryOfBank().set(selectedCountry);
        model.getCountryErrorVisible().set(false);
        model.getSepaIbanValidator().setRestrictedToCountryCode(selectedCountry.getCode());
    }

    void onSelectAcceptedCountry(Country country, boolean selected, boolean isEuroCountry) {
        if (isEuroCountry) {
            if (selected) {
                model.getAcceptedEuroCountries().add(country);
            } else {
                model.getAcceptedEuroCountries().remove(country);
            }
        } else {
            if (selected) {
                model.getAcceptedNonEuroCountries().add(country);

            } else {
                model.getAcceptedNonEuroCountries().remove(country);
            }
        }
        model.getAcceptedCountriesErrorVisible().set(getAcceptedCountries().isEmpty());
    }

    private ArrayList<Country> getAcceptedCountries() {
        ArrayList<Country> acceptedCountries = new ArrayList<>(model.getAcceptedEuroCountries());
        acceptedCountries.addAll(model.getAcceptedNonEuroCountries());
        return acceptedCountries;
    }
}