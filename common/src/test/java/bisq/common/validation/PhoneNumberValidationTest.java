package bisq.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhoneNumberValidationTest {
    @Test
    @DisplayName("valid german numbers")
    void valid_german_numbers() {
        assertTrue(PhoneNumberValidation.isValid("+4915123456789", "DE"));
        assertTrue(PhoneNumberValidation.isValid("0151 23456789", "DE"));
        assertTrue(PhoneNumberValidation.isValid("004915123456789", "DE")); // using international prefix
    }

    @Test
    @DisplayName("invalid german numbers")
    void invalid_german_numbers() {
        assertFalse(PhoneNumberValidation.isValid("12345", "DE")); // too short
        assertFalse(PhoneNumberValidation.isValid("+49 15123", "DE")); // too short
        assertFalse(PhoneNumberValidation.isValid("++4915123456789", "DE")); // invalid format
        assertFalse(PhoneNumberValidation.isValid("+++4915123456789", "DE")); // invalid format
    }

    @Test
    @DisplayName("formatted number with valid digits should be accepted")
    void formatted_number_with_valid_digits_should_be_accepted() {
        // Same German mobile number digits as valid case, but with extra separators/spaces.
        assertTrue(PhoneNumberValidation.isValid("+49 151 2345 6789", "DE"));
    }

    @Test
    @DisplayName("number with more than15 digits is rejected")
    void number_with_more_than15_digits_is_rejected() {
        assertFalse(PhoneNumberValidation.isValid("+1 202 555 0171 23456", "US"));
    }

    @Test
    @DisplayName("valid us numbers")
    void valid_us_numbers() {
        assertTrue(PhoneNumberValidation.isValid("+1 202-555-0171", "US"));
        assertTrue(PhoneNumberValidation.isValid("202-555-0171", "US"));
        assertTrue(PhoneNumberValidation.isValid("+1 415 9604264", "US"));
    }

    @Test
    @DisplayName("invalid us numbers")
    void invalid_us_numbers() {
        assertFalse(PhoneNumberValidation.isValid("123", "US"));
        assertFalse(PhoneNumberValidation.isValid("999-999-9999", "US")); // possible format but invalid number
        assertFalse(PhoneNumberValidation.isValid("\u202D+1 415 9604264\u202C", "US")); //invisible directional formatting chars
        assertFalse(PhoneNumberValidation.isValid("+1202555017123456", "US"));
    }

    @Test
    @DisplayName("null or empty input")
    void null_or_empty_input() {
        assertFalse(PhoneNumberValidation.isValid(null, "US"));
        assertFalse(PhoneNumberValidation.isValid("", "US"));
    }

    @Test
    @DisplayName("invalid region")
    void invalid_region() {
        assertFalse(PhoneNumberValidation.isValid("+4915123456789", "XX")); // invalid region
    }
}