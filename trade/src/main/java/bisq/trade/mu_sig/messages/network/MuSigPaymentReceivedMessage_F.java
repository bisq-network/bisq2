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
import bisq.trade.mu_sig.messages.grpc.SwapTxSignatureResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigPaymentReceivedMessage_F extends MuSigTradeMessage {
    public final static int MAX_LENGTH = 1000;
    private final SwapTxSignatureResponse swapTxSignatureResponse;

    public MuSigPaymentReceivedMessage_F(String id,
                                         String tradeId,
                                         String protocolVersion,
                                         NetworkId sender,
                                         NetworkId receiver,
                                         SwapTxSignatureResponse swapTxSignatureResponse) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.swapTxSignatureResponse = swapTxSignatureResponse;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigPaymentReceivedMessageF(toMuSigPaymentReceivedMessage_FProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigPaymentReceivedMessage_F toMuSigPaymentReceivedMessage_FProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigPaymentReceivedMessage_F.Builder builder = getMuSigPaymentReceivedMessage_F(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigPaymentReceivedMessage_F.Builder getMuSigPaymentReceivedMessage_F(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigPaymentReceivedMessage_F.newBuilder();
    }

    public static MuSigPaymentReceivedMessage_F fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigPaymentReceivedMessage_F muSigMessageProto = proto.getMuSigTradeMessage().getMuSigPaymentReceivedMessageF();
        return new MuSigPaymentReceivedMessage_F(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                SwapTxSignatureResponse.fromProto(muSigMessageProto.getSwapTxSignatureResponse()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
