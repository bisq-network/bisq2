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

package bisq.wallets.electrum.rpc.responses;

import bisq.common.monetary.Coin;
import bisq.wallets.core.model.TransactionInfo;
import com.squareup.moshi.Json;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@ToString
@Getter
public class ElectrumOnChainTransactionResponse implements TransactionInfo {
    @Json(name = "bc_balance")
    private String bcBalance;
    @Json(name = "bc_value")
    private String bcValue;
    private int confirmations;

    private String date;

    private String fee;

    @Json(name = "fee_sat")
    private Long feeSat;

    private int height;
    private boolean incoming;
    private String label;

    @Json(name = "monotonic_timestamp")
    private long monotonicTimestamp;
    private Long timestamp;

    @Json(name = "txid")
    private String txId;
    @Json(name = "txpos_in_block")
    private Integer txPosInBlock;

    @Override
    public Coin getAmount() {
        return Coin.parseBtc(bcValue);
    }

    @Override
    public Date getDate() {
        return timestamp != null ? new Date(timestamp * 1000L) : new Date(0);
    }
}
