package bisq.wallets.bitcoind.responses;

import bisq.wallets.bitcoind.rpc.RpcCallFailureException;
import lombok.Getter;
import lombok.Setter;

public class CreateOrLoadWalletResponse extends BitcoindWalletResponse {

    @Getter
    @Setter
    private String name;

    public CreateOrLoadWalletResponse() {
    }

    public void validate(String walletDirPath) {
        if (!isSuccess(walletDirPath)) {
            throw new RpcCallFailureException(warning);
        }
    }

    public boolean isSuccess(String walletDirPath) {
        return name.equals(walletDirPath) && hasWarning();
    }
}
