package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalizePsbtResponse {
    private String psbt;
    private String hex;
    private boolean complete;
}
