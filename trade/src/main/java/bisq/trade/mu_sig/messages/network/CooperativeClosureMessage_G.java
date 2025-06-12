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

import bisq.common.data.ByteArray;
import bisq.network.identity.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class CooperativeClosureMessage_G extends MuSigTradeMessage {
    private final ByteArray peerOutputPrvKeyShare;

    public CooperativeClosureMessage_G(String id,
                                       String tradeId,
                                       String protocolVersion,
                                       NetworkId sender,
                                       NetworkId receiver,
                                       ByteArray peerOutputPrvKeyShare) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.peerOutputPrvKeyShare = peerOutputPrvKeyShare;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setCooperativeClosureMessageG(toCooperativeClosureMessage_GProto(serializeForHash));
    }

    private bisq.trade.protobuf.CooperativeClosureMessage_G toCooperativeClosureMessage_GProto(boolean serializeForHash) {
        bisq.trade.protobuf.CooperativeClosureMessage_G.Builder builder = getCooperativeClosureMessage_G(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.CooperativeClosureMessage_G.Builder getCooperativeClosureMessage_G(boolean serializeForHash) {
        return bisq.trade.protobuf.CooperativeClosureMessage_G.newBuilder()
                .setPeerOutputPrvKeyShare(peerOutputPrvKeyShare.toProto(serializeForHash));
    }

    public static CooperativeClosureMessage_G fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.CooperativeClosureMessage_G muSigMessageProto = proto.getMuSigTradeMessage().getCooperativeClosureMessageG();
        return new CooperativeClosureMessage_G(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                ByteArray.fromProto(muSigMessageProto.getPeerOutputPrvKeyShare()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
