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
public final class SwapTxSignatureRequest implements Proto {
    private final String tradeId;
    private final byte[] swapTxInputPeersPartialSignature;

    public SwapTxSignatureRequest(String tradeId, byte[] swapTxInputPeersPartialSignature) {
        this.tradeId = tradeId;
        this.swapTxInputPeersPartialSignature = swapTxInputPeersPartialSignature;
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignatureRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.SwapTxSignatureRequest.newBuilder()
                .setTradeId(tradeId)
                .setSwapTxInputPeersPartialSignature(ByteString.copyFrom(swapTxInputPeersPartialSignature));
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignatureRequest toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static SwapTxSignatureRequest fromProto(bisq.trade.protobuf.SwapTxSignatureRequest proto) {
        return new SwapTxSignatureRequest(proto.getTradeId(), proto.getSwapTxInputPeersPartialSignature().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SwapTxSignatureRequest that)) return false;

        return Objects.equals(tradeId, that.tradeId) &&
                Arrays.equals(swapTxInputPeersPartialSignature, that.swapTxInputPeersPartialSignature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(tradeId);
        result = 31 * result + Arrays.hashCode(swapTxInputPeersPartialSignature);
        return result;
    }
}
