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

package bisq.trade.mu_sig.messages.network.mu_sig_data;

import bisq.common.proto.NetworkProto;
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;

@Getter
public final class SwapTxSignature implements NetworkProto {
    public static SwapTxSignature from(SwapTxSignatureResponse swapTxSignatureResponse) {
        return new SwapTxSignature(swapTxSignatureResponse.getSwapTx(),
                swapTxSignatureResponse.getPeerOutputPrvKeyShare());
    }

    private final byte[] swapTx;
    private final byte[] peerOutputPrvKeyShare;

    private SwapTxSignature(byte[] swapTx, byte[] peerOutputPrvKeyShare) {
        this.swapTx = swapTx;
        this.peerOutputPrvKeyShare = peerOutputPrvKeyShare;

        verify();
    }

    @Override
    public void verify() {
        // TODO
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignature.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.SwapTxSignature.newBuilder()
                .setSwapTx(ByteString.copyFrom(swapTx))
                .setPeerOutputPrvKeyShare(ByteString.copyFrom(peerOutputPrvKeyShare));
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignature toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static SwapTxSignature fromProto(bisq.trade.protobuf.SwapTxSignature proto) {
        return new SwapTxSignature(proto.getSwapTx().toByteArray(), proto.getPeerOutputPrvKeyShare().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SwapTxSignature that)) return false;

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
