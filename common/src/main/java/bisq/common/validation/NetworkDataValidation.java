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

import bisq.common.platform.Version;
import bisq.common.util.DateUtils;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NetworkDataValidation {
    public static final long TWO_HOURS = TimeUnit.HOURS.toMillis(2);
    public static final long BISQ_1_LAUNCH_DATE = DateUtils.getUTCDate(2016, GregorianCalendar.APRIL, 27).getTime();

    public static void validateDate(long date) {
        // Date can be max 2 hours in future and cannot be older than bisq 1 launch date
        checkArgument(date < System.currentTimeMillis() + TWO_HOURS && date > BISQ_1_LAUNCH_DATE,
                "Date is either too far in the future or too far in the past. date=" + new Date(date));
    }

    public static void validateHash(byte[] hash) {
        checkArgument(hash.length == 20,
                "Hash must be 20 bytes");
    }

    // Signature are usually 71 - 73 chars
    public static void validateECSignature(byte[] signature) {
        checkArgument(signature.length >= 68 && signature.length <= 74,
                "Signature not of the expected size. signature=" + Arrays.toString(signature));
    }

    public static void validateECPubKey(byte[] pubKey) {
        checkArgument(pubKey.length > 50 && pubKey.length < 100,
                "Public key not of the expected size. pubKey=" + Arrays.toString(pubKey));
    }

    public static void validateECPubKey(PublicKey publicKey) {
        validateECPubKey(publicKey.getEncoded());
    }


    // IDs are created with StringUtils.createUid() which generates 36 chars. We allow upt to 50 for more flexibility.
    // Can be short id as well or custom IDs...
    public static void validateId(String id) {
        checkArgument(id.length() <= 50, "ID must not be longer than 50 characters. id=" + id);
    }

    public static void validateId(Optional<String> id) {
        id.ifPresent(NetworkDataValidation::validateId);
    }

    // Profile ID is hash as hex
    public static void validateProfileId(String profileId) {
        checkArgument(profileId.length() == 40, "Profile ID must be 40 characters. profileId=" + profileId);
    }

    public static void validateTradeId(String tradeId) {
        // For private channels we combine user profile IDs for channelId
        validateText(tradeId, 200);
    }

    public static void validateText(String text, int maxLength) {
        checkArgument(text.length() <= maxLength,
                "Text must not be longer than " + maxLength + ". text=" + text);
    }

    public static void validateText(Optional<String> text, int maxTextLength) {
        text.ifPresent(e -> validateText(e, maxTextLength));
    }

    public static void validateRequiredText(String text, int maxLength) {
        checkArgument(!StringUtils.isEmpty(text), "Text must not be null or empty");
        validateText(text, maxLength);
    }

    public static void validateByteArray(byte[] bytes, int maxLength) {
        checkArgument(bytes.length <= maxLength,
                "Byte array must not be longer than " + maxLength + ". bytes.length=" + bytes.length);
    }

    // Longest supported version is xxx.xxx.xxx
    public static void validateVersion(String version) {
        Version.validate(version);
        checkArgument(version.length() <= 11 && !version.isEmpty(),
                "Version too long or empty. version=" + version);
    }

    // Language or country code
    public static void validateCode(String code) {
        checkArgument(code.length() < 10,
                "Code too long. code=" + code);
    }

    public static void validateRequiredCode(String code) {
        checkArgument(!StringUtils.isEmpty(code), "Code must not be null or empty");
        validateCode(code);
    }

    public static void validateBtcTxId(String txId) {
        checkArgument(txId.length() == 64,
                "BTC txId must be 64 characters. txId=" + txId);
    }

    public static void validateBtcAddress(String address) {
        checkArgument(address.length() >= 26 && address.length() <= 90,
                "BTC address not in expected size. address=" + address);
    }

    public static void validateHashAsHex(String hashAsHex) {
        checkArgument(hashAsHex.length() == 40,
                "Hash as hex must be 40 characters. hashAsHex=" + hashAsHex);
    }

    // Bisq 1 pubKeys about 600 chars
    public static void validatePubKeyBase64(String pubKeyBase64) {
        checkArgument(pubKeyBase64.length() < 1000,
                "pubKeyBase64 too long. pubKeyBase64=" + pubKeyBase64);
    }

    // About 176 chars
    public static void validatePubKeyHex(String pubKeyHex) {
        checkArgument(pubKeyHex.length() < 200,
                "signatureBase64 too long.pubKeyHex=" + pubKeyHex);
    }

    // Bisq 1 signatureBase64 about 88 chars 
    public static void validateSignatureBase64(String signatureBase64) {
        checkArgument(signatureBase64.length() < 100,
                "signatureBase64 too long. signatureBase64=" + signatureBase64);
    }

    // Bisq 1 bond usernames
    public static void validateBondUserName(String bondUserName) {
        checkArgument(bondUserName.length() < 100,
                "Bond username too long. bondUserName=" + bondUserName);
    }

    /**
     * The validation process follows these steps:
     * 1. Basic format and length validation
     * 2. Country code validation (must be letters)
     * 3. Check digits validation (must be numeric)
     * 4. Character validation (alphanumeric only)
     * 5. MOD-97 checksum verification using the international standard algorithm
     */
    public static void validateIbanFormat(String iban) {
        checkArgument(StringUtils.isNotEmpty(iban), "IBAN must not be null or empty");

        checkArgument(iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}"),
                "Invalid IBAN format. Must start with country code (2 letters) followed by check digits (2 numbers) and BBAN. iban=" + iban);

        checkArgument(iban.length() >= 15 && iban.length() <= 34,
                "IBAN length must be between 15 and 34 characters. iban=" + iban);

        validateIbanChecksum(iban);
    }

    public static void validateSepaIbanFormat(String iban, List<String> sepaCountryCodes) {
        validateIbanFormat(iban);

        String countryCode = iban.substring(0, 2);

        checkArgument(sepaCountryCodes.contains(countryCode),
                "IBAN country code '" + countryCode + "' is not a SEPA member country. Only SEPA countries are supported for SEPA transfers. iban=" + iban);
    }

    /**
     * The validation covers:
     * 1. Length validation (8 or 11 characters)
     * 2. Format validation (bank code, country code, location code, optional branch code)
     * 3. Character validation (specific rules for each position)
     * 4. Business rule validation (location code restrictions, branch code patterns)
     * 5. Problematic BIC detection (Revolut blocking for SEPA)
     */
    public static void validateBicFormat(String bic) {
        checkArgument(StringUtils.isNotEmpty(bic), "BIC must not be null or empty");

        checkArgument(bic.length() == 8 || bic.length() == 11,
                "BIC length must be 8 or 11 characters. bic=" + bic);

        // Basic format validation using the same pattern as Bisq v1
        checkArgument(bic.matches("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?"),
                "Invalid BIC/SWIFT format. Must follow pattern of institution code (4 letters) + country code (2 letters) + location code (2 alphanumeric) + optional branch code (3 alphanumeric). bic=" + bic);

        String upperBic = bic.toUpperCase(Locale.ROOT);

        // Bank code and country code must be letters only (positions 0-5)
        // This follows the exact validation logic from Bisq v1's BICValidator
        for (int i = 0; i < 6; i++) {
            checkArgument(Character.isLetter(upperBic.charAt(i)),
                    "BIC bank code and country code must be letters only. Invalid character at position " + i + ". bic=" + bic);
        }

        // Location code validation (positions 6-7) - specific banking standard rules
        char locationFirst = upperBic.charAt(6);
        char locationSecond = upperBic.charAt(7);
        checkArgument(locationFirst != '0' && locationFirst != '1',
                "BIC location code cannot start with 0 or 1 (reserved for test purposes). bic=" + bic);
        checkArgument(locationSecond != 'O',
                "BIC location code cannot end with letter O (to avoid confusion with zero). bic=" + bic);

        // Branch code validation (if present) - must follow an XXX pattern if starts with X
        // This implements the same business rule from Bisq v1's BICValidator
        if (upperBic.length() == 11) {
            if (upperBic.charAt(8) == 'X') {
                checkArgument(upperBic.charAt(9) == 'X' && upperBic.charAt(10) == 'X',
                        "BIC branch code starting with X must be XXX. bic=" + bic);
            }
        }

        // Business rule - block problematic BICs that cause issues in SEPA transfers
        // This maintains the same Revolut blocking logic from Bisq v1
        checkArgument(!upperBic.startsWith("REVO"),
                "Revolut BIC codes are not supported for traditional SEPA transfers. bic=" + bic);
    }

    public static void validateEmail(String email) {
        checkArgument(StringUtils.isNotEmpty(email), "Email must not be empty");
        checkArgument(email.length() <= 100, "Email must not be longer than 100 characters. email=" + email);

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        checkArgument(email.matches(emailRegex), "Invalid email format. email: " + email);
    }

    public static void validateIbanCountryConsistency(String iban, String countryCode) {
        checkArgument(StringUtils.isNotEmpty(iban), "IBAN must not be empty for country consistency check");
        checkArgument(StringUtils.isNotEmpty(countryCode), "Country code must not be empty for IBAN consistency check");
        checkArgument(iban.length() >= 2, "IBAN too short for country code extraction. iban=" + iban);

        String cleanIban = iban.replaceAll("\\s", "").toUpperCase();
        String ibanCountryCode = cleanIban.substring(0, 2);

        checkArgument(countryCode.equals(ibanCountryCode),
                "IBAN country code '" + ibanCountryCode + "' does not match declared country '" + countryCode + "'. iban=" + iban);
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


    /**
     * Validates IBAN checksum using the MOD-97 algorithm from ISO 13616.
     */
    private static void validateIbanChecksum(String iban) {
        try {
            String upperIban = iban.toUpperCase(Locale.ROOT);

            // Country code validation (positions 0-1 must be letters)
            checkArgument(Character.isLetter(upperIban.charAt(0)) && Character.isLetter(upperIban.charAt(1)),
                    "IBAN country code must be letters. iban=" + iban);

            // Check digits validation (positions 2-3 must be digits)
            checkArgument(Character.isDigit(upperIban.charAt(2)) && Character.isDigit(upperIban.charAt(3)),
                    "IBAN check digits must be numeric. iban=" + iban);

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
                    throw new IllegalArgumentException("IBAN contains invalid characters. Only letters and digits are allowed. iban=" + iban);
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

            checkArgument(result == 1, "IBAN checksum validation failed. The IBAN appears to contain errors. iban=" + iban);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("IBAN checksum validation failed due to invalid format. iban=" + iban, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("IBAN checksum validation failed. The IBAN appears to contain errors. iban=" + iban, e);
        }
    }
}