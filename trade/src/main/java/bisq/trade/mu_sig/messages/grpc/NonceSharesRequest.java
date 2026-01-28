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

package bisq.trade.mu_sig.messages.grpc;

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public final class NonceSharesRequest implements Proto {
    private final String tradeId;
    private final byte[] buyerOutputPeersPubKeyShare;
    private final byte[] sellerOutputPeersPubKeyShare;
    private final long depositTxFeeRate;
    private final long preparedTxFeeRate;
    private final long tradeAmount;
    private final long buyersSecurityDeposit;
    private final long sellersSecurityDeposit;

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

    @Override
    public bisq.trade.protobuf.NonceSharesRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.NonceSharesRequest.newBuilder()
                .setTradeId(tradeId)
                .setBuyerOutputPeersPubKeyShare(ByteString.copyFrom(buyerOutputPeersPubKeyShare))
                .setSellerOutputPeersPubKeyShare(ByteString.copyFrom(sellerOutputPeersPubKeyShare))
                .setDepositTxFeeRate(depositTxFeeRate)
                .setPreparedTxFeeRate(preparedTxFeeRate)
                .setTradeAmount(tradeAmount)
                .setBuyersSecurityDeposit(buyersSecurityDeposit)
                .setSellersSecurityDeposit(sellersSecurityDeposit);
    }

    @Override
    public bisq.trade.protobuf.NonceSharesRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static NonceSharesRequest fromProto(bisq.trade.protobuf.NonceSharesRequest proto) {
        return new NonceSharesRequest(proto.getTradeId(),
                proto.getBuyerOutputPeersPubKeyShare().toByteArray(),
                proto.getSellerOutputPeersPubKeyShare().toByteArray(),
                proto.getDepositTxFeeRate(),
                proto.getPreparedTxFeeRate(),
                proto.getTradeAmount(),
                proto.getBuyersSecurityDeposit(),
                proto.getSellersSecurityDeposit());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NonceSharesRequest that)) return false;

        return depositTxFeeRate == that.depositTxFeeRate &&
                preparedTxFeeRate == that.preparedTxFeeRate &&
                tradeAmount == that.tradeAmount &&
                buyersSecurityDeposit == that.buyersSecurityDeposit &&
                sellersSecurityDeposit == that.sellersSecurityDeposit &&
                Objects.equals(tradeId, that.tradeId) &&
                Arrays.equals(buyerOutputPeersPubKeyShare, that.buyerOutputPeersPubKeyShare) &&
                Arrays.equals(sellerOutputPeersPubKeyShare, that.sellerOutputPeersPubKeyShare);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(tradeId);
        result = 31 * result + Arrays.hashCode(buyerOutputPeersPubKeyShare);
        result = 31 * result + Arrays.hashCode(sellerOutputPeersPubKeyShare);
        result = 31 * result + Long.hashCode(depositTxFeeRate);
        result = 31 * result + Long.hashCode(preparedTxFeeRate);
        result = 31 * result + Long.hashCode(tradeAmount);
        result = 31 * result + Long.hashCode(buyersSecurityDeposit);
        result = 31 * result + Long.hashCode(sellersSecurityDeposit);
        return result;
    }
}