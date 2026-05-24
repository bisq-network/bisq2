package bisq.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

public class LightningPreImageValidationTest {
    @Test
    @DisplayName("validate lightning invoice hash")
    public void validate_lightning_invoice_hash() {
        assertTrue(LightningPreImageValidation.isValid("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"));

        assertFalse(LightningPreImageValidation.isValid("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        assertFalse(LightningPreImageValidation.isValid("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"));
        assertFalse(LightningPreImageValidation.isValid("bc1qar0srrr7xw7b6gdk9w2v0du8gfpv4p2c5f9l5w"));
        assertFalse(LightningPreImageValidation.isValid(""));
    }
}