package bisq.common.validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoinDataValidationTest {

    @Test
    public void testValidateWalletAddressHash() {
        assertTrue(BitcoinDataValidation.validateWalletAddressHash("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // P2PKH
        assertTrue(BitcoinDataValidation.validateWalletAddressHash("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy")); // P2SH
        assertTrue(BitcoinDataValidation.validateWalletAddressHash("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")); // Bech32
        assertTrue(BitcoinDataValidation.validateWalletAddressHash("bc1qar0srrr7xw7b6gdk9w2v0du8gfpv4p2c5f9l5w")); // Bech32
        assertFalse(BitcoinDataValidation.validateWalletAddressHash("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t40")); // Invalid Bech32
        assertFalse(BitcoinDataValidation.validateWalletAddressHash("4A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")); // Invalid address
        assertFalse(BitcoinDataValidation.validateWalletAddressHash("")); // Empty string
    }

    @Test
    public void testValidateTransactionId() {
        assertTrue(BitcoinDataValidation.validateTransactionId("4b1ecb5cf2b6f254dc2b82c4e6a6c44f11b1e9d35cce63143142b6eae3c721c2")); // Valid TXID
        assertTrue(BitcoinDataValidation.validateTransactionId("4b1fcb5cf2b6f254dc2b82c4e6a6c14f11b1e9d33cce63143142b6e3e3c721c2")); // Valid TXID
        assertFalse(BitcoinDataValidation.validateTransactionId("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0")); // Valid TXID
        assertFalse(BitcoinDataValidation.validateTransactionId("InvalidAddressOrTxId")); // Invalid address or TXID
        assertFalse(BitcoinDataValidation.validateTransactionId("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t01")); // Invalid TXID
        assertFalse(BitcoinDataValidation.validateTransactionId("")); // Empty string
    }
}