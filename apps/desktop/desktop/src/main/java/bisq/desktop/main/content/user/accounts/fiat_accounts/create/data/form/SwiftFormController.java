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

import bisq.account.accounts.fiat.SwiftAccountPayload;
import bisq.common.asset.FiatCurrency;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.validator.ValidatorBase;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SwiftFormController extends FormController<SwiftFormView, SwiftFormModel, SwiftAccountPayload> {
    public SwiftFormController(ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    @Override
    protected SwiftFormView createView() {
        return new SwiftFormView(model, this);
    }

    @Override
    protected SwiftFormModel createModel() {
        return new SwiftFormModel(StringUtils.createUid(),
                CountryRepository.getAllCountries(),
                FiatCurrencyRepository.getAllCurrencies());
    }

    @Override
    public void onActivate() {
        super.onActivate();
        model.getRunValidation().set(false);
        model.getBankCountryErrorVisible().set(false);
        model.getCurrencyErrorVisible().set(false);
        model.getIntermediaryBankCountryErrorVisible().set(false);
        showOverlay();
    }

    @Override
    public boolean validate() {
        boolean bankCountrySet = model.getSelectedBankCountry().get() != null;
        model.getBankCountryErrorVisible().set(!bankCountrySet);
        boolean currencySet = model.getSelectedCurrency().get() != null;
        model.getCurrencyErrorVisible().set(!currencySet);

        boolean intermediaryBankValid = true;
        if (model.getUseIntermediaryBank().get()) {
            boolean intermediaryCountrySet = model.getIntermediaryBankCountry().get() != null;
            model.getIntermediaryBankCountryErrorVisible().set(!intermediaryCountrySet);
            intermediaryBankValid = intermediaryCountrySet &&
                    model.getIntermediaryBankSwiftCodeValidator().validateAndGet() &&
                    model.getIntermediaryBankNameValidator().validateAndGet() &&
                    model.getIntermediaryBankAddressValidator().validateAndGet() &&
                    validateIfPresent(model.getIntermediaryBankBranch().get(),
                            model.getIntermediaryBankBranchValidator());
        }

        boolean isValid = bankCountrySet &&
                currencySet &&
                model.getBeneficiaryNameValidator().validateAndGet() &&
                model.getBeneficiaryAccountNrValidator().validateAndGet() &&
                model.getBeneficiaryAddressValidator().validateAndGet() &&
                validateIfPresent(model.getBeneficiaryPhone().get(),
                        model.getBeneficiaryPhoneValidator()) &&
                model.getBankSwiftCodeValidator().validateAndGet() &&
                model.getBankNameValidator().validateAndGet() &&
                model.getBankAddressValidator().validateAndGet() &&
                validateIfPresent(model.getBankBranch().get(),
                        model.getBankBranchValidator()) &&
                validateIfPresent(model.getAdditionalInstructions().get(),
                        model.getAdditionalInstructionsValidator()) &&
                intermediaryBankValid;
        model.getRunValidation().set(true);
        return isValid;
    }

    @Override
    public SwiftAccountPayload createAccountPayload() {
        Optional<String> bankBranch = StringUtils.toOptional(model.getBankBranch().get());
        Optional<String> beneficiaryPhone = StringUtils.toOptional(model.getBeneficiaryPhone().get());
        Optional<String> additionalInstructions = StringUtils.toOptional(model.getAdditionalInstructions().get());

        Optional<String> intermediaryBankCountryCode = Optional.empty();
        Optional<String> intermediaryBankSwiftCode = Optional.empty();
        Optional<String> intermediaryBankName = Optional.empty();
        Optional<String> intermediaryBankBranch = Optional.empty();
        Optional<String> intermediaryBankAddress = Optional.empty();
        if (model.getUseIntermediaryBank().get()) {
            intermediaryBankCountryCode = Optional.ofNullable(model.getIntermediaryBankCountry().get())
                    .map(Country::getCode);
            intermediaryBankSwiftCode = StringUtils.toOptional(model.getIntermediaryBankSwiftCode().get());
            intermediaryBankName = StringUtils.toOptional(model.getIntermediaryBankName().get());
            intermediaryBankBranch = StringUtils.toOptional(model.getIntermediaryBankBranch().get());
            intermediaryBankAddress = StringUtils.toOptional(model.getIntermediaryBankAddress().get());
        }

        return new SwiftAccountPayload(model.getId(),
                model.getSelectedBankCountry().get().getCode(),
                model.getBeneficiaryName().get(),
                model.getBeneficiaryAccountNr().get(),
                beneficiaryPhone,
                model.getBeneficiaryAddress().get(),
                model.getSelectedCurrency().get().getCode(),
                model.getBankSwiftCode().get(),
                model.getBankName().get(),
                bankBranch,
                model.getBankAddress().get(),
                intermediaryBankCountryCode,
                intermediaryBankSwiftCode,
                intermediaryBankName,
                intermediaryBankBranch,
                intermediaryBankAddress,
                additionalInstructions);
    }

    void onValidationDone() {
        model.getRunValidation().set(false);
    }

    void onSelectBankCountry(Country selectedCountry) {
        model.getSelectedBankCountry().set(selectedCountry);
        model.getBankCountryErrorVisible().set(false);
        model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(selectedCountry.getCode()));
        checkCurrencyCountryMatch();
    }

    void onSelectCurrency(FiatCurrency selectedCurrency) {
        model.getSelectedCurrency().set(selectedCurrency);
        model.getCurrencyErrorVisible().set(false);
        checkCurrencyCountryMatch();
    }

    void onSelectIntermediaryBankCountry(Country selectedCountry) {
        model.getIntermediaryBankCountry().set(selectedCountry);
        model.getIntermediaryBankCountryErrorVisible().set(false);
    }

    void onToggleUseIntermediaryBank(boolean useIntermediaryBank) {
        model.getUseIntermediaryBank().set(useIntermediaryBank);
        if (!useIntermediaryBank) {
            model.getIntermediaryBankCountryErrorVisible().set(false);
        }
    }

    void onCurrencyCountryMisMatchPopupClosed(boolean applyMatchingCurrency) {
        model.getCurrencyCountryMismatch().set(false);
        if (applyMatchingCurrency) {
            model.getSelectedCurrency().set(FiatCurrencyRepository.getCurrencyByCountryCode(model.getSelectedBankCountry().get().getCode()));
        }
    }

    private void checkCurrencyCountryMatch() {
        if (model.getSelectedBankCountry().get() != null &&
                model.getSelectedCurrency().get() != null &&
                !FiatCurrencyRepository.getCurrencyByCountryCode(model.getSelectedBankCountry().get().getCode()).equals(model.getSelectedCurrency().get())) {
            model.getCurrencyCountryMismatch().set(true);
        } else {
            model.getCurrencyCountryMismatch().set(false);
        }
    }

    private boolean validateIfPresent(String value, ValidatorBase validator) {
        if (StringUtils.isNotEmpty(value)) {
            return validator.validateAndGet();
        }
        return true;
    }
}
