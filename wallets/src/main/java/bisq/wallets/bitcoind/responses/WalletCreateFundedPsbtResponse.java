package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public class WalletCreateFundedPsbtResponse {
    @Getter
    @Setter
    private String psbt;
    @Getter
    @Setter
    private double fee;
    @Getter
    @Setter
    private int changepos;
}
