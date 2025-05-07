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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigPaymentInitiatedMessage_E extends MuSigTradeMessage {
    public final static int MAX_LENGTH = 1000;

    public MuSigPaymentInitiatedMessage_E(String id,
                                          String tradeId,
                                          String protocolVersion,
                                          NetworkId sender,
                                          NetworkId receiver) {
        super(id, tradeId, protocolVersion, sender, receiver);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setMuSigPaymentInitiatedMessageE(toMuSigPaymentInitiatedMessage_EProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigPaymentInitiatedMessage_E toMuSigPaymentInitiatedMessage_EProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigPaymentInitiatedMessage_E.Builder builder = getMuSigPaymentInitiatedMessage_E(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigPaymentInitiatedMessage_E.Builder getMuSigPaymentInitiatedMessage_E(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigPaymentInitiatedMessage_E.newBuilder();
    }

    public static MuSigPaymentInitiatedMessage_E fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigPaymentInitiatedMessage_E muSigMessageProto = proto.getMuSigTradeMessage().getMuSigPaymentInitiatedMessageE();
        return new MuSigPaymentInitiatedMessage_E(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
