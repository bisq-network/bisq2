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

    @Test
    public void testCanonicalize() {
        // bitcoin: prefix stripped (#4782) and bech32 lowercased (#4712) to match the explorer output
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressValidation.canonicalize("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"));
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressValidation.canonicalize("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"));
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressValidation.canonicalize("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"));
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressValidation.canonicalize("BITCOIN:BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"));

        // base58 is case-sensitive: never altered (prefix/params still stripped)
        assertEquals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
                BitcoinAddressValidation.canonicalize("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy",
                BitcoinAddressValidation.canonicalize("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy?amount=1"));
    }

    // Wallets encode QR codes in upper case, because QR codes are more compact then. A base58 address is case-sensitive.
    @Test
    public void testNormalizeScannedPaymentData() {
        assertEquals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
                BitcoinAddressValidation.normalizeScannedPaymentData("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy",
                BitcoinAddressValidation.normalizeScannedPaymentData("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy?amount=1"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy",
                BitcoinAddressValidation.normalizeScannedPaymentData("Bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"));
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressValidation.normalizeScannedPaymentData("BITCOIN:BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4?AMOUNT=0.1"));
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressValidation.normalizeScannedPaymentData("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4"));
        assertEquals("lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq",
                BitcoinAddressValidation.normalizeScannedPaymentData("LNBC2500U1PVJLUEZPP5QQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQYPQ"));
    }
}