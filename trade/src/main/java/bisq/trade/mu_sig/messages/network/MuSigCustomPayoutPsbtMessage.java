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

package bisq.trade.mu_sig.messages.network;

import bisq.common.validation.BitcoinTransactionValidation;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@ToString(callSuper = true)
@Getter
public final class MuSigCustomPayoutPsbtMessage extends MuSigTradeMessage {
    public static final int MAX_PSBT_SIZE = 10_000;

    private final byte[] mediationResultHash;
    private final String txId;
    @ToString.Exclude
    private final byte[] psbt;

    public MuSigCustomPayoutPsbtMessage(String id,
                                        String tradeId,
                                        String protocolVersion,
                                        NetworkId sender,
                                        NetworkId receiver,
                                        byte[] mediationResultHash,
                                        String txId,
                                        byte[] psbt) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.mediationResultHash = mediationResultHash.clone();
        this.txId = txId;
        this.psbt = psbt.clone();

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateHash(mediationResultHash);
        checkArgument(BitcoinTransactionValidation.isValid(txId), "Invalid Bitcoin transaction ID");
        checkArgument(psbt.length > 0, "PSBT must not be empty");
        checkArgument(psbt.length <= MAX_PSBT_SIZE,
                "PSBT must not exceed " + MAX_PSBT_SIZE + " bytes");
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigCustomPayoutPsbtMessage(toMuSigCustomPayoutPsbtMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigCustomPayoutPsbtMessage toMuSigCustomPayoutPsbtMessageProto(
            boolean serializeForHash) {
        bisq.trade.protobuf.MuSigCustomPayoutPsbtMessage.Builder builder =
                getMuSigCustomPayoutPsbtMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigCustomPayoutPsbtMessage.Builder getMuSigCustomPayoutPsbtMessageBuilder(
            boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigCustomPayoutPsbtMessage.newBuilder()
                .setMediationResultHash(ByteString.copyFrom(mediationResultHash))
                .setTxId(txId)
                .setPsbt(ByteString.copyFrom(psbt));
    }

    public static MuSigCustomPayoutPsbtMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigCustomPayoutPsbtMessage message =
                proto.getMuSigTradeMessage().getMuSigCustomPayoutPsbtMessage();
        return new MuSigCustomPayoutPsbtMessage(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                message.getMediationResultHash().toByteArray(),
                message.getTxId(),
                message.getPsbt().toByteArray());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }

    public byte[] getMediationResultHash() {
        return mediationResultHash.clone();
    }

    public byte[] getPsbt() {
        return psbt.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigCustomPayoutPsbtMessage that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        return Objects.equals(txId, that.txId)
                && Arrays.equals(mediationResultHash, that.mediationResultHash)
                && Arrays.equals(psbt, that.psbt);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(mediationResultHash);
        result = 31 * result + Objects.hashCode(txId);
        result = 31 * result + Arrays.hashCode(psbt);
        return result;
    }
}
