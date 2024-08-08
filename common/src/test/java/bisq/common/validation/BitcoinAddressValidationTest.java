package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BitcoinAddressValidationTest {
    @Test
    public void testValidateWalletAddressHash() {
        assertTrue(BitcoinAddressValidation.validateAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // P2PKH
        assertTrue(BitcoinAddressValidation.validateAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy")); // P2SH
        assertTrue(BitcoinAddressValidation.validateAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")); // Bech32
        assertTrue(BitcoinAddressValidation.validateAddress("bc1qar0srrr7xw7b6gdk9w2v0du8gfpv4p2c5f9l5w")); // Bech32

        assertFalse(BitcoinAddressValidation.validateAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t40")); // Invalid Bech32
        assertFalse(BitcoinAddressValidation.validateAddress("4A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // Invalid address
        assertFalse(BitcoinAddressValidation.validateAddress("")); // Empty string
    }
}