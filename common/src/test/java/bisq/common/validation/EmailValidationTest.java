package bisq.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmailValidationTest {
    @Test
    @DisplayName("valid emails")
    void valid_emails() {
        assertTrue(EmailValidation.isValid("user@example.com"));
        assertTrue(EmailValidation.isValid("user.name+tag@sub.domain.co.uk"));
        assertTrue(EmailValidation.isValid("u.ser_name123@domain.io"));
        assertTrue(EmailValidation.isValid("user-name@domain.co"));
        assertTrue(EmailValidation.isValid("user123@domain.travel"));
    }

    @Test
    @DisplayName("invalid emails")
    void invalid_emails() {
        assertFalse(EmailValidation.isValid(null));
        assertFalse(EmailValidation.isValid(""));
        assertFalse(EmailValidation.isValid("ss"));
        assertFalse(EmailValidation.isValid("plainaddress"));
        assertFalse(EmailValidation.isValid("@no-local-part.com"));
        assertFalse(EmailValidation.isValid("user@.nodomain"));
        assertFalse(EmailValidation.isValid("user@domain..com"));
        assertFalse(EmailValidation.isValid("user@domain.c")); // too short TLD
        assertFalse(EmailValidation.isValid("user@@domain.com"));
        assertFalse(EmailValidation.isValid(".a@x.com"));
        assertFalse(EmailValidation.isValid("a.@x.com"));
        assertFalse(EmailValidation.isValid("user@domain.com."));
        assertFalse(EmailValidation.isValid("user@-domain.com"));
        assertFalse(EmailValidation.isValid("user@domain-.com"));
        assertFalse(EmailValidation.isValid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.com\n"));
    }
}