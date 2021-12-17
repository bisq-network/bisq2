package network.misq.wallets.bitcoind;

import network.misq.wallets.AddressType;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindReceiveAddrTests extends SharedBitcoindInstanceTests {

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
