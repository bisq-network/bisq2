package bisq.wallets.bitcoind;

import bisq.wallets.AddressType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindReceiveAddrIntegrationTests extends SharedBitcoindInstanceTests {

    @Test
    void getNewLegacyAddress() {
        String address = minerWalletBackend.getNewAddress(AddressType.LEGACY, "");
        assertTrue(address.startsWith("m") || address.startsWith("n"), address);
    }

    @Test
    void getNewP2ShSegwitAddress() {
        String address = minerWalletBackend.getNewAddress(AddressType.P2SH_SEGWIT, "");
        assertTrue(address.startsWith("2"), address);
    }

    @Test
    void getNewBech32Address() {
        String address = minerWalletBackend.getNewAddress(AddressType.BECH32, "");
        assertTrue(address.startsWith("bcr"), address);
    }
}
