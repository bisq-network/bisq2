package network.misq.wallets.bitcoind.psbt;

import lombok.Getter;
import lombok.Setter;

public class PsbtInput {
    @Getter
    @Setter
    private String txid;
    @Getter
    @Setter
    private int vout;
}
