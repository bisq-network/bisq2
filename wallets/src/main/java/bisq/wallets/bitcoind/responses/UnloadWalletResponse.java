package bisq.wallets.bitcoind.responses;

import bisq.wallets.exceptions.RpcCallFailureException;

public class UnloadWalletResponse extends BitcoindWalletResponse {

    public void validate() {
        if (!isSuccess()) {
            throw new RpcCallFailureException(warning);
        }
    }

    public boolean isSuccess() {
        return hasWarning();
    }
}