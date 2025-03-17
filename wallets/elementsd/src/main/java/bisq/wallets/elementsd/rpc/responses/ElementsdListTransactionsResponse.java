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

package bisq.wallets.elementsd.rpc.responses;

import bisq.wallets.bitcoind.rpc.responses.BitcoindListTransactionsResponse;
import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;

import java.util.List;

public class ElementsdListTransactionsResponse extends JsonRpcResponse<List<ElementsdListTransactionsResponse.Entry>> {
    @Getter
    public static class Entry extends BitcoindListTransactionsResponse.Entry {
        @Json(name = "amountblinder")
        private String amountBlinder;
        private String asset;
        @Json(name = "assetblinder")
        private String assetBlinder;
    }
}
