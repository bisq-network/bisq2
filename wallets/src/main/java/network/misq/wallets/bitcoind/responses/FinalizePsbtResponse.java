package network.misq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public class FinalizePsbtResponse {
    @Getter
    @Setter
    private String psbt;
    @Getter
    @Setter
    private String hex;
    @Getter
    @Setter
    private boolean complete;
}
