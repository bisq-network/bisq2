package bisq.wallets.bitcoind.rpc;

import java.nio.file.Path;

public record WalletRpcConfig(RpcConfig rpcConfig, Path walletPath) {
}
