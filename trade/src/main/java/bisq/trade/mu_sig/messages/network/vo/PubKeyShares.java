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

package bisq.trade.mu_sig.messages.network.vo;

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class PubKeyShares implements Proto {
    private final byte[] buyerOutputPubKeyShare;
    private final byte[] sellerOutputPubKeyShare;

    public PubKeyShares(byte[] buyerOutputPubKeyShare,
                        byte[] sellerOutputPubKeyShare) {
        this.buyerOutputPubKeyShare = buyerOutputPubKeyShare;
        this.sellerOutputPubKeyShare = sellerOutputPubKeyShare;
    }

    @Override
    public bisq.trade.protobuf.PubKeyShares.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.PubKeyShares.newBuilder()
                .setBuyerOutputPubKeyShare(ByteString.copyFrom(buyerOutputPubKeyShare))
                .setSellerOutputPubKeyShare(ByteString.copyFrom(sellerOutputPubKeyShare));
    }

    @Override
    public bisq.trade.protobuf.PubKeyShares toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static PubKeyShares fromProto(bisq.trade.protobuf.PubKeyShares proto) {
        return new PubKeyShares(proto.getBuyerOutputPubKeyShare().toByteArray(),
                proto.getSellerOutputPubKeyShare().toByteArray());
    }
}
