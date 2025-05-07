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

import bisq.network.identity.NetworkId;
import bisq.trade.mu_sig.messages.grpc.NonceSharesMessage;
import bisq.trade.mu_sig.messages.grpc.PartialSignaturesMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigSetupTradeMessage_C extends MuSigTradeMessage {
    public final static int MAX_LENGTH = 1000;
    private final NonceSharesMessage nonceSharesMessage;
    private final PartialSignaturesMessage partialSignaturesMessage;

    public MuSigSetupTradeMessage_C(String id,
                                    String tradeId,
                                    String protocolVersion,
                                    NetworkId sender,
                                    NetworkId receiver,
                                    NonceSharesMessage nonceSharesMessage,
                                    PartialSignaturesMessage partialSignaturesMessage) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.nonceSharesMessage = nonceSharesMessage;
        this.partialSignaturesMessage = partialSignaturesMessage;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigSetupTradeMessageC(toMuSigSetupTradeMessage_CProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigSetupTradeMessage_C toMuSigSetupTradeMessage_CProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigSetupTradeMessage_C.Builder builder = getMuSigSetupTradeMessage_C(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigSetupTradeMessage_C.Builder getMuSigSetupTradeMessage_C(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigSetupTradeMessage_C.newBuilder()
                .setNonceSharesMessage(nonceSharesMessage.toProto(serializeForHash))
                .setPartialSignaturesMessage(partialSignaturesMessage.toProto(serializeForHash));
    }

    public static MuSigSetupTradeMessage_C fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigSetupTradeMessage_C muSigMessageProto = proto.getMuSigTradeMessage().getMuSigSetupTradeMessageC();
        return new MuSigSetupTradeMessage_C(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                NonceSharesMessage.fromProto(muSigMessageProto.getNonceSharesMessage()),
                PartialSignaturesMessage.fromProto(muSigMessageProto.getPartialSignaturesMessage()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
