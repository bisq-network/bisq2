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
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class PubKeySharesResponse implements Proto {
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
        return getBuilder(serializeForHash).build();
    }

    public static PubKeySharesResponse fromProto(bisq.trade.protobuf.PubKeySharesResponse proto) {
        return new PubKeySharesResponse(proto.getBuyerOutputPubKeyShare().toByteArray(),
                proto.getSellerOutputPubKeyShare().toByteArray(),
                proto.getCurrentBlockHeight());
    }
}
