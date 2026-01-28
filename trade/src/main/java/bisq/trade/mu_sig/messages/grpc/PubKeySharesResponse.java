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

@Getter
public final class PubKeySharesResponse implements Proto {
    private final byte[] buyerOutputPubKeyShare;
    private final byte[] sellerOutputPubKeyShare;
    private final int currentBlockHeight;

    public PubKeySharesResponse(byte[] buyerOutputPubKeyShare,
                                byte[] sellerOutputPubKeyShare,
                                int currentBlockHeight) {
        this.buyerOutputPubKeyShare = buyerOutputPubKeyShare;
        this.sellerOutputPubKeyShare = sellerOutputPubKeyShare;
        this.currentBlockHeight = currentBlockHeight;
    }

    @Override
    public bisq.trade.protobuf.PubKeySharesResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.PubKeySharesResponse.newBuilder()
                .setBuyerOutputPubKeyShare(ByteString.copyFrom(buyerOutputPubKeyShare))
                .setSellerOutputPubKeyShare(ByteString.copyFrom(sellerOutputPubKeyShare))
                .setCurrentBlockHeight(currentBlockHeight);
    }

    @Override
    public bisq.trade.protobuf.PubKeySharesResponse toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static PubKeySharesResponse fromProto(bisq.trade.protobuf.PubKeySharesResponse proto) {
        return new PubKeySharesResponse(proto.getBuyerOutputPubKeyShare().toByteArray(),
                proto.getSellerOutputPubKeyShare().toByteArray(),
                proto.getCurrentBlockHeight());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PubKeySharesResponse that)) return false;

        return currentBlockHeight == that.currentBlockHeight &&
                Arrays.equals(buyerOutputPubKeyShare, that.buyerOutputPubKeyShare) &&
                Arrays.equals(sellerOutputPubKeyShare, that.sellerOutputPubKeyShare);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(buyerOutputPubKeyShare);
        result = 31 * result + Arrays.hashCode(sellerOutputPubKeyShare);
        result = 31 * result + currentBlockHeight;
        return result;
    }
}
