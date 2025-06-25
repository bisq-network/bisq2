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

import bisq.common.util.StringUtils;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class PaymentAccountValidation {
    public static void validateEmail(String email) {
        checkArgument(StringUtils.isNotEmpty(email), "Email must not be empty");
        checkArgument(email.length() <= 100, "Email must not be longer than 100 characters. email=" + email);

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        checkArgument(email.matches(emailRegex), "Invalid email format. email: " + email);
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
}
