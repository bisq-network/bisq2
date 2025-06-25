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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SepaPaymentAccountValidationTest {

    @Test
    void validIbansShouldPass() {
        List<String> validIbans = Arrays.asList(
                "DE89370400440532013000", // Germany
                "FR1420041010050500013M02606", // France
                "GB29NWBK60161331926819", // UK
                "GR1601101250000000012300695", // Greece
                "ES9121000418450200051332" // Spain
        );

        for (String iban : validIbans) {
            assertDoesNotThrow(() -> SepaPaymentAccountValidation.validateIbanFormat(iban),
                    "Valid IBAN should not throw: " + iban);
        }
    }

    @Test
    void ibanWithInvalidChecksumShouldFail() {
        String invalidIban = "DE89370400440532013001"; // altered last digit to fail checksum
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIbanFormat(invalidIban));
        assertTrue(exception.getMessage().contains("checksum"), "Expected checksum error");
    }

    @Test
    void ibanWithInvalidFormatShouldFail() {
        String invalidIban = "D189370400440532013000"; // 1st char is digit, invalid format
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIbanFormat(invalidIban));
        assertTrue(exception.getMessage().contains("Invalid IBAN format"), "Expected format error");
    }

    @Test
    void ibanWithWrongLengthShouldFail() {
        String shortIban = "DE89370400"; // too short
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIbanFormat(shortIban));
        assertTrue(exception.getMessage().contains("length"), "Expected length error");
    }

    @Test
    void nullOrEmptyIbanShouldFail() {
        assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIbanFormat(null));

        assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIbanFormat(""));
    }

    @Test
    void ibanWithSpecialCharactersShouldFail() {
        String invalidIban = "DE89-3704-0044-0532-0130-00"; // special characters
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                SepaPaymentAccountValidation.validateIbanFormat(invalidIban));
        assertTrue(exception.getMessage().contains("invalid characters") || exception.getMessage().contains("Invalid IBAN format"));
    }
}

