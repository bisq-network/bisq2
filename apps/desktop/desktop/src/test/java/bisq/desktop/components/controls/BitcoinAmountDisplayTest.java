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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class BitcoinAmountDisplayTest {

    private BitcoinAmountDisplay bitcoinAmountDisplay;
    private char decimalSeparator;

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.US);
        decimalSeparator = DecimalFormatSymbols.getInstance(Locale.getDefault()).getDecimalSeparator();
        bitcoinAmountDisplay = new BitcoinAmountDisplay("0.12345678");
    }

    @AfterEach
    void tearDown() {
        bitcoinAmountDisplay = null;
    }

    @Test
    void testZeroBtcAmount() {
        bitcoinAmountDisplay.setBtcAmount("0.0");
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testWholeNumberBtcAmount() {
        bitcoinAmountDisplay.setBtcAmount("5");
        //5.00 000 000
        assertEquals("5" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part"));
        assertFalse(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
    }

    @Test
    void testBtcAmountWithLeadingZeros() {
        bitcoinAmountDisplay.setBtcAmount("0.00123456");
        //0.00 123 456
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 ", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("123 456", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testLargeBtcAmount() {
        bitcoinAmountDisplay.setBtcAmount("1234.56789");
        //1234.56 789 000
        assertEquals("1234" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("56 789 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part"));
        assertFalse(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
    }

    @Test
    void testSmallFractionalBtcAmount() {
        bitcoinAmountDisplay.setBtcAmount("0.00000001");
        //0.00 000 001
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 00", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("1", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testVerySmallBtcAmount() {
        bitcoinAmountDisplay.setBtcAmount("0.000000124");
        //0.00 000 001 (last digit should be truncated)
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("12", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testSmallBtcAmountWith6LeadingZeros() {
        bitcoinAmountDisplay.setBtcAmount("0.000002146");
        //0.00 000 214 (last digit should be trancated)
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 ", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("214", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testSmallBtcAmountWith5LeadingZeros() {
        bitcoinAmountDisplay.setBtcAmount("0.000019754");
        //0.00 001 975 (last digit should be trancated)
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 00", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("1 975", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testSmallBtcAmountWith4LeadingZeros() {
        bitcoinAmountDisplay.setBtcAmount("0.0006942");
        //0.00 069 420
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("69 420", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testSmallBtcAmountWith3LeadingZeros() {
        bitcoinAmountDisplay.setBtcAmount("0.00582");
        //0.00 582 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 ", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("582 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testSmallBtcAmountWith2LeadingZeros() {
        bitcoinAmountDisplay.setBtcAmount("0.024");
        //0.02 400 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("2 400 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testBtcAmountWithOneLeadingZeroAndMultipleSignificantDigits() {
        bitcoinAmountDisplay.setBtcAmount("0.120011");
        //0.12 001 100
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("12 001 100", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    void testBtcAmountPointTwelve() {
        bitcoinAmountDisplay.setBtcAmount("0.12");
        //0.12 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("12 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    void testBtcAmountPointOne() {
        bitcoinAmountDisplay.setBtcAmount("0.1");
        //0.10 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("10 000 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-empty"));
    }

    @Test
    void testBtcAmountOnePointFive() {
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
    void testBtcAmountWithJustDecimalPoint() {
        bitcoinAmountDisplay.setBtcAmount(String.valueOf(decimalSeparator));
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testBtcAmountWithDecimalPointZero() {
        bitcoinAmountDisplay.setBtcAmount(".0");
        //0.00 000 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("00 000 000", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testBtcAmountWithDecimalPointAndNumber() {
        bitcoinAmountDisplay.setBtcAmount(".0112");
        //0.01 120 000
        assertEquals("0" + decimalSeparator, bitcoinAmountDisplay.getIntegerPart().getText());
        assertEquals("0", bitcoinAmountDisplay.getLeadingZeros().getText());
        assertEquals("1 120 000", bitcoinAmountDisplay.getSignificantDigits().getText());

        assertTrue(bitcoinAmountDisplay.getIntegerPart().getStyleClass().contains("bitcoin-amount-display-integer-part-dimmed"));
        assertTrue(bitcoinAmountDisplay.getLeadingZeros().getStyleClass().contains("bitcoin-amount-display-leading-zeros-dimmed"));
    }

    @Test
    void testBtcCodeVisibility() {
        BitcoinAmountDisplay displayWithCode = new BitcoinAmountDisplay("1.0", true);
        BitcoinAmountDisplay displayWithoutCode = new BitcoinAmountDisplay("1.0", false);

        assertTrue(displayWithCode.getBtcCode().isVisible());
        assertFalse(displayWithoutCode.getBtcCode().isVisible());
    }

    @Test
    void testBtcAmountWithTrailingDecimalPoint() {
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
    void testDifferentStyleConfigurations() {
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
    void testDecimalSeparatorWithGermanLocale() {
        Locale originalLocale = LocaleRepository.getDefaultLocale();
        try {
            LocaleRepository.setDefaultLocale(Locale.GERMANY);
            BitcoinAmountDisplay germanDisplay = new BitcoinAmountDisplay("1,234");
            char germanSeparator = DecimalFormatSymbols.getInstance(LocaleRepository.getDefaultLocale()).getDecimalSeparator();

            assertEquals(',', germanSeparator);
            assertEquals("1" + germanSeparator, germanDisplay.getIntegerPart().getText());
            assertEquals("", germanDisplay.getLeadingZeros().getText());
            assertEquals("23 400 000", germanDisplay.getSignificantDigits().getText());
        } finally {
            LocaleRepository.setDefaultLocale(originalLocale);
        }
    }

    @Test
    void testDecimalSeparatorWithFrenchLocale() {
        Locale originalLocale = LocaleRepository.getDefaultLocale();
        try {
            LocaleRepository.setDefaultLocale(Locale.FRANCE);

            BitcoinAmountDisplay frenchDisplayZero = new BitcoinAmountDisplay("0,0");
            BitcoinAmountDisplay frenchDisplaySmall = new BitcoinAmountDisplay("0,00123");
            BitcoinAmountDisplay frenchDisplayLarge = new BitcoinAmountDisplay("123,456");

            char frenchSeparator = DecimalFormatSymbols.getInstance(LocaleRepository.getDefaultLocale()).getDecimalSeparator();

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