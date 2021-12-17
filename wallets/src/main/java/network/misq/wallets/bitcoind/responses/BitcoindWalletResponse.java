package network.misq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public abstract class BitcoindWalletResponse {

    @Getter
    @Setter
    protected String warning;

    public BitcoindWalletResponse() {
    }

    public boolean hasWarning() {
        return warning.isEmpty();
    }
}
