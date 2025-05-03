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

package bisq.trade.mu_sig.grpc;

import lombok.Getter;

@Getter
public class NonceSharesRequest {
    private final String tradeId;
    private final byte[] buyerOutputPeersPubKeyShare;
    private final byte[] sellerOutputPeersPubKeyShare;
    private final long depositTxFeeRate;       // sats per kwu
    private final long preparedTxFeeRate;      // sats per kwu
    private final long tradeAmount;            // sats
    private final long buyersSecurityDeposit;  // sats
    private final long sellersSecurityDeposit; // sats

    public NonceSharesRequest(String tradeId,
                              byte[] buyerOutputPeersPubKeyShare,
                              byte[] sellerOutputPeersPubKeyShare,
                              long depositTxFeeRate,
                              long preparedTxFeeRate,
                              long tradeAmount,
                              long buyersSecurityDeposit,
                              long sellersSecurityDeposit) {
        this.tradeId = tradeId;
        this.buyerOutputPeersPubKeyShare = buyerOutputPeersPubKeyShare;
        this.sellerOutputPeersPubKeyShare = sellerOutputPeersPubKeyShare;
        this.depositTxFeeRate = depositTxFeeRate;
        this.preparedTxFeeRate = preparedTxFeeRate;
        this.tradeAmount = tradeAmount;
        this.buyersSecurityDeposit = buyersSecurityDeposit;
        this.sellersSecurityDeposit = sellersSecurityDeposit;
    }
}
