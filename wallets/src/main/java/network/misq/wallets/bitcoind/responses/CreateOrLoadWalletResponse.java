package network.misq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;

public class CreateOrLoadWalletResponse extends BitcoindWalletResponse {

    @Getter
    @Setter
    private String name;

    public CreateOrLoadWalletResponse() {
    }

    public void validate(String walletDirPath) throws RpcCallFailureException {
        if (!isSuccess(walletDirPath)) {
            throw new RpcCallFailureException(warning);
        }
    }

    public boolean isSuccess(String walletDirPath) {
        return name.equals(walletDirPath) && hasWarning();
    }
}
