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

import bisq.wallets.json_rpc.JsonRpcResponse;
import com.squareup.moshi.Json;
import lombok.Getter;

public class ElementsdIssuance extends JsonRpcResponse<ElementsdIssuance.Result> {
    @Getter
    public static class Result {
        private String assetBlindingNonce;
        private String assetEntropy;
        @Json(name = "isreissuance")
        private boolean isReissuance;
        private String token;
        private String asset;
        @Json(name = "assetamount")
        private double assetAmount;
        @Json(name = "tokenamountcommitment")
        private String tokenAmountCommitment;
    }
}