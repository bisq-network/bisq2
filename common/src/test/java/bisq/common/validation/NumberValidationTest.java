package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumberValidationTest {
    @Test
    void testValidTokensWithDotSeparator() {
        char decimalSeparator = '.';
        assertTrue(NumberValidation.isValidNumberInputToken("0", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("123", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken(".", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken(".5", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("0.", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("1.", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("1.23", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("0001", decimalSeparator));
    }

    @Test
    void testInvalidTokensWithDotSeparator() {
        char decimalSeparator = '.';
        assertFalse(NumberValidation.isValidNumberInputToken(null, decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken(" ", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-1", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("+1", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("..", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1-", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("--1", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1..2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1.2.3", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1,2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1 2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1_2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("abc", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1e2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-.", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-1.23", decimalSeparator));
    }

    @Test
    void testValidTokensWithCommaSeparator() {
        char decimalSeparator = ',';
        assertTrue(NumberValidation.isValidNumberInputToken("0", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken(",", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken(",5", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("1,", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("1,23", decimalSeparator));
    }

    @Test
    void testInvalidTokensWithCommaSeparator() {
        char decimalSeparator = ',';
        assertFalse(NumberValidation.isValidNumberInputToken(",,", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1.23", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1,2,3", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1,,2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("+1", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-1,", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1-2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-,", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("-1,23", decimalSeparator));
    }

    @Test
    void testSeparatorIsTreatedLiterally() {
        char decimalSeparator = '|';
        assertTrue(NumberValidation.isValidNumberInputToken("1|2", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("|", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1.2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1||2", decimalSeparator));
    }
}
