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
import bisq.common.validation.BitcoinTransactionValidation;
import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public final class PeerCustomPayoutPsbt implements NetworkProto {
    private final String txId;
    private final byte[] psbt;

    public PeerCustomPayoutPsbt(String txId, byte[] psbt) {
        this.txId = txId;
        this.psbt = psbt.clone();

        verify();
    }

    @Override
    public void verify() {
        checkArgument(BitcoinTransactionValidation.isValid(txId), "Invalid Bitcoin transaction ID");
        checkArgument(psbt.length > 0, "PSBT must not be empty");
    }

    @Override
    public bisq.trade.protobuf.PeerCustomPayoutPsbt.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.PeerCustomPayoutPsbt.newBuilder()
                .setTxId(txId)
                .setPsbt(ByteString.copyFrom(psbt));
    }

    @Override
    public bisq.trade.protobuf.PeerCustomPayoutPsbt toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static PeerCustomPayoutPsbt fromProto(bisq.trade.protobuf.PeerCustomPayoutPsbt proto) {
        return new PeerCustomPayoutPsbt(
                proto.getTxId(),
                proto.getPsbt().toByteArray());
    }

    public String getTxId() {
        return txId;
    }

    public byte[] getPsbt() {
        return psbt.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PeerCustomPayoutPsbt that)) {
            return false;
        }

        return Objects.equals(txId, that.txId)
                && Arrays.equals(psbt, that.psbt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(txId);
        result = 31 * result + Arrays.hashCode(psbt);
        return result;
    }
}
