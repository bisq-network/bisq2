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

import java.util.regex.Pattern;

public class EmailValidation {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^(?!.*\\.\\.)[A-Z0-9._%+-]+@" +                   // Local part, no consecutive dots
                    "(?:[A-Z0-9](?:[A-Z0-9-]*[A-Z0-9])?\\.)+" +         // Domain labels: no leading/trailing hyphens
                    "[A-Z]{2,}$",                                       // TLD: at least 2 letters
            Pattern.CASE_INSENSITIVE
    );

    public static boolean isValid(String email) {
        return email != null &&
                email.length() <= 254 &&
                EMAIL_PATTERN.matcher(email).matches();
    }
}
