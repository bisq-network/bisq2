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
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;

@Getter
public final class RedactedPartialSignatures implements NetworkProto {
    public static RedactedPartialSignatures from(PartialSignaturesMessage partialSignaturesMessage) {
        return new RedactedPartialSignatures(
                partialSignaturesMessage.getPeersWarningTxBuyerInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersWarningTxSellerInputPartialSignature().clone(),
                partialSignaturesMessage.getPeersRedirectTxInputPartialSignature().clone()
        );
    }

    private final byte[] peersWarningTxBuyerInputPartialSignature;
    private final byte[] peersWarningTxSellerInputPartialSignature;
    private final byte[] peersRedirectTxInputPartialSignature;

    private RedactedPartialSignatures(byte[] peersWarningTxBuyerInputPartialSignature,
                                      byte[] peersWarningTxSellerInputPartialSignature,
                                      byte[] peersRedirectTxInputPartialSignature) {
        this.peersWarningTxBuyerInputPartialSignature = peersWarningTxBuyerInputPartialSignature;
        this.peersWarningTxSellerInputPartialSignature = peersWarningTxSellerInputPartialSignature;
        this.peersRedirectTxInputPartialSignature = peersRedirectTxInputPartialSignature;

        verify();
    }

    @Override
    public void verify() {
        // TODO
    }

    @Override
    public bisq.trade.protobuf.RedactedPartialSignatures.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.RedactedPartialSignatures.newBuilder()
                .setPeersWarningTxBuyerInputPartialSignature(ByteString.copyFrom(peersWarningTxBuyerInputPartialSignature))
                .setPeersWarningTxSellerInputPartialSignature(ByteString.copyFrom(peersWarningTxSellerInputPartialSignature))
                .setPeersRedirectTxInputPartialSignature(ByteString.copyFrom(peersRedirectTxInputPartialSignature));
    }

    @Override
    public bisq.trade.protobuf.RedactedPartialSignatures toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static RedactedPartialSignatures fromProto(bisq.trade.protobuf.RedactedPartialSignatures proto) {
        return new RedactedPartialSignatures(proto.getPeersWarningTxBuyerInputPartialSignature().toByteArray(),
                proto.getPeersWarningTxSellerInputPartialSignature().toByteArray(),
                proto.getPeersRedirectTxInputPartialSignature().toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RedactedPartialSignatures that)) return false;

        return Arrays.equals(peersWarningTxBuyerInputPartialSignature, that.peersWarningTxBuyerInputPartialSignature) &&
                Arrays.equals(peersWarningTxSellerInputPartialSignature, that.peersWarningTxSellerInputPartialSignature) &&
                Arrays.equals(peersRedirectTxInputPartialSignature, that.peersRedirectTxInputPartialSignature);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(peersWarningTxBuyerInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersWarningTxSellerInputPartialSignature);
        result = 31 * result + Arrays.hashCode(peersRedirectTxInputPartialSignature);
        return result;
    }
}
