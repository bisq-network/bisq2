package bisq.wallets.bitcoind.psbt;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PsbtInput {
    private String txid;
    private int vout;
}
