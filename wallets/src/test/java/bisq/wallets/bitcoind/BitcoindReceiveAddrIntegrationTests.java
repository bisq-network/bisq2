package bisq.wallets.bitcoind;

import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.rpc.RpcCallFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindReceiveAddrIntegrationTests extends SharedBitcoindInstanceTests {

    @Test
    void getNewLegacyAddress() throws RpcCallFailureException {
        String address = minerWalletBackend.getNewAddress(AddressType.LEGACY, "");
        assertTrue(address.startsWith("m") || address.startsWith("n"), address);
    }

    @Test
    void getNewP2ShSegwitAddress() throws RpcCallFailureException {
        String address = minerWalletBackend.getNewAddress(AddressType.P2SH_SEGWIT, "");
        assertTrue(address.startsWith("2"), address);
    }

    @Test
    void getNewBech32Address() throws RpcCallFailureException {
        String address = minerWalletBackend.getNewAddress(AddressType.BECH32, "");
        assertTrue(address.startsWith("bcr"), address);
    }
}
