package bisq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class AddMultisigAddressResponse {
    private String address;
    private String redeemScript;
    private String descriptor;
}
