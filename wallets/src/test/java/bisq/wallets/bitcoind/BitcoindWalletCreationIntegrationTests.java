package bisq.wallets.bitcoind;

import bisq.wallets.bitcoind.rpc.RpcCallFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoindWalletCreationIntegrationTests extends SharedBitcoindInstanceTests {
    @Test
    public void createFreshWallet() throws RpcCallFailureException {
        assertTrue(walletFilePath.toFile().exists());
        assertEquals(0, minerWalletBackend.getBalance());
    }

    @Test
    public void loadWalletIfExisting() throws RpcCallFailureException {
        assertTrue(walletFilePath.toFile().exists());

        minerChainBackend.unloadWallet(walletFilePath);
        minerChainBackend.createOrLoadWallet(walletFilePath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        assertEquals(0, minerWalletBackend.getBalance());
        assertTrue(walletFilePath.toFile().exists());
    }
}
