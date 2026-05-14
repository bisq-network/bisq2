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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SepaPaymentAccountValidationTest {

    @Test
    @DisplayName("valid ibans should pass")
    void valid_ibans_should_pass() {
        List<String> validIbans = Arrays.asList(
                "DE89370400440532013000", // Germany
                "FR1420041010050500013M02606", // France
                "GB29NWBK60161331926819", // UK
                "GR1601101250000000012300695", // Greece
                "ES9121000418450200051332" // Spain
        );

        for (String iban : validIbans) {
            assertDoesNotThrow(() -> SepaPaymentAccountValidation.validateIban(iban),
                    "Valid IBAN should not throw: " + iban);
        }
    }

    @Test
    @DisplayName("iban with invalid checksum should fail")
    void iban_with_invalid_checksum_should_fail() {
        String invalidIban = "DE89370400440532013001"; // altered last digit to fail checksum
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIban(invalidIban));
        assertTrue(exception.getMessage().contains("checksum"), "Expected checksum error");
    }

    @Test
    @DisplayName("iban with invalid format should fail")
    void iban_with_invalid_format_should_fail() {
        String invalidIban = "D189370400440532013000"; // 1st char is digit, invalid format
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIban(invalidIban));
        assertTrue(exception.getMessage().contains("Invalid IBAN format"), "Expected format error");
    }

    @Test
    @DisplayName("iban with wrong length should fail")
    void iban_with_wrong_length_should_fail() {
        String shortIban = "DE89370400"; // too short
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIban(shortIban));
        assertTrue(exception.getMessage().contains("length"), "Expected length error");
    }

    @Test
    @DisplayName("null or empty iban should fail")
    void null_or_empty_iban_should_fail() {
        assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIban(null));

        assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIban(""));
    }

    @Test
    @DisplayName("iban with special characters should fail")
    void iban_with_special_characters_should_fail() {
        String invalidIban = "DE89-3704-0044-0532-0130-00"; // special characters
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIban(invalidIban));
        assertTrue(exception.getMessage().contains("invalid characters") || exception.getMessage().contains("Invalid IBAN format"));
    }
}

