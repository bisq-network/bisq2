package bisq.wallets.bitcoind;

import bisq.wallets.AddressType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindSigningIntegrationTests extends SharedBitcoindInstanceTests {
    private static final String MESSAGE = "my message";

    @Test
    public void signAndVerifyMessage() {
        String address = minerWalletBackend.getNewAddress(AddressType.LEGACY, "");
        String signature = minerWalletBackend.signMessage(address, MESSAGE);
        boolean isValid = minerWalletBackend.verifyMessage(address, signature, MESSAGE);
        assertTrue(isValid);
    }
}
