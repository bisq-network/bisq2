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
import bisq.trade.mu_sig.messages.network.vo.PartialSignatures;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;

@Getter
public final class PartialSignaturesMessage implements Proto {
    public static PartialSignaturesMessage from(PartialSignatures peersPartialSignatures) {
        return new PartialSignaturesMessage(
                peersPartialSignatures.getPeersWarningTxBuyerInputPartialSignature(),
                peersPartialSignatures.getPeersWarningTxSellerInputPartialSignature(),
                peersPartialSignatures.getPeersRedirectTxInputPartialSignature(),
                peersPartialSignatures.getSwapTxInputPartialSignature()
        );
    }

    private final byte[] peersWarningTxBuyerInputPartialSignature;
    private final byte[] peersWarningTxSellerInputPartialSignature;
    private final byte[] peersRedirectTxInputPartialSignature;
    private final byte[] swapTxInputPartialSignature;

    public PartialSignaturesMessage(byte[] peersWarningTxBuyerInputPartialSignature,
                                    byte[] peersWarningTxSellerInputPartialSignature,
                                    byte[] peersRedirectTxInputPartialSignature,
                                    byte[] swapTxInputPartialSignature) {
        this.peersWarningTxBuyerInputPartialSignature = peersWarningTxBuyerInputPartialSignature;
        this.peersWarningTxSellerInputPartialSignature = peersWarningTxSellerInputPartialSignature;
        this.peersRedirectTxInputPartialSignature = peersRedirectTxInputPartialSignature;
        this.swapTxInputPartialSignature = swapTxInputPartialSignature;
    }

    @Override
    public bisq.trade.protobuf.PartialSignaturesMessage.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.PartialSignaturesMessage.newBuilder()
                .setPeersWarningTxBuyerInputPartialSignature(ByteString.copyFrom(peersWarningTxBuyerInputPartialSignature))
                .setPeersWarningTxSellerInputPartialSignature(ByteString.copyFrom(peersWarningTxSellerInputPartialSignature))
                .setPeersRedirectTxInputPartialSignature(ByteString.copyFrom(peersRedirectTxInputPartialSignature))
                .setSwapTxInputPartialSignature(ByteString.copyFrom(swapTxInputPartialSignature));
    }

    @Override
    public bisq.trade.protobuf.PartialSignaturesMessage toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static PartialSignaturesMessage fromProto(bisq.trade.protobuf.PartialSignaturesMessage proto) {
        return new PartialSignaturesMessage(proto.getPeersWarningTxBuyerInputPartialSignature().toByteArray(),
                proto.getPeersWarningTxSellerInputPartialSignature().toByteArray(),
                proto.getPeersRedirectTxInputPartialSignature().toByteArray(),
                proto.getSwapTxInputPartialSignature().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PartialSignaturesMessage that)) return false;

        return Arrays.equals(peersWarningTxBuyerInputPartialSignature, that.peersWarningTxBuyerInputPartialSignature) &&
                Arrays.equals(peersWarningTxSellerInputPartialSignature, that.peersWarningTxSellerInputPartialSignature) &&
                Arrays.equals(peersRedirectTxInputPartialSignature, that.peersRedirectTxInputPartialSignature) &&
                Arrays.equals(swapTxInputPartialSignature, that.swapTxInputPartialSignature);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(peersWarningTxBuyerInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersWarningTxSellerInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersRedirectTxInputPartialSignature);
        result = 31 * result + Arrays.hashCode(swapTxInputPartialSignature);
        return result;
    }
}
