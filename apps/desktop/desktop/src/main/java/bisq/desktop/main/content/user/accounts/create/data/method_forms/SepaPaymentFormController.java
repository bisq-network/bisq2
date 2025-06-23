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

package bisq.desktop.main.content.user.accounts.create.data.method_forms;

import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.locale.Country;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.desktop.ServiceProvider;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class SepaPaymentFormController extends PaymentFormController {

    private final List<String> sepaCountryCodes;

    public SepaPaymentFormController(ServiceProvider serviceProvider, Consumer<Map<String, Object>> dataChangeHandler) {
        super(serviceProvider, dataChangeHandler);
        sepaCountryCodes = FiatPaymentRailUtil.getSepaEuroCountries();
    }

    @Override
    public void onActivate() {
        super.onActivate();
        updateViewFromFormData();
    }

    public void restoreViewFromFormData() {
        updateViewFromFormData();
    }

    @Override
    public void onFieldChanged(String fieldName, Object value) {
        super.onFieldChanged(fieldName, value);
        SepaPaymentFormView sepaView = getView();
        switch (fieldName) {
            case "holderName":
                Optional.ofNullable(value)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .ifPresent(text -> sepaView.setHolderNameText(Optional.of(text)));
                break;
            case "iban":
                Optional.ofNullable(value)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .ifPresent(text -> sepaView.setIbanText(Optional.of(text)));
                break;
            case "bic":
                Optional.ofNullable(value)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .ifPresent(text -> sepaView.setBicText(Optional.of(text)));
                break;
            case "country":
                Optional.ofNullable(value)
                        .filter(Country.class::isInstance)
                        .map(Country.class::cast)
                        .ifPresent(country -> sepaView.setSelectedCountry(Optional.of(country)));
                break;
        }
    }

    public Map<String, String> getValidationErrors() {
        Map<String, String> errors = new HashMap<>();

        Optional<String> holderName = getFormDataAsString("holderName")
                .map(String::trim);
        Optional<String> iban = getFormDataAsString("iban")
                .map(value -> value.replaceAll("\\s", "").toUpperCase());
        Optional<String> bic = getFormDataAsString("bic")
                .map(value -> value.replaceAll("\\s", "").toUpperCase());
        Optional<Country> country = getFormDataAsCountry();
        Optional<List<String>> acceptedCountryCodes = getFormDataAsList();

        getHolderNameValidationError(holderName)
                .ifPresent(error -> errors.put("holderName", error));

        getIbanValidationError(iban)
                .ifPresent(error -> errors.put("iban", error));

        getBicValidationError(bic)
                .ifPresent(error -> errors.put("bic", error));

        getCountryValidationError(country)
                .ifPresent(error -> errors.put("country", error));

        getAcceptedCountriesValidationError(acceptedCountryCodes)
                .ifPresent(error -> errors.put("acceptedCountries", error));

        getIbanCountryConsistencyError(iban, country)
                .ifPresent(error -> errors.put("iban", error));

        return errors;
    }

    public boolean validateIban(String iban) {
        return Optional.ofNullable(iban)
                .filter(StringUtils::isNotEmpty)
                .map(i -> {
                    try {
                        PaymentAccountValidation.validateSepaIbanFormat(i, sepaCountryCodes);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    public boolean validateBic(String bic) {
        return Optional.ofNullable(bic)
                .filter(b -> !b.trim().isEmpty())
                .map(b -> {
                    try {
                        PaymentAccountValidation.validateBicFormat(b);
                        return true;
                    } catch (IllegalArgumentException e) {
                        log.debug("BIC validation failed: {}", e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }

    public boolean validateAcceptedCountries(List<String> acceptedCountryCodes) {
        return Optional.ofNullable(acceptedCountryCodes)
                .filter(codes -> !codes.isEmpty())
                .map(sepaCountryCodes::containsAll)
                .orElse(false);
    }

    public boolean validateIbanCountryConsistency(String iban, Country country) {
        if (country == null || StringUtils.isEmpty(iban) || iban.length() < 2) {
            return true;
        }
        String cleanIban = iban.replaceAll("\\s", "").toUpperCase();
        String ibanCountryCode = cleanIban.substring(0, 2);
        return country.getCode().equals(ibanCountryCode);
    }

    public boolean isRevolutBic(String bic) {
        return Optional.ofNullable(bic)
                .map(String::toUpperCase)
                .filter(b -> b.startsWith("REVO"))
                .isPresent();
    }

    @Override
    public SepaPaymentFormView getView() {
        return (SepaPaymentFormView) super.getView();
    }

    @Override
    protected PaymentFormView createView() {
        return new SepaPaymentFormView(new PaymentFormModel(), this);
    }

    @Override
    protected void updateViewFromFormData() {
        SepaPaymentFormView sepaView = getView();

        Optional<String> holderName = getFormDataAsString("holderName");
        Optional<String> iban = getFormDataAsString("iban");
        Optional<String> bic = getFormDataAsString("bic");
        Optional<Country> country = getFormDataAsCountry();
        Optional<List<String>> acceptedCountryCodes = getFormDataAsList();

        sepaView.setHolderNameText(holderName);
        sepaView.setIbanText(iban);
        sepaView.setBicText(bic);
        sepaView.setSelectedCountry(country);
        sepaView.setAcceptedCountryCodes(acceptedCountryCodes.orElse(FiatPaymentRailUtil.getSepaEuroCountries()));
    }

    private Optional<String> getHolderNameValidationError(Optional<String> holderNameOpt) {
        if (holderNameOpt.isEmpty()) {
            return Optional.of(Res.get("validation.empty"));
        }

        String holderName = holderNameOpt.get().trim();
        if ((holderName.length() < 2) || (holderName.length() > 100)) {
            return Optional.of(Res.get("validation.holderNameInvalidLength"));
        }

        try {
            NetworkDataValidation.validateRequiredText(holderName, 100);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(Res.get("validation.holderNameInvalid"));
        }
    }

    private Optional<String> getIbanValidationError(Optional<String> ibanOpt) {
        if (ibanOpt.isEmpty()) {
            return Optional.of(Res.get("validation.empty"));
        }

        String iban = ibanOpt.get();
        if (!validateIban(iban)) {
            if (iban.length() < 15 || iban.length() > 34) {
                return Optional.of(Res.get("paymentMethod.validation.iban.invalidLength"));
            }
            if (!Character.isLetter(iban.charAt(0)) || !Character.isLetter(iban.charAt(1))) {
                return Optional.of(Res.get("paymentMethod.validation.iban.invalidCountryCode"));
            }
            if (!Character.isDigit(iban.charAt(2)) || !Character.isDigit(iban.charAt(3))) {
                return Optional.of(Res.get("paymentMethod.validation.iban.checkSumNotNumeric"));
            }

            boolean hasInvalidChars = false;
            for (char c : iban.toCharArray()) {
                if (!Character.isLetterOrDigit(c)) {
                    hasInvalidChars = true;
                    break;
                }
            }
            if (hasInvalidChars) {
                return Optional.of(Res.get("paymentMethod.validation.iban.nonNumericChars"));
            }

            String countryCode = iban.substring(0, 2);
            if (!sepaCountryCodes.contains(countryCode)) {
                return Optional.of(Res.get("paymentMethod.validation.iban.sepaNotSupported"));
            }

            return Optional.of(Res.get("paymentMethod.validation.iban.checkSumInvalid"));
        }

        return Optional.empty();
    }

    private Optional<String> getBicValidationError(Optional<String> bicOpt) {
        if (bicOpt.isEmpty()) {
            return Optional.of(Res.get("validation.empty"));
        }

        String bic = bicOpt.get();
        if (!validateBic(bic)) {
            if (isRevolutBic(bic)) {
                return Optional.of(Res.get("paymentMethod.validation.bic.sepaRevolutBic"));
            }
            if (bic.length() != 8 && bic.length() != 11) {
                return Optional.of(Res.get("paymentMethod.validation.bic.invalidLength"));
            }
            if (!bic.matches("[A-Za-z]{4}[A-Za-z]{2}[A-Za-z0-9]{2}([A-Za-z0-9]{3})?")) {
                boolean bankCodeValid = bic.substring(0, 4).matches("[A-Z]{4}");
                boolean countryCodeValid = bic.substring(4, 6).matches("[A-Z]{2}");
                if (!bankCodeValid || !countryCodeValid) {
                    return Optional.of(Res.get("paymentMethod.validation.bic.letters"));
                }
                if (bic.charAt(6) == '0' || bic.charAt(6) == '1') {
                    return Optional.of(Res.get("paymentMethod.validation.bic.invalidLocationCode"));
                }
                if (bic.charAt(7) == 'O') {
                    return Optional.of(Res.get("paymentMethod.validation.bic.invalidLocationCode"));
                }
                if (bic.length() == 11 && bic.charAt(8) == 'X' && (!bic.substring(8).equals("XXX"))) {
                    return Optional.of(Res.get("paymentMethod.validation.bic.invalidBranchCode"));
                }
                return Optional.of(Res.get("paymentMethod.validation.bic.invalidLength"));
            }
        }

        return Optional.empty();
    }

    private Optional<String> getCountryValidationError(Optional<Country> countryOpt) {
        return countryOpt.isEmpty() ?
                Optional.of(Res.get("validation.countryRequired")) :
                Optional.empty();
    }

    private Optional<String> getAcceptedCountriesValidationError(Optional<List<String>> acceptedCountryCodesOpt) {
        return !validateAcceptedCountries(acceptedCountryCodesOpt.orElse(null)) ?
                Optional.of(Res.get("validation.acceptedCountriesRequired")) :
                Optional.empty();
    }

    private Optional<String> getIbanCountryConsistencyError(Optional<String> ibanOpt, Optional<Country> countryOpt) {
        if (countryOpt.isPresent() && ibanOpt.isPresent()) {
            String iban = ibanOpt.get();
            if (iban.length() >= 2 && !validateIbanCountryConsistency(iban, countryOpt.get())) {
                String ibanCountryCode = iban.substring(0, 2);
                return Optional.of(Res.get("validation.ibanCountryMismatch", ibanCountryCode));
            }
        }
        return Optional.empty();
    }

    private Optional<String> getFormDataAsString(String key) {
        return StringUtils.toOptional((String) formData.get(key));
    }

    private Optional<Country> getFormDataAsCountry() {
        return Optional.ofNullable((Country) formData.get("country"));
    }

    @SuppressWarnings("unchecked")
    private Optional<List<String>> getFormDataAsList() {
        return Optional.ofNullable((List<String>) formData.get("acceptedCountryCodes"));
    }
}