package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public class WalletProcessPsbtResponse {
    @Getter
    @Setter
    private String psbt;
    @Getter
    @Setter
    private boolean complete;
}
