package bisq.wallets.bitcoind.responses;

import bisq.wallets.model.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class ListTransactionsResponseEntry {
    @Getter
    @Setter
    private boolean involvesWatchonly;
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String category;
    @Getter
    @Setter
    private double amount;
    @Getter
    @Setter
    private String label;
    @Getter
    @Setter
    private int vout;
    @Getter
    @Setter
    private double fee;
    @Getter
    @Setter
    private int confirmations;
    @Getter
    @Setter
    private boolean generated;
    @Getter
    @Setter
    private boolean trusted;
    @Getter
    @Setter
    private String blockhash;
    @Getter
    @Setter
    private int blockheight;
    @Getter
    @Setter
    private int blockindex;
    @Getter
    @Setter
    private int blocktime;
    @Getter
    @Setter
    private String txid;
    @Getter
    @Setter
    private String[] walletconflicts;
    @Getter
    @Setter
    private int time;
    @Getter
    @Setter
    private int timereceived;
    @Getter
    @Setter
    private String comment;
    private String bip125Replaceable;
    @Getter
    @Setter
    private boolean abandoned;

    @JsonProperty("bip125-replaceable")
    public String getBip125Replaceable() {
        return bip125Replaceable;
    }

    public void setBip125Replaceable(String bip125Replaceable) {
        this.bip125Replaceable = bip125Replaceable;
    }

    public Transaction toTransaction() {
        return new Transaction.Builder()
                .txId(txid)
                .address(address)
                .amount(amount)
                .confirmations(confirmations)
                .build();
    }
}
