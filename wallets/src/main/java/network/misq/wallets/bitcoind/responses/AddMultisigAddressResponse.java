package network.misq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public class AddMultisigAddressResponse {
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String redeemScript;
    @Getter
    @Setter
    private String descriptor;
}
