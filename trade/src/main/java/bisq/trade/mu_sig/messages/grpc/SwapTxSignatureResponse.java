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
public final class SwapTxSignatureResponse implements Proto {
    private final byte[] swapTx;
    private final byte[] peerOutputPrvKeyShare;

    public SwapTxSignatureResponse(byte[] swapTx, byte[] peerOutputPrvKeyShare) {
        this.swapTx = swapTx;
        this.peerOutputPrvKeyShare = peerOutputPrvKeyShare;
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignatureResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.SwapTxSignatureResponse.newBuilder()
                .setSwapTx(ByteString.copyFrom(swapTx))
                .setPeerOutputPrvKeyShare(ByteString.copyFrom(peerOutputPrvKeyShare));
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignatureResponse toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static SwapTxSignatureResponse fromProto(bisq.trade.protobuf.SwapTxSignatureResponse proto) {
        return new SwapTxSignatureResponse(proto.getSwapTx().toByteArray(), proto.getPeerOutputPrvKeyShare().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SwapTxSignatureResponse that)) return false;

        return Arrays.equals(swapTx, that.swapTx) &&
                Arrays.equals(peerOutputPrvKeyShare, that.peerOutputPrvKeyShare);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(swapTx);
        result = 31 * result + Arrays.hashCode(peerOutputPrvKeyShare);
        return result;
    }
}
