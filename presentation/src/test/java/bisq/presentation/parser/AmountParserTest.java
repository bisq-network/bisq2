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

package bisq.presentation.parser;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class AmountParserTest {
    @Test
    void testFractionDigitsWithinTheAssetPrecisionAreKept() {
        assertEquals(123456789012L, AmountParser.parse("0.123456789012", "XMR").getValue());
        assertEquals(12345678, AmountParser.parse("0.12345678", "BTC").getValue());
        assertEquals(123, AmountParser.parse("1.23", "BSQ").getValue());
        assertEquals(11234, AmountParser.parse("1.1234", "USD").getValue());
        // Codes which are not listed in the CryptoAssetRepository fall back to a precision of 8
        assertEquals(12345678, AmountParser.parse("0.12345678", "UNDEFINED").getValue());
    }

    @Test
    void testFractionDigitsBeyondTheAssetPrecisionAreCutDown() {
        assertEquals(123456789012L, AmountParser.parse("0.1234567890129", "XMR").getValue());
        assertEquals(12345678, AmountParser.parse("0.123456789", "BTC").getValue());
        assertEquals(-12345678, AmountParser.parse("-0.123456789", "BTC").getValue());
        assertEquals(123, AmountParser.parse("1.239", "BSQ").getValue());
        assertEquals(11234, AmountParser.parse("1.12349", "USD").getValue());
    }

    @Test
    void testInputBelowTheSmallestUnitIsCutToZeroQuickly() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertEquals(0, AmountParser.parse("1E-1000000000", "BTC").getValue());
            assertEquals(0, AmountParser.parse("-1E-1000000000", "BTC").getValue());
            assertEquals(0, AmountParser.parse("1E-1000000000", "XMR").getValue());
            assertEquals(0, AmountParser.parse("1E-1000000000", "USD").getValue());
            assertEquals(0, AmountParser.parse("-1E-1000000000", "USD").getValue());
            assertEquals(0, AmountParser.parse("9.99E-99999999", "BTC").getValue());
        });
        assertEquals(0, AmountParser.parse("0.000000009", "BTC").getValue());
        assertEquals(1, AmountParser.parse("0.000000019", "BTC").getValue());
    }

    @Test
    void testUserInputFormats() {
        assertEquals(15000, AmountParser.parse("1,5", "USD").getValue());
        assertEquals(1234567800L, AmountParser.parse("123 456.78", "USD").getValue());
        assertEquals(12345678, AmountParser.parse(" 0.12345678 ", "BTC").getValue());
    }

    @Test
    void testOutOfRangeAndMalformedInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AmountParser.parse("92233720368.54775808", "BTC"));
        assertThrows(IllegalArgumentException.class, () -> AmountParser.parse("1E1000000000", "BTC"));
        assertThrows(NumberFormatException.class, () -> AmountParser.parse("abc", "BTC"));
        // A double parser accepts these, the amount parser does not
        assertThrows(NumberFormatException.class, () -> AmountParser.parse("NaN", "BTC"));
        assertThrows(NumberFormatException.class, () -> AmountParser.parse("0.5d", "BTC"));
        assertThrows(NumberFormatException.class, () -> AmountParser.parse("", "BTC"));
    }
}
