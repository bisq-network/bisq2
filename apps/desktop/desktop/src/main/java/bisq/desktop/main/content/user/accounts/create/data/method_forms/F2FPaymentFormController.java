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

import bisq.common.locale.Country;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.desktop.ServiceProvider;
import bisq.i18n.Res;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class F2FPaymentFormController extends PaymentFormController {

    public F2FPaymentFormController(ServiceProvider serviceProvider, Consumer<Map<String, Object>> dataChangeHandler) {
        super(serviceProvider, dataChangeHandler);
    }

    @Override
    protected PaymentFormView createView() {
        return new F2FPaymentFormView(new PaymentFormModel(), this);
    }

    public Map<String, String> getValidationErrors() {
        Map<String, String> errors = new HashMap<>();

        Optional<String> city = getFormDataAsString("city");
        Optional<String> contact = getFormDataAsString("contact");
        Optional<String> extraInfo = getFormDataAsString("extraInfo");
        Optional<Country> country = getFormDataAsCountry();

        validateTextField(city, 2, 100, "city")
                .ifPresent(error -> errors.put("city", error));

        validateTextField(contact, 5, 100, "contact")
                .ifPresent(error -> errors.put("contact", error));

        validateTextField(extraInfo, 2, 500, "extraInfo")
                .ifPresent(error -> errors.put("extraInfo", error));

        if (country.isEmpty()) {
            errors.put("country", Res.get("validation.countryRequired"));
        }

        return errors;
    }

    @Override
    public F2FPaymentFormView getView() {
        return (F2FPaymentFormView) super.getView();
    }

    @Override
    protected void updateViewFromFormData() {
        F2FPaymentFormView f2fView = getView();

        Optional<String> city = getFormDataAsString("city");
        Optional<String> contact = getFormDataAsString("contact");
        Optional<String> extraInfo = getFormDataAsString("extraInfo");
        Optional<Country> country = getFormDataAsCountry();

        f2fView.setCityText(city);
        f2fView.setContactText(contact);
        f2fView.setExtraInfoText(extraInfo);
        f2fView.setSelectedCountry(country);
    }

    private Optional<String> getFormDataAsString(String key) {
        return StringUtils.toOptional((String) formData.get(key));
    }

    private Optional<Country> getFormDataAsCountry() {
        return Optional.ofNullable((Country) formData.get("country"));
    }

    private Optional<String> validateTextField(Optional<String> valueOpt, int minLength, int maxLength,
                                               String fieldKey) {
        if (valueOpt.isEmpty()) {
            return Optional.of(Res.get("validation." + fieldKey + "Required"));
        }

        String value = valueOpt.get();
        if (value.trim().length() < minLength) {
            return Optional.of(Res.get("validation." + fieldKey + "TooShort"));
        }

        try {
            NetworkDataValidation.validateRequiredText(value, maxLength);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(Res.get("validation." + fieldKey + "Invalid"));
        }
    }
}