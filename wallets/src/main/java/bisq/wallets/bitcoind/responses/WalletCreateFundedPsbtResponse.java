package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter

public class WalletCreateFundedPsbtResponse {
    private String psbt;
    private double fee;
    private int changepos;
}
