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
import bisq.trade.mu_sig.messages.network.mu_sig_data.PartialSignatures;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class SetupTradeMessage_D extends MuSigTradeMessage {
    private final PartialSignatures partialSignatures;

    public SetupTradeMessage_D(String id,
                               String tradeId,
                               String protocolVersion,
                               NetworkId sender,
                               NetworkId receiver,
                               PartialSignatures partialSignatures) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.partialSignatures = partialSignatures;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setSetupTradeMessageD(toSetupTradeMessage_DProto(serializeForHash));
    }

    private bisq.trade.protobuf.SetupTradeMessage_D toSetupTradeMessage_DProto(boolean serializeForHash) {
        bisq.trade.protobuf.SetupTradeMessage_D.Builder builder = getSetupTradeMessage_D(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.SetupTradeMessage_D.Builder getSetupTradeMessage_D(boolean serializeForHash) {
        return bisq.trade.protobuf.SetupTradeMessage_D.newBuilder()
                .setPartialSignatures(partialSignatures.toProto(serializeForHash));
    }

    public static SetupTradeMessage_D fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.SetupTradeMessage_D muSigMessageProto = proto.getMuSigTradeMessage().getSetupTradeMessageD();
        return new SetupTradeMessage_D(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                PartialSignatures.fromProto(muSigMessageProto.getPartialSignatures()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
