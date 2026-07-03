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
public class CustomPayoutPsbt implements Proto {
    private final byte[] psbt;
    private final String txId;
    private final long buyersPayoutAmountIncludingFee;
    private final long sellersPayoutAmountIncludingFee;

    public CustomPayoutPsbt(byte[] psbt,
                            String txId,
                            long buyersPayoutAmountIncludingFee,
                            long sellersPayoutAmountIncludingFee) {
        this.psbt = psbt;
        this.txId = txId;
        this.buyersPayoutAmountIncludingFee = buyersPayoutAmountIncludingFee;
        this.sellersPayoutAmountIncludingFee = sellersPayoutAmountIncludingFee;
    }

    @Override
    public bisq.trade.protobuf.CustomPayoutPsbt.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.CustomPayoutPsbt.newBuilder()
                .setPsbt(ByteString.copyFrom(psbt))
                .setTxId(txId)
                .setBuyersPayoutAmountIncludingFee(buyersPayoutAmountIncludingFee)
                .setSellersPayoutAmountIncludingFee(sellersPayoutAmountIncludingFee);
    }

    @Override
    public bisq.trade.protobuf.CustomPayoutPsbt toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CustomPayoutPsbt fromProto(bisq.trade.protobuf.CustomPayoutPsbt proto) {
        return new CustomPayoutPsbt(proto.getPsbt().toByteArray(),
                proto.getTxId(),
                proto.getBuyersPayoutAmountIncludingFee(),
                proto.getSellersPayoutAmountIncludingFee());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomPayoutPsbt that &&
                Arrays.equals(psbt, that.psbt) &&
                Objects.equals(txId, that.txId) &&
                buyersPayoutAmountIncludingFee == that.buyersPayoutAmountIncludingFee &&
                sellersPayoutAmountIncludingFee == that.sellersPayoutAmountIncludingFee;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(psbt);
        result = 31 * result + Objects.hashCode(txId);
        result = 31 * result + Long.hashCode(buyersPayoutAmountIncludingFee);
        result = 31 * result + Long.hashCode(sellersPayoutAmountIncludingFee);
        return result;
    }
}
