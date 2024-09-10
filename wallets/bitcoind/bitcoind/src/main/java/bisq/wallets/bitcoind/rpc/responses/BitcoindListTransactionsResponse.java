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

import bisq.wallets.core.model.TransactionInfo;
import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class BitcoindListTransactionsResponse extends JsonRpcResponse<List<BitcoindListTransactionsResponse.Entry>> {
    @Getter
    public static class Entry implements TransactionInfo {
        private boolean involvesWatchonly;
        private String address;
        @Json(name = "parent_descs")
        private List<String> parentDescs;
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
        @Json(name = "txid")
        private String txId;
        @Json(name = "wtxid")
        private String wtxId;
        private String[] walletconflicts;
        private int time;
        private int timereceived;
        private String comment;
        @Json(name = "bip125-replaceable")
        private String bip125Replaceable;
        private boolean abandoned;

        @Override
        public long getAmount() {
            return doubleValueToLong(amount, 8);
        }

        @Override
        public Optional<Date> getDate() {
            return Optional.of(new Date(time * 1000L));
        }
    }

    private static long doubleValueToLong(double value, int precision) {
        double max = BigDecimal.valueOf(Long.MAX_VALUE).movePointLeft(precision).doubleValue();
        if (value > max) {
            throw new ArithmeticException("Provided value would lead to an overflow");
        }
        return BigDecimal.valueOf(value).movePointRight(precision).longValue();
    }
}
