package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LightningInvoiceValidationTest {
    @Test
    public void testValidateLightningInvoiceHash() {
        assertTrue(LightningInvoiceValidation.validateInvoice("lnbc1234567890asddfghjkl1234567890asddfghjkl1234567890asddfghjkl1234567890asddfghjkl")); // P2SH

        assertFalse(LightningInvoiceValidation.validateInvoice("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // invalid
        assertFalse(LightningInvoiceValidation.validateInvoice("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")); // Bech32
        assertFalse(LightningInvoiceValidation.validateInvoice("bc1qar0srrr7xw7b6gdk9w2v0du8gfpv4p2c5f9l5w")); // Bech32
        assertFalse(LightningInvoiceValidation.validateInvoice("")); // Empty string
    }
}