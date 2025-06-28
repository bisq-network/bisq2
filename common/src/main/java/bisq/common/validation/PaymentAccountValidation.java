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

package bisq.common.validation;

import bisq.common.currency.FiatCurrencyRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class PaymentAccountValidation {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;

    public static void validateHolderName(String name) {
        NetworkDataValidation.validateText(name, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
    }

    public static void validateCountryCodes(List<String> countryCodes,
                                            List<String> allowedCountryCodes,
                                            String contextDescription) {
        checkArgument(countryCodes != null && !countryCodes.isEmpty(),
                "Country codes list must not be null or empty for " + contextDescription);

        checkArgument(allowedCountryCodes != null && !allowedCountryCodes.isEmpty(),
                "Allowed country codes list must not be null or empty for " + contextDescription);

        for (String countryCode : countryCodes) {
            checkArgument(allowedCountryCodes.contains(countryCode),
                    "Country code '" + countryCode + "' is not supported for " + contextDescription + ". Supported countries: " + allowedCountryCodes);
        }
    }

    public static void validateCurrencyCodes(List<String> currencyCodes) {
        try {
            FiatCurrencyRepository.getCurrencyByCodes(currencyCodes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid currencyCodes " + currencyCodes);
        }
    }

    public static void validateCurrencyCode(String currencyCode) {
        try {
            FiatCurrencyRepository.getCurrencyByCode(currencyCode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid currencyCode " + currencyCode);
        }
    }
}
