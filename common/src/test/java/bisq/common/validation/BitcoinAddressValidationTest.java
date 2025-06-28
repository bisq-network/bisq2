package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BitcoinAddressValidationTest {
    @Test
    public void testValidateWalletAddressHash() {
        assertTrue(BitcoinAddressValidation.isValid("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // P2PKH
        assertTrue(BitcoinAddressValidation.isValid("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy")); // P2SH
        assertTrue(BitcoinAddressValidation.isValid("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")); // Bech32
        assertTrue(BitcoinAddressValidation.isValid("bc1qar0srrr7xw7b6gdk9w2v0du8gfpv4p2c5f9l5w")); // Bech32
        assertTrue(BitcoinAddressValidation.isValid("bc1qce2xmvr7h3lev3l3ecc2uxcsac7t26wppagkngat2s7x4ah7peesy4fz4q")); // Bech32

        assertFalse(BitcoinAddressValidation.isValid("1bc1qce2xmvr7h3lev3l3ecc2uxcsac7t26wppagkngat2s7x4ah7peesy4fz4q"));
        assertFalse(BitcoinAddressValidation.isValid("3bc1qce2xmvr7h3lev3l3ecc2uxcsac7t26wppagkngat2s7x4ah7peesy4fz4q"));

        assertFalse(BitcoinAddressValidation.isValid("1IOIOIOIOIOIOIOIOIOIOIOIOIOIOIOIOIOIOIOI")); // Invalid address
        assertFalse(BitcoinAddressValidation.isValid("4A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // Invalid address
        assertFalse(BitcoinAddressValidation.isValid("")); // Empty string
    }
}