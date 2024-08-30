package bisq.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BitcoinTransactionValidationTest {
    @Test
    public void testValidateTransactionId() {
        assertTrue(BitcoinTransactionValidation.validateTransactionId("4b1ecb5cf2b6f254dc2b82c4e6a6c44f11b1e9d35cce63143142b6eae3c721c2")); // Valid TXID
        assertTrue(BitcoinTransactionValidation.validateTransactionId("4b1fcb5cf2b6f254dc2b82c4e6a6c14f11b1e9d33cce63143142b6e3e3c721c2")); // Valid TXID

        assertFalse(BitcoinTransactionValidation.validateTransactionId("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0")); // Invalid TXID
        assertFalse(BitcoinTransactionValidation.validateTransactionId("InvalidAddressOrTxId")); // Invalid address or TXID
        assertFalse(BitcoinTransactionValidation.validateTransactionId("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t01")); // Invalid TXID
        assertFalse(BitcoinTransactionValidation.validateTransactionId("")); // Empty string
    }
}