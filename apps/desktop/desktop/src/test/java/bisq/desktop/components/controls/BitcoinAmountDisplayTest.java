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

package bisq.desktop.components.controls;

import bisq.common.locale.LocaleRepository;
import bisq.common.util.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitcoinAmountDisplayTest {

    private BitcoinAmountDisplay bitcoinAmountDisplay;
    private char decimalSeparator;

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.US);
        decimalSeparator = StringUtils.getDecimalSeparator(Locale.getDefault());
        bitcoinAmountDisplay = new BitcoinAmountDisplay("0.12345678");
    }

    @AfterEach
    void tearDown() {
        bitcoinAmountDisplay = null;
    }

    @Test
    @DisplayName("zero btc amount")
    void zero_btc_amount() {
        bitcoinAmountDisplay.setBtcAmount("0.0");
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("whole number btc amount")
    void whole_number_btc_amount() {
        bitcoinAmountDisplay.setBtcAmount("5");
        //5.00 000 000
        assertEquals("5" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part"));
        assertFalse(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
    }

    @Test
    @DisplayName("btc amount with leading zeros")
    void btc_amount_with_leading_zeros() {
        bitcoinAmountDisplay.setBtcAmount("0.00123456");
        //0.00 123 456
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 ", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("123 456", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("large btc amount")
    void large_btc_amount() {
        bitcoinAmountDisplay.setBtcAmount("1234.56789");
        //1234.56 789 000
        assertEquals("1234" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("56 789 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part"));
        assertFalse(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
    }

    @Test
    @DisplayName("small fractional btc amount")
    void small_fractional_btc_amount() {
        bitcoinAmountDisplay.setBtcAmount("0.00000001");
        //0.00 000 001
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 00", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("1", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("very small btc amount")
    void very_small_btc_amount() {
        bitcoinAmountDisplay.setBtcAmount("0.000000124");
        //0.00 000 001 (last digit should be truncated)
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("12", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("small btc amount with 6 leading zeros")
    void small_btc_amount_with6_leading_zeros() {
        bitcoinAmountDisplay.setBtcAmount("0.000002146");
        //0.00 000 214 (last digit should be trancated)
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 ", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("214", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("small btc amount with 5 leading zeros")
    void small_btc_amount_with5_leading_zeros() {
        bitcoinAmountDisplay.setBtcAmount("0.000019754");
        //0.00 001 975 (last digit should be trancated)
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 00", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("1 975", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("small btc amount with 4 leading zeros")
    void small_btc_amount_with4_leading_zeros() {
        bitcoinAmountDisplay.setBtcAmount("0.0006942");
        //0.00 069 420
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("69 420", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("small btc amount with 3 leading zeros")
    void small_btc_amount_with3_leading_zeros() {
        bitcoinAmountDisplay.setBtcAmount("0.00582");
        //0.00 582 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 ", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("582 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("small btc amount with 2 leading zeros")
    void small_btc_amount_with2_leading_zeros() {
        bitcoinAmountDisplay.setBtcAmount("0.024");
        //0.02 400 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("2 400 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("btc amount with one leading zero and multiple significant digits")
    void btc_amount_with_one_leading_zero_and_multiple_significant_digits() {
        bitcoinAmountDisplay.setBtcAmount("0.120011");
        //0.12 001 100
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("12 001 100", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    @DisplayName("btc amount point twelve")
    void btc_amount_point_twelve() {
        bitcoinAmountDisplay.setBtcAmount("0.12");
        //0.12 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("12 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    @DisplayName("btc amount point one")
    void btc_amount_point_one() {
        bitcoinAmountDisplay.setBtcAmount("0.1");
        //0.10 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("10 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    @DisplayName("btc amount one point five")
    void btc_amount_one_point_five() {
        bitcoinAmountDisplay.setBtcAmount("1.5");
        //1.50 000 000
        assertEquals("1" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("50 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part"));
        assertFalse(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    @DisplayName("btc amount with just decimal point")
    void btc_amount_with_just_decimal_point() {
        bitcoinAmountDisplay.setBtcAmount(String.valueOf(decimalSeparator));
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("btc amount with decimal point zero")
    void btc_amount_with_decimal_point_zero() {
        bitcoinAmountDisplay.setBtcAmount(".0");
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("btc amount with decimal point and number")
    void btc_amount_with_decimal_point_and_number() {
        bitcoinAmountDisplay.setBtcAmount(".0112");
        //0.01 120 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("1 120 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    @DisplayName("btc code visibility")
    void btc_code_visibility() {
        BitcoinAmountDisplay displayWithCode = new BitcoinAmountDisplay("1.0", true);
        BitcoinAmountDisplay displayWithoutCode = new BitcoinAmountDisplay("1.0", false);

        assertTrue(displayWithCode.getBtcCode().isVisible());
        assertFalse(displayWithoutCode.getBtcCode().isVisible());
    }

    @Test
    @DisplayName("btc amount with trailing decimal point")
    void btc_amount_with_trailing_decimal_point() {
        bitcoinAmountDisplay.setBtcAmount("0.");
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));

        bitcoinAmountDisplay.setBtcAmount("1.");
        //1.00 000 000
        assertEquals("1" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part"));
        assertFalse(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    @DisplayName("different style configurations")
    void different_style_configurations() {
        BitcoinAmountDisplay testDisplay = new BitcoinAmountDisplay("123.456789");

        testDisplay.applySmallCompactConfig();
        assertEquals(18, testDisplay.getIntegerPart().getFont().getSize());
        assertEquals(13, testDisplay.getBtcCode().getFont().getSize());

        testDisplay.applyMediumCompactConfig();
        assertEquals(21, testDisplay.getIntegerPart().getFont().getSize());
        assertEquals(18, testDisplay.getBtcCode().getFont().getSize());

        testDisplay.applyMicroCompactConfig();
        assertEquals(12, testDisplay.getIntegerPart().getFont().getSize());
        assertEquals(12, testDisplay.getBtcCode().getFont().getSize());
    }

    @Test
    @DisplayName("decimal separator with german locale")
    void decimal_separator_with_german_locale() {
        Locale originalLocale = LocaleRepository.getDefaultLocale();
        try {
            LocaleRepository.setDefaultLocale(Locale.GERMANY);
            BitcoinAmountDisplay germanDisplay = new BitcoinAmountDisplay("1,234");
            char germanSeparator = StringUtils.getDecimalSeparator();

            assertEquals(',', germanSeparator);
            assertEquals("1" + germanSeparator, germanDisplay.getIntegerPart().getText());
            assertEquals("", germanDisplay.getLeadingZeros().getText());
            assertEquals("23 400 000", germanDisplay.getSignificantDigits().getText());
        } finally {
            LocaleRepository.setDefaultLocale(originalLocale);
        }
    }

    @Test
    @DisplayName("decimal separator with french locale")
    void decimal_separator_with_french_locale() {
        Locale originalLocale = LocaleRepository.getDefaultLocale();
        try {
            LocaleRepository.setDefaultLocale(Locale.FRANCE);

            BitcoinAmountDisplay frenchDisplayZero = new BitcoinAmountDisplay("0,0");
            BitcoinAmountDisplay frenchDisplaySmall = new BitcoinAmountDisplay("0,00123");
            BitcoinAmountDisplay frenchDisplayLarge = new BitcoinAmountDisplay("123,456");

            char frenchSeparator = StringUtils.getDecimalSeparator();

            assertEquals("0" + frenchSeparator, frenchDisplayZero.getIntegerPart().getText());
            assertEquals("0" + frenchSeparator, frenchDisplaySmall.getIntegerPart().getText());
            assertEquals("123" + frenchSeparator, frenchDisplayLarge.getIntegerPart().getText());

            assertEquals("00 ", frenchDisplaySmall.getLeadingZeros().getText());
            assertEquals("123 000", frenchDisplaySmall.getSignificantDigits().getText());

            assertEquals("", frenchDisplayLarge.getLeadingZeros().getText());
            assertEquals("45 600 000", frenchDisplayLarge.getSignificantDigits().getText());
        } finally {
            LocaleRepository.setDefaultLocale(originalLocale);
        }
    }
}