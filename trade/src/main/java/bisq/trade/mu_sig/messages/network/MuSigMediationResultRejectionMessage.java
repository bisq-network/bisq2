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

import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;

@ToString(callSuper = true)
@Getter
public final class MuSigMediationResultRejectionMessage extends MuSigTradeMessage {
    private final byte[] mediationResultHash;

    public MuSigMediationResultRejectionMessage(String id,
                                                String tradeId,
                                                String protocolVersion,
                                                NetworkId sender,
                                                NetworkId receiver,
                                                byte[] mediationResultHash) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.mediationResultHash = mediationResultHash.clone();

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateHash(mediationResultHash);
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigMediationResultRejectionMessage(
                        toMuSigMediationResultRejectionMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigMediationResultRejectionMessage toMuSigMediationResultRejectionMessageProto(
            boolean serializeForHash) {
        bisq.trade.protobuf.MuSigMediationResultRejectionMessage.Builder builder =
                bisq.trade.protobuf.MuSigMediationResultRejectionMessage.newBuilder()
                        .setMediationResultHash(ByteString.copyFrom(mediationResultHash));
        return resolveBuilder(builder, serializeForHash).build();
    }

    public static MuSigMediationResultRejectionMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigMediationResultRejectionMessage message =
                proto.getMuSigTradeMessage().getMuSigMediationResultRejectionMessage();
        return new MuSigMediationResultRejectionMessage(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                message.getMediationResultHash().toByteArray());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.2);
    }

    public byte[] getMediationResultHash() {
        return mediationResultHash.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigMediationResultRejectionMessage that)) {
            return false;
        }
        return super.equals(o) && Arrays.equals(mediationResultHash, that.mediationResultHash);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Arrays.hashCode(mediationResultHash);
    }
}
