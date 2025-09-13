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

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;

public class SepaPaymentAccountValidation {
    /**
     * The validation process follows these steps:
     * 1. Basic format and length validation
     * 2. Country code validation (must be letters)
     * 3. Check digits validation (must be numeric)
     * 4. Character validation (alphanumeric only)
     * 5. MOD-97 checksum verification using the international standard algorithm
     */
    public static void validateIban(String iban) {
        checkArgument(StringUtils.isNotEmpty(iban), "IBAN must not be null or empty");

        checkArgument(iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}"),
                "Invalid IBAN format. Must start with country code (2 letters) followed by checksum digits (2 numbers) and BBAN.");

        checkArgument(iban.length() >= 15 && iban.length() <= 34,
                "IBAN length must be between 15 and 34 characters.");

        validateIbanChecksum(iban);
    }

    public static void validateSepaIban(String iban, List<String> sepaCountryCodes) {
        validateIban(iban);

        String countryCode = iban.substring(0, 2);
        checkArgument(sepaCountryCodes.contains(countryCode),
                "IBAN country code '" + countryCode + "' is not a SEPA member country. Only SEPA countries are supported for SEPA transfers.");
    }

    /**
     * The validation covers:
     * 1. Length validation (8 or 11 characters)
     * 2. Format validation (bank code, country code, location code, optional branch code)
     * 3. Character validation (specific rules for each position)
     * 4. Business rule validation (location code restrictions, branch code patterns)
     * 5. Problematic BIC detection (Revolut blocking for SEPA)
     */
    public static void validateBic(String bic) {
        checkArgument(StringUtils.isNotEmpty(bic), "BIC must not be null or empty");

        checkArgument(bic.length() == 8 || bic.length() == 11,
                "BIC length must be 8 or 11 characters.");

        // Basic format validation using the same pattern as Bisq v1
        checkArgument(bic.matches("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?"),
                "Invalid BIC/SWIFT format. Must follow pattern of institution code (4 letters) + country code (2 letters) + location code (2 alphanumeric) + optional branch code (3 alphanumeric).");

        String upperBic = bic.toUpperCase(Locale.ROOT);

        // Bank code and country code must be letters only (positions 0-5)
        // This follows the exact validation logic from Bisq v1's BICValidator
        for (int i = 0; i < 6; i++) {
            checkArgument(Character.isLetter(upperBic.charAt(i)),
                    "BIC bank code and country code must be letters only. Invalid character at position " + i + ".");
        }

        // Location code validation (positions 6-7) - specific banking standard rules
        char locationFirst = upperBic.charAt(6);
        char locationSecond = upperBic.charAt(7);
        checkArgument(locationFirst != '0' && locationFirst != '1',
                "BIC location code cannot start with 0 or 1 (reserved for test purposes).");
        checkArgument(locationSecond != 'O',
                "BIC location code cannot end with letter O (to avoid confusion with zero).");

        // Branch code validation (if present) - must follow an XXX pattern if starts with X
        // This implements the same business rule from Bisq v1's BICValidator
        if (upperBic.length() == 11) {
            if (upperBic.charAt(8) == 'X') {
                checkArgument(upperBic.charAt(9) == 'X' && upperBic.charAt(10) == 'X',
                        "BIC branch code starting with X must be XXX.");
            }
        }

        // Business rule - block problematic BICs that cause issues in SEPA transfers
        // This maintains the same Revolut blocking logic from Bisq v1
        checkArgument(!upperBic.startsWith("REVO"),
                "Revolut BIC codes are not supported for traditional SEPA transfers.");
    }

    public static void validateIbanMatchesCountryCode(String iban, String countryCode) {
        checkArgument(StringUtils.isNotEmpty(iban), "IBAN must not be empty for country consistency check");
        checkArgument(StringUtils.isNotEmpty(countryCode), "Country code must not be empty for IBAN consistency check");
        checkArgument(iban.length() >= 2, "IBAN too short for country code extraction.");

        String cleanIban = iban.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        String ibanCountryCode = cleanIban.substring(0, 2);

        checkArgument(countryCode.equals(ibanCountryCode),
                "IBAN country code '" + ibanCountryCode + "' does not match declared country '" + countryCode + "'.");
    }

    /**
     * Validates IBAN checksum using the MOD-97 algorithm from ISO 13616.
     */
    private static void validateIbanChecksum(String iban) {
        try {
            String upperIban = iban.toUpperCase(Locale.ROOT);

            // Country code validation (positions 0-1 must be letters)
            checkArgument(Character.isLetter(upperIban.charAt(0)) && Character.isLetter(upperIban.charAt(1)),
                    "IBAN country code must be letters.");

            // Check digits validation (positions 2-3 must be digits)
            checkArgument(Character.isDigit(upperIban.charAt(2)) && Character.isDigit(upperIban.charAt(3)),
                    "IBAN check digits must be numeric.");

            // Step 1: Rearrange IBAN - move first 4 characters to the end
            // Example: DE89370400440532013000 becomes 370400440532013000DE89
            String rearranged = upperIban.substring(4) + upperIban.substring(0, 4);

            // Step 2: Validate characters and count letters for array sizing
            int charCount = 0;
            for (int k = 0; k < rearranged.length(); k++) {
                char ch = rearranged.charAt(k);
                if (Character.isLetter(ch)) {
                    charCount++;
                } else if (!Character.isDigit(ch)) {
                    throw new IllegalArgumentException("IBAN contains invalid characters. Only letters and digits are allowed.");
                }
            }

            // Step 3: Replace letters with numbers (A=10, B=11, ..., Z=35)
            char[] charArray = new char[rearranged.length() + charCount];
            int i = 0;
            for (int k = 0; k < rearranged.length(); k++) {
                char ch = rearranged.charAt(k);
                if (Character.isLetter(ch)) {
                    // Convert A=10, B=11, etc.
                    int tmp = ch - ('A' - 10);
                    String s = Integer.toString(tmp);
                    charArray[i++] = s.charAt(0);
                    charArray[i++] = s.charAt(1);
                } else {
                    charArray[i++] = ch;
                }
            }

            // Step 4: Perform MOD-97 operation - result should be 1 for valid IBAN
            BigInteger bigInt = new BigInteger(new String(charArray));
            int result = bigInt.mod(new BigInteger("97")).intValue();

            checkArgument(result == 1, "IBAN checksum validation failed. The IBAN appears to contain errors.");

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("IBAN checksum validation failed due to invalid format.", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("IBAN checksum validation failed. The IBAN appears to contain errors.", e);
        }
    }
}
