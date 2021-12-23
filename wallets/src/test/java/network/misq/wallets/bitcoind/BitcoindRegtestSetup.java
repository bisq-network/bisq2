package network.misq.wallets.bitcoind;

import network.misq.common.util.FileUtils;
import network.misq.wallets.AddressType;
import network.misq.wallets.NetworkType;
import network.misq.wallets.bitcoind.responses.ListUnspentResponseEntry;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;
import network.misq.wallets.bitcoind.rpc.RpcClient;
import network.misq.wallets.bitcoind.rpc.RpcConfig;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BitcoindRegtestSetup {
    public static final String WALLET_PASSPHRASE = "My super secret passphrase that nobody can guess.";

    public static final RpcConfig RPC_CONFIG = new RpcConfig.Builder()
            .networkType(NetworkType.REGTEST)
            .hostname("127.0.0.1")
            .user("bisq")
            .password("bisq")
            .build();

    public static BitcoindProcess createAndStartBitcoind() throws IOException {
        Path bitcoindDataDir = FileUtils.createTempDir();
        var bitcoindProcess = new BitcoindProcess(
                BitcoindRegtestSetup.RPC_CONFIG,
                bitcoindDataDir
        );
        bitcoindProcess.startAndWaitUntilReady();
        return bitcoindProcess;
    }

    public static BitcoindWalletBackend createTestWalletBackend(BitcoindChainBackend chainBackend,
                                                                Path tmpDirPath,
                                                                String walletName) throws MalformedURLException, RpcCallFailureException {
        Path receiverWalletPath = tmpDirPath.resolve(walletName);
        RpcClient receiverWalletRpc = createWalletRpcClient(receiverWalletPath);

        chainBackend.createOrLoadWallet(receiverWalletPath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        var walletBackend = new BitcoindWalletBackend(receiverWalletRpc);
        walletBackend.walletPassphrase(BitcoindRegtestSetup.WALLET_PASSPHRASE, BitcoindWalletBackend.DEFAULT_WALLET_TIMEOUT);
        return walletBackend;
    }

    public static RpcClient createWalletRpcClient(Path walletFilePath) throws MalformedURLException {
        RpcConfig walletConfig = new RpcConfig.Builder(BitcoindRegtestSetup.RPC_CONFIG)
                .walletName(walletFilePath.toAbsolutePath().toString())
                .build();
        return new RpcClient(walletConfig);
    }

    public static Optional<ListUnspentResponseEntry> filterUtxosByAddress(List<ListUnspentResponseEntry> utxos, String address) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getAddress(), address))
                .findFirst();
    }

    public static void mineInitialRegtestBlocks(BitcoindChainBackend minerChainBackend, BitcoindWalletBackend minerBackend) throws RpcCallFailureException {
        String address = minerBackend.getNewAddress(AddressType.BECH32, "");
        minerChainBackend.generateToAddress(101, address);
    }

    public static void mineOneBlock(BitcoindChainBackend minerChainBackend, BitcoindWalletBackend minerBackend) throws RpcCallFailureException {
        String minerAddress = minerBackend.getNewAddress(AddressType.BECH32, "");
        minerChainBackend.generateToAddress(1, minerAddress);
    }

    public static String sendBtcAndMineOneBlock(BitcoindChainBackend minerChainBackend,
                                                BitcoindWalletBackend minerWalletBackend,
                                                BitcoindWalletBackend receiverBackend,
                                                double amount) throws RpcCallFailureException {
        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWalletBackend.sendToAddress(receiverAddress, amount);

        mineOneBlock(minerChainBackend, minerWalletBackend);
        return receiverAddress;
    }
}
