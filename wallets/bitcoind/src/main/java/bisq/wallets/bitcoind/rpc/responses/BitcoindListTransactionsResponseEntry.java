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

package bisq.wallets.bitcoind.rpc.responses;

import bisq.wallets.core.model.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitcoindListTransactionsResponseEntry implements Transaction {
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
    @JsonProperty("txid")
    private String txId;
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
}
