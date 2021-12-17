package network.misq.wallets.bitcoind;

import network.misq.wallets.AddressType;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindSigningTests extends SharedBitcoindInstanceTests {
    private static final String MESSAGE = "my message";

    @Test
    public void signAndVerifyMessage() throws RpcCallFailureException {
        String address = minerWalletBackend.getNewAddress(AddressType.LEGACY, "");
        String signature = minerWalletBackend.signMessage(address, MESSAGE);
        boolean isValid = minerWalletBackend.verifyMessage(address, signature, MESSAGE);
        assertTrue(isValid);
    }
}
