package network.misq.wallets.bitcoind.responses;

import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;

public class UnloadWalletResponse extends BitcoindWalletResponse {

    public void validate() throws RpcCallFailureException {
        if (!isSuccess()) {
            throw new RpcCallFailureException(warning);
        }
    }

    public boolean isSuccess() {
        return hasWarning();
    }
}