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
import bisq.common.util.OptionalUtils;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public final class PartialSignatures implements NetworkProto {
    public static PartialSignatures from(PartialSignaturesMessage partialSignaturesMessage,
                                         boolean redactSwapTxInputPartialSignature) {
        Optional<byte[]> swapTxInputPartialSignature = redactSwapTxInputPartialSignature
                ? Optional.empty()
                : partialSignaturesMessage.getSwapTxInputPartialSignature().map(byte[]::clone);
        return new PartialSignatures(
                partialSignaturesMessage.getPeersWarningTxBuyerInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersWarningTxSellerInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersRedirectTxInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersClaimTxInputPartialSignature().clone(),
                swapTxInputPartialSignature,
                partialSignaturesMessage.getSwapTxInputSighash().map(byte[]::clone)
        );
    }

    public static PartialSignatures toUnRedacted(PartialSignatures redactedPartialSignatures,
                                                 byte[] swapTxInputPartialSignature) {
        checkArgument(redactedPartialSignatures.getSwapTxInputPartialSignature().isEmpty(),
                "The PartialSignaturesMessage for the fromRedacted method call is expected to have an empty (redacted) swapTxInputPartialSignature.");
        return new PartialSignatures(
                redactedPartialSignatures.getPeersWarningTxBuyerInputPartialSignature().clone(),
                redactedPartialSignatures.getPeersWarningTxSellerInputPartialSignature().clone(),
                redactedPartialSignatures.getPeersRedirectTxInputPartialSignature().clone(),
                redactedPartialSignatures.getPeersClaimTxInputPartialSignature().clone(),
                Optional.of(swapTxInputPartialSignature.clone()),
                redactedPartialSignatures.getSwapTxInputSighash().map(byte[]::clone)
        );
    }

    private final byte[] peersWarningTxBuyerInputPartialSignature;
    private final byte[] peersWarningTxSellerInputPartialSignature;
    private final byte[] peersRedirectTxInputPartialSignature;
    private final byte[] peersClaimTxInputPartialSignature;
    private final Optional<byte[]> swapTxInputPartialSignature;
    private final Optional<byte[]> swapTxInputSighash;

    private PartialSignatures(byte[] peersWarningTxBuyerInputPartialSignature,
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

        verify();
    }

    @Override
    public void verify() {
        // TODO
    }

    @Override
    public bisq.trade.protobuf.PartialSignatures.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.PartialSignatures.Builder builder = bisq.trade.protobuf.PartialSignatures.newBuilder()
                .setPeersWarningTxBuyerInputPartialSignature(ByteString.copyFrom(peersWarningTxBuyerInputPartialSignature))
                .setPeersWarningTxSellerInputPartialSignature(ByteString.copyFrom(peersWarningTxSellerInputPartialSignature))
                .setPeersRedirectTxInputPartialSignature(ByteString.copyFrom(peersRedirectTxInputPartialSignature))
                .setPeersClaimTxInputPartialSignature(ByteString.copyFrom(peersClaimTxInputPartialSignature));
        swapTxInputPartialSignature.ifPresent(e -> builder.setSwapTxInputPartialSignature(ByteString.copyFrom(e)));
        swapTxInputSighash.ifPresent(e -> builder.setSwapTxInputSighash(ByteString.copyFrom(e)));
        return builder;
    }

    @Override
    public bisq.trade.protobuf.PartialSignatures toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static PartialSignatures fromProto(bisq.trade.protobuf.PartialSignatures proto) {
        return new PartialSignatures(proto.getPeersWarningTxBuyerInputPartialSignature().toByteArray(),
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
        if (!(o instanceof PartialSignatures that)) return false;

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
