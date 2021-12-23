package network.misq.wallets.bitcoind;

import network.misq.common.util.FileUtils;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;
import network.misq.wallets.bitcoind.rpc.RpcClient;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SharedBitcoindInstanceTests {
    protected RpcClient rpcClient;
    protected BitcoindProcess bitcoindProcess;

    protected BitcoindChainBackend minerChainBackend;
    protected BitcoindWalletBackend minerWalletBackend;

    protected Path tmpDirPath;
    protected Path walletFilePath;

    @BeforeAll
    public void startBitcoind() throws IOException {
        bitcoindProcess = BitcoindRegtestSetup.createAndStartBitcoind();
    }

    @AfterAll
    public void stopBitcoind() {
        bitcoindProcess.stopAndWaitUntilStopped();
    }

    @BeforeEach
    public void setUp() throws IOException, RpcCallFailureException {
        tmpDirPath = FileUtils.createTempDir();
        walletFilePath = tmpDirPath.resolve("wallet");
        assertFalse(walletFilePath.toFile().exists());

        rpcClient = new RpcClient(BitcoindRegtestSetup.RPC_CONFIG);
        minerChainBackend = new BitcoindChainBackend(rpcClient);
        minerChainBackend.createOrLoadWallet(walletFilePath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        RpcClient walletRpcClient = BitcoindRegtestSetup.createWalletRpcClient(walletFilePath);
        minerWalletBackend = new BitcoindWalletBackend(walletRpcClient);
        minerWalletBackend.walletPassphrase(BitcoindRegtestSetup.WALLET_PASSPHRASE, BitcoindWalletBackend.DEFAULT_WALLET_TIMEOUT);
    }

    @AfterEach
    public void cleanUp() throws RpcCallFailureException {
        minerChainBackend.unloadWallet(walletFilePath);
    }
}
