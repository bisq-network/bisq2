package bisq.wallets.bitcoind.responses;

import bisq.wallets.model.Utxo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ListUnspentResponseEntry {
    private String txid;
    private int vout;
    private String address;
    private String label;
    private String scriptPubKey;
    private double amount;
    private int confirmations;
    private String redeemScript;
    private String witnessScript;
    private boolean spendable;
    private boolean solvable;
    private boolean reused;
    private String desc;
    private boolean safe;

    public Utxo toUtxo() {
        return new Utxo.Builder()
                .txId(txid)
                .address(address)
                .amount(amount)
                .confirmations(confirmations)
                .reused(reused)
                .build();
    }
}
