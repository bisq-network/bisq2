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
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.regex.Pattern;

public class PhoneNumberValidation {
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    private static final Pattern VALID_PHONE_INPUT = Pattern.compile("^\\+?[0-9 ()-]*$");

    public static boolean isValid(String number, String regionCode) {
        if (StringUtils.isEmpty(number) ||
                StringUtils.isEmpty(regionCode) ||
                number.length() > 15) {
            return false;
        }
        if (!PHONE_NUMBER_UTIL.getSupportedRegions().contains(regionCode)) {
            return false;
        }

        // Basic sanity check: allow only 0 or 1 '+' at the beginning
        if (!VALID_PHONE_INPUT.matcher(number).matches()
                || countChar(number, '+') > 1
                || !number.startsWith("+")
                && number.contains("+")) {
            return false;
        }

        try {
            PhoneNumber phoneNumber = PHONE_NUMBER_UTIL.parse(number, regionCode);
            return PHONE_NUMBER_UTIL.isValidNumber(phoneNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }

    private static int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }
}
