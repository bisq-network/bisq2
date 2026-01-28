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
public final class CloseTradeRequest implements Proto {
    private final String tradeId;
    private final byte[] myOutputPeersPrvKeyShare;
    private final byte[] swapTx;

    public CloseTradeRequest(
            String tradeId,
            byte[] myOutputPeersPrvKeyShare,
            byte[] swapTx) {
        this.tradeId = tradeId;
        this.myOutputPeersPrvKeyShare = myOutputPeersPrvKeyShare;
        this.swapTx = swapTx;
    }

    @Override
    public bisq.trade.protobuf.CloseTradeRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.CloseTradeRequest.newBuilder()
                .setTradeId(tradeId)
                .setMyOutputPeersPrvKeyShare(ByteString.copyFrom(myOutputPeersPrvKeyShare))
                .setSwapTx(ByteString.copyFrom(swapTx));
    }

    @Override
    public bisq.trade.protobuf.CloseTradeRequest toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CloseTradeRequest fromProto(bisq.trade.protobuf.CloseTradeRequest proto) {
        return new CloseTradeRequest(
                proto.getTradeId(),
                proto.getMyOutputPeersPrvKeyShare().toByteArray(),
                proto.getSwapTx().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CloseTradeRequest that)) return false;

        return Objects.equals(tradeId, that.tradeId) &&
                Arrays.equals(myOutputPeersPrvKeyShare, that.myOutputPeersPrvKeyShare) &&
                Arrays.equals(swapTx, that.swapTx);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(tradeId);
        result = 31 * result + Arrays.hashCode(myOutputPeersPrvKeyShare);
        result = 31 * result + Arrays.hashCode(swapTx);
        return result;
    }
}
