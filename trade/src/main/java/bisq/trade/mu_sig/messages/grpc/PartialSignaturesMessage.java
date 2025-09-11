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
import bisq.common.util.OptionalUtils;
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public final class PartialSignaturesMessage implements Proto {
    public static PartialSignaturesMessage from(PartialSignatures peersPartialSignatures) {
        return new PartialSignaturesMessage(
                peersPartialSignatures.getPeersWarningTxBuyerInputPartialSignature(),
                peersPartialSignatures.getPeersWarningTxSellerInputPartialSignature(),
                peersPartialSignatures.getPeersRedirectTxInputPartialSignature(),
                peersPartialSignatures.getPeersClaimTxInputPartialSignature(),
                peersPartialSignatures.getSwapTxInputPartialSignature(),
                peersPartialSignatures.getSwapTxInputSighash()
        );
    }

    private final byte[] peersWarningTxBuyerInputPartialSignature;
    private final byte[] peersWarningTxSellerInputPartialSignature;
    private final byte[] peersRedirectTxInputPartialSignature;
    private final byte[] peersClaimTxInputPartialSignature;
    private final Optional<byte[]> swapTxInputPartialSignature;
    private final Optional<byte[]> swapTxInputSighash;

    public PartialSignaturesMessage(byte[] peersWarningTxBuyerInputPartialSignature,
                                    byte[] peersWarningTxSellerInputPartialSignature,
                                    byte[] peersRedirectTxInputPartialSignature,
                                    byte[] peersClaimTxInputPartialSignature,
                                    Optional<byte[]> swapTxInputPartialSignature,
                                    Optional<byte[]> swapTxInputSighash) {
        this.peersWarningTxBuyerInputPartialSignature = peersWarningTxBuyerInputPartialSignature;
        this.peersWarningTxSellerInputPartialSignature = peersWarningTxSellerInputPartialSignature;
        this.peersRedirectTxInputPartialSignature = peersRedirectTxInputPartialSignature;
        this.peersClaimTxInputPartialSignature = peersClaimTxInputPartialSignature;
        this.swapTxInputPartialSignature = swapTxInputPartialSignature;
        this.swapTxInputSighash = swapTxInputSighash;
    }

    @Override
    public bisq.trade.protobuf.PartialSignaturesMessage.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.PartialSignaturesMessage.Builder builder = bisq.trade.protobuf.PartialSignaturesMessage.newBuilder()
                .setPeersWarningTxBuyerInputPartialSignature(ByteString.copyFrom(peersWarningTxBuyerInputPartialSignature))
                .setPeersWarningTxSellerInputPartialSignature(ByteString.copyFrom(peersWarningTxSellerInputPartialSignature))
                .setPeersRedirectTxInputPartialSignature(ByteString.copyFrom(peersRedirectTxInputPartialSignature))
                .setPeersClaimTxInputPartialSignature(ByteString.copyFrom(peersClaimTxInputPartialSignature));
        swapTxInputPartialSignature.ifPresent(e -> builder.setSwapTxInputPartialSignature(ByteString.copyFrom(e)));
        swapTxInputSighash.ifPresent(e -> builder.setSwapTxInputSighash(ByteString.copyFrom(e)));
        return builder;
    }

    @Override
    public bisq.trade.protobuf.PartialSignaturesMessage toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static PartialSignaturesMessage fromProto(bisq.trade.protobuf.PartialSignaturesMessage proto) {
        return new PartialSignaturesMessage(proto.getPeersWarningTxBuyerInputPartialSignature().toByteArray(),
                proto.getPeersWarningTxSellerInputPartialSignature().toByteArray(),
                proto.getPeersRedirectTxInputPartialSignature().toByteArray(),
                proto.getPeersClaimTxInputPartialSignature().toByteArray(),
                proto.hasSwapTxInputPartialSignature()
                        ? Optional.of(proto.getSwapTxInputPartialSignature().toByteArray())
                        : Optional.empty(),
                proto.hasSwapTxInputSighash()
                        ? Optional.of(proto.getSwapTxInputSighash().toByteArray())
                        : Optional.empty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartialSignaturesMessage that)) return false;

        return Arrays.equals(peersWarningTxBuyerInputPartialSignature, that.peersWarningTxBuyerInputPartialSignature) &&
                Arrays.equals(peersWarningTxSellerInputPartialSignature, that.peersWarningTxSellerInputPartialSignature) &&
                Arrays.equals(peersRedirectTxInputPartialSignature, that.peersRedirectTxInputPartialSignature) &&
                Arrays.equals(peersClaimTxInputPartialSignature, that.peersClaimTxInputPartialSignature) &&
                OptionalUtils.optionalByteArrayEquals(swapTxInputPartialSignature, that.swapTxInputPartialSignature) &&
                OptionalUtils.optionalByteArrayEquals(swapTxInputSighash, that.swapTxInputSighash);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(peersWarningTxBuyerInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersWarningTxSellerInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersRedirectTxInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersClaimTxInputPartialSignature);
        result = 31 * result + swapTxInputPartialSignature.map(Arrays::hashCode).orElse(0);
        result = 31 * result + swapTxInputSighash.map(Arrays::hashCode).orElse(0);
        return result;
    }
}
