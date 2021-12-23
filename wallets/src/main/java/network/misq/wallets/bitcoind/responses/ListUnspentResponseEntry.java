package network.misq.wallets.bitcoind.responses;

import lombok.Getter;
import lombok.Setter;

public class ListUnspentResponseEntry {
    @Getter
    @Setter
    private String txid;
    @Getter
    @Setter
    private int vout;
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String label;
    @Getter
    @Setter
    private String scriptPubKey;
    @Getter
    @Setter
    private double amount;
    @Getter
    @Setter
    private int confirmations;
    @Getter
    @Setter
    private String redeemScript;
    @Getter
    @Setter
    private String witnessScript;
    @Getter
    @Setter
    private boolean spendable;
    @Getter
    @Setter
    private boolean solvable;
    @Getter
    @Setter
    private boolean reused;
    @Getter
    @Setter
    private String desc;
    @Getter
    @Setter
    private boolean safe;
}
