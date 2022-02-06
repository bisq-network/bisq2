package bisq.wallets.bitcoind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindWalletCreationIntegrationTests extends SharedBitcoindInstanceTests {
    @Test
    public void createFreshWallet() {
        assertTrue(walletFilePath.toFile().exists());
        assertEquals(0, minerWalletBackend.getBalance());
    }

    @Test
    public void loadWalletIfExisting() {
        assertTrue(walletFilePath.toFile().exists());

        minerChainBackend.unloadWallet(walletFilePath);
        minerChainBackend.createOrLoadWallet(walletFilePath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        assertEquals(0, minerWalletBackend.getBalance());
        assertTrue(walletFilePath.toFile().exists());
    }
}
