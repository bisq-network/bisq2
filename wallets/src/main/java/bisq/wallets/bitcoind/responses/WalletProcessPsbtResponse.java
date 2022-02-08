package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class WalletProcessPsbtResponse {
    private String psbt;
    private boolean complete;
}
