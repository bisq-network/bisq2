package bisq.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumberValidationTest {
    @Test
    @DisplayName("valid tokens with dot separator")
    void valid_tokens_with_dot_separator() {
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
    @DisplayName("invalid tokens with dot separator")
    void invalid_tokens_with_dot_separator() {
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
    @DisplayName("valid tokens with comma separator")
    void valid_tokens_with_comma_separator() {
        char decimalSeparator = ',';
        assertTrue(NumberValidation.isValidNumberInputToken("0", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken(",", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken(",5", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("1,", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("1,23", decimalSeparator));
    }

    @Test
    @DisplayName("invalid tokens with comma separator")
    void invalid_tokens_with_comma_separator() {
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
    @DisplayName("separator is treated literally")
    void separator_is_treated_literally() {
        char decimalSeparator = '|';
        assertTrue(NumberValidation.isValidNumberInputToken("1|2", decimalSeparator));
        assertTrue(NumberValidation.isValidNumberInputToken("|", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1.2", decimalSeparator));
        assertFalse(NumberValidation.isValidNumberInputToken("1||2", decimalSeparator));
    }
}
