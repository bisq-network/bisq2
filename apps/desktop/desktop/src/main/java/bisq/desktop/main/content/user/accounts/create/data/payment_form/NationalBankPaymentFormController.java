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

import bisq.account.accounts.BankAccountType;
import bisq.account.accounts.BankAccountUtils;
import bisq.account.accounts.NationalBankAccountPayload;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.currency.FiatCurrency;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class NationalBankPaymentFormController extends PaymentFormController<NationalBankPaymentFormView, NationalBankPaymentFormModel, NationalBankAccountPayload> {
    public NationalBankPaymentFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected NationalBankPaymentFormView createView() {
        return new NationalBankPaymentFormView(model, this);
    }

    @Override
    protected NationalBankPaymentFormModel createModel() {
        List<String> nationBankAccountCountryCodes = FiatPaymentRailUtil.getNationalBankAccountCountries();
        List<Country> nationBankAccountCountries = CountryRepository.getCountriesFromCodes(nationBankAccountCountryCodes);
        List<FiatCurrency> nationBankAccountCurrencies = nationBankAccountCountryCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCountryCode)
                .collect(Collectors.toList());
        return new NationalBankPaymentFormModel(UUID.randomUUID().toString(),
                nationBankAccountCountries,
                nationBankAccountCurrencies,
                List.of(BankAccountType.CHECKINGS, BankAccountType.SAVINGS));
    }

    @Override
    public void onActivate() {
        model.getRequireValidation().set(false);
        model.getCountryErrorVisible().set(false);
        model.getCurrencyErrorVisible().set(false);
        model.getBankAccountTypeErrorVisible().set(false);

        Country selectedCountry = model.getSelectedCountry().get();
        applySelectCountry(selectedCountry);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public boolean validate() {
        Country selectedCountry = model.getSelectedCountry().get();
        boolean isCountrySet = selectedCountry != null;
        model.getCountryErrorVisible().set(!isCountrySet);
        if (!isCountrySet) {
            model.getRequireValidation().set(true);
            return false;
        }
        boolean isAccountNrValid = model.getAccountNrValidator().validateAndGet();
        if (!isAccountNrValid) {
            model.getRequireValidation().set(true);
            return false;
        }

        String countryCode = selectedCountry.getCode();
        if (!model.getUseValidation().get()) {
            model.getRequireValidation().set(true);
            return true;
        }

        boolean isCurrencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!isCurrencySet);

        boolean isHolderNameValid = model.getHolderNameValidator().validateAndGet();
        boolean isHolderIdValid = !BankAccountUtils.isHolderIdRequired(countryCode) || model.getHolderIdValidator().validateAndGet();

        boolean isBankNameValid = !BankAccountUtils.isBankNameRequired(countryCode) || model.getBankNameValidator().validateAndGet();
        boolean isBankIdValid = !BankAccountUtils.isBankIdRequired(countryCode) || model.getBankIdValidator().validateAndGet();
        boolean isBranchIdValid = !BankAccountUtils.isBranchIdRequired(countryCode) || model.getBranchIdValidator().validateAndGet();

        boolean bankAccountTypeRequired = BankAccountUtils.isBankAccountTypeRequired(countryCode);
        boolean isBankAccountTypeSet = !bankAccountTypeRequired || model.getSelectedBankAccountType().get() != null;
        model.getBankAccountTypeErrorVisible().set(!isBankAccountTypeSet);

        boolean isNationalAccountIdValid = !BankAccountUtils.isNationalAccountIdRequired(countryCode) || model.getNationalAccountIdValidator().validateAndGet();

        boolean isValid = isCurrencySet &&
                isHolderNameValid &&
                isHolderIdValid &&
                isBankNameValid &&
                isBankIdValid &&
                isBranchIdValid &&
                isBankAccountTypeSet &&
                isNationalAccountIdValid;
        model.getRequireValidation().set(true);
        return isValid;
    }

    @Override
    public NationalBankAccountPayload getAccountPayload() {
        return new NationalBankAccountPayload(model.getId(),
                model.getSelectedCountry().get().getCode(),
                model.getSelectedCurrency().get().getCode(),
                Optional.ofNullable(model.getHolderName().get()),
                Optional.ofNullable(model.getHolderId().get()),
                Optional.ofNullable(model.getBankName().get()),
                Optional.ofNullable(model.getBankId().get()),
                Optional.ofNullable(model.getBranchId().get()),
                model.getAccountNr().get(),
                Optional.ofNullable(model.getSelectedBankAccountType().get()),
                Optional.ofNullable(model.getNationalAccountId().get()));
    }

    void onValidationDone() {
        model.getRequireValidation().set(false);
    }

    void onSelectCountry(Country selectedCountry) {
        if (selectedCountry == null || selectedCountry.equals(model.getSelectedCountry().get())) {
            return;
        }
        model.getSelectedCountry().set(selectedCountry);
        model.getCountryErrorVisible().set(false);
        model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(selectedCountry.getCode()));
        checkCurrencyCountryMatch();

        // Reset data
        model.getSelectedBankAccountType().set(null);
        model.getHolderName().set(null);
        model.getHolderId().set(null);
        model.getBankName().set(null);
        model.getBankId().set(null);
        model.getBranchId().set(null);
        model.getAccountNr().set(null);
        model.getNationalAccountId().set(null);

        applySelectCountry(selectedCountry);
    }

    void onSelectCurrency(FiatCurrency selectedCurrency) {
        model.getSelectedCurrency().set(selectedCurrency);
        model.getCurrencyErrorVisible().set(false);
        model.getRequireValidation().set(false);
        checkCurrencyCountryMatch();
    }

    void onCurrencyCountryMisMatchPopupClosed(boolean applyMatchingCurrency) {
        model.getCurrencyCountryMismatch().set(false);
        if (applyMatchingCurrency) {
            model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(model.getSelectedCountry().get().getCode()));
        }
    }

    void onSelectBankAccountType(BankAccountType selectedBankAccountType) {
        model.getSelectedBankAccountType().set(selectedBankAccountType);
        model.getBankAccountTypeErrorVisible().set(false);
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

    private void applySelectCountry(Country selectedCountry) {
        if (selectedCountry != null) {
            String countryCode = selectedCountry.getCode();

            model.getUseValidation().set(BankAccountUtils.useValidation(countryCode));

            model.getHolderIdDescription().set(BankAccountUtils.getHolderIdDescription(countryCode));
            model.getHolderIdPrompt().set(BankAccountUtils.getPrompt(countryCode, BankAccountUtils.getHolderIdDescription(countryCode)));

            model.getBankIdDescription().set(BankAccountUtils.getBankIdDescription(countryCode));
            model.getBankIdPrompt().set(BankAccountUtils.getPrompt(countryCode, BankAccountUtils.getBankIdDescription(countryCode)));

            model.getBranchIdDescription().set(BankAccountUtils.getBranchIdDescription(countryCode));
            model.getBranchIdPrompt().set(BankAccountUtils.getPrompt(countryCode, BankAccountUtils.getBranchIdDescription(countryCode)));

            model.getAccountNrDescription().set(BankAccountUtils.getAccountNrDescription(countryCode));
            model.getAccountNrPrompt().set(BankAccountUtils.getPrompt(countryCode, BankAccountUtils.getAccountNrDescription(countryCode)));

            model.getNationalAccountIdDescription().set(BankAccountUtils.getNationalAccountIdDescription(countryCode));
            model.getNationalAccountIdPrompt().set(BankAccountUtils.getPrompt(countryCode, BankAccountUtils.getNationalAccountIdDescription(countryCode)));


            model.getIsBankAccountTypesVisible().set(BankAccountUtils.isBankAccountTypeRequired(countryCode));
            model.getIsHolderIdVisible().set(BankAccountUtils.isHolderIdRequired(countryCode));
            model.getIsBankNameVisible().set(BankAccountUtils.isBankNameRequired(countryCode));
            model.getIsBankIdVisible().set(BankAccountUtils.isBankIdRequired(countryCode));
            model.getIsBranchIdVisible().set(BankAccountUtils.isBranchIdRequired(countryCode));
            model.getIsNationalAccountIdVisible().set(BankAccountUtils.isNationalAccountIdRequired(countryCode));
        }
    }
}