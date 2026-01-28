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
    private final boolean sellerReadyToRelease;

    public SwapTxSignatureRequest(String tradeId,
                                  byte[] swapTxInputPeersPartialSignature,
                                  boolean sellerReadyToRelease) {
        this.tradeId = tradeId;
        this.swapTxInputPeersPartialSignature = swapTxInputPeersPartialSignature;
        this.sellerReadyToRelease = sellerReadyToRelease;
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignatureRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.SwapTxSignatureRequest.newBuilder()
                .setTradeId(tradeId)
                .setSwapTxInputPeersPartialSignature(ByteString.copyFrom(swapTxInputPeersPartialSignature))
                .setSellerReadyToRelease(sellerReadyToRelease);
    }

    @Override
    public bisq.trade.protobuf.SwapTxSignatureRequest toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static SwapTxSignatureRequest fromProto(bisq.trade.protobuf.SwapTxSignatureRequest proto) {
        return new SwapTxSignatureRequest(proto.getTradeId(),
                proto.getSwapTxInputPeersPartialSignature().toByteArray(),
                proto.getSellerReadyToRelease());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SwapTxSignatureRequest that)) return false;

        return Objects.equals(tradeId, that.tradeId) &&
                Arrays.equals(swapTxInputPeersPartialSignature, that.swapTxInputPeersPartialSignature) &&
                sellerReadyToRelease == that.sellerReadyToRelease;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(tradeId);
        result = 31 * result + Arrays.hashCode(swapTxInputPeersPartialSignature);
        result = 31 * result + Boolean.hashCode(sellerReadyToRelease);
        return result;
    }
}
