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

import bisq.common.proto.Proto;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public final class PartialSignatures implements Proto {
    public static PartialSignatures from(PartialSignaturesMessage partialSignaturesMessage) {
        Optional<byte[]> swapTxInputPartialSignature = partialSignaturesMessage.getSwapTxInputPartialSignature();
        checkArgument(swapTxInputPartialSignature.isPresent(),
                "swapTxInputPartialSignature must not be empty when creating PartialSignatures from PartialSignaturesMessage");
        return new PartialSignatures(
                partialSignaturesMessage.getPeersWarningTxBuyerInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersWarningTxSellerInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersRedirectTxInputPartialSignature().clone(),
                swapTxInputPartialSignature.get().clone()
        );
    }

    public static PartialSignatures from(RedactedPartialSignatures partialSignatures,
                                         byte[] swapTxInputPartialSignature) {
        return new PartialSignatures(
                partialSignatures.getPeersWarningTxBuyerInputPartialSignature().clone(),
                partialSignatures.getPeersWarningTxSellerInputPartialSignature().clone(),
                partialSignatures.getPeersRedirectTxInputPartialSignature().clone(),
                swapTxInputPartialSignature.clone()
        );
    }

    private final byte[] peersWarningTxBuyerInputPartialSignature;
    private final byte[] peersWarningTxSellerInputPartialSignature;
    private final byte[] peersRedirectTxInputPartialSignature;
    private final byte[] swapTxInputPartialSignature;

    private PartialSignatures(byte[] peersWarningTxBuyerInputPartialSignature,
                              byte[] peersWarningTxSellerInputPartialSignature,
                              byte[] peersRedirectTxInputPartialSignature,
                              byte[] swapTxInputPartialSignature) {
        this.peersWarningTxBuyerInputPartialSignature = peersWarningTxBuyerInputPartialSignature;
        this.peersWarningTxSellerInputPartialSignature = peersWarningTxSellerInputPartialSignature;
        this.peersRedirectTxInputPartialSignature = peersRedirectTxInputPartialSignature;
        this.swapTxInputPartialSignature = swapTxInputPartialSignature;
    }

    @Override
    public bisq.trade.protobuf.PartialSignatures.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.PartialSignatures.newBuilder()
                .setPeersWarningTxBuyerInputPartialSignature(ByteString.copyFrom(peersWarningTxBuyerInputPartialSignature))
                .setPeersWarningTxSellerInputPartialSignature(ByteString.copyFrom(peersWarningTxSellerInputPartialSignature))
                .setPeersRedirectTxInputPartialSignature(ByteString.copyFrom(peersRedirectTxInputPartialSignature))
                .setSwapTxInputPartialSignature(ByteString.copyFrom(swapTxInputPartialSignature));
    }

    @Override
    public bisq.trade.protobuf.PartialSignatures toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static PartialSignatures fromProto(bisq.trade.protobuf.PartialSignatures proto) {
        return new PartialSignatures(proto.getPeersWarningTxBuyerInputPartialSignature().toByteArray(),
                proto.getPeersWarningTxSellerInputPartialSignature().toByteArray(),
                proto.getPeersRedirectTxInputPartialSignature().toByteArray(),
                proto.getSwapTxInputPartialSignature().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PartialSignatures that)) return false;

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
