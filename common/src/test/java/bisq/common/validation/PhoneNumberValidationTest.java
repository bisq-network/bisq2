package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhoneNumberValidationTest {
    @Test
    void testValidGermanNumbers() {
        assertTrue(PhoneNumberValidation.isValid("+4915123456789", "DE"));
        assertTrue(PhoneNumberValidation.isValid("0151 23456789", "DE"));
        assertTrue(PhoneNumberValidation.isValid("004915123456789", "DE")); // using international prefix
    }

    @Test
    void testInvalidGermanNumbers() {
        assertFalse(PhoneNumberValidation.isValid("12345", "DE")); // too short
        assertFalse(PhoneNumberValidation.isValid("+49 15123", "DE")); // too short
        assertFalse(PhoneNumberValidation.isValid("++4915123456789", "DE")); // invalid format
        assertFalse(PhoneNumberValidation.isValid("+++4915123456789", "DE")); // invalid format
    }

    @Test
    void testValidUSNumbers() {
        assertTrue(PhoneNumberValidation.isValid("+1 202-555-0171", "US"));
        assertTrue(PhoneNumberValidation.isValid("202-555-0171", "US"));
        assertTrue(PhoneNumberValidation.isValid("+1 415 9604264", "US"));
    }

    @Test
    void testInvalidUSNumbers() {
        assertFalse(PhoneNumberValidation.isValid("123", "US"));
        assertFalse(PhoneNumberValidation.isValid("999-999-9999", "US")); // possible format but invalid number
        assertFalse(PhoneNumberValidation.isValid("\u202D+1 415 9604264\u202C", "US")); //invisible directional formatting chars
        assertFalse(PhoneNumberValidation.isValid("+1202555017123456", "US"));
    }

    @Test
    void testNullOrEmptyInput() {
        assertFalse(PhoneNumberValidation.isValid(null, "US"));
        assertFalse(PhoneNumberValidation.isValid("", "US"));
    }

    @Test
    void testInvalidRegion() {
        assertFalse(PhoneNumberValidation.isValid("+4915123456789", "XX")); // invalid region
    }
}