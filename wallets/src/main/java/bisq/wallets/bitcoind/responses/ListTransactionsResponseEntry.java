/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.wallets.bitcoind.responses;

import bisq.wallets.model.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListTransactionsResponseEntry {
    private boolean involvesWatchonly;
    private String address;
    private String category;
    private double amount;
    private String label;
    private int vout;
    private double fee;
    private int confirmations;
    private boolean generated;
    private boolean trusted;
    private String blockhash;
    private int blockheight;
    private int blockindex;
    private int blocktime;
    private String txid;
    private String[] walletconflicts;
    private int time;
    private int timereceived;
    private String comment;
    private String bip125Replaceable;
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
