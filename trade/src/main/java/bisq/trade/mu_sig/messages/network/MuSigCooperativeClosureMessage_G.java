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
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@ToString(callSuper = true)
@Getter
//@EqualsAndHashCode(callSuper = true)
public final class MuSigCooperativeClosureMessage_G extends MuSigTradeMessage {
    private final byte[] peerOutputPrvKeyShare;

    public MuSigCooperativeClosureMessage_G(String id,
                                            String tradeId,
                                            String protocolVersion,
                                            NetworkId sender,
                                            NetworkId receiver,
                                            byte[] peerOutputPrvKeyShare) {
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
                .setMuSigCooperativeClosureMessageG(toMuSigCooperativeClosureMessage_GProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigCooperativeClosureMessage_G toMuSigCooperativeClosureMessage_GProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigCooperativeClosureMessage_G.Builder builder = getMuSigCooperativeClosureMessage_G(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigCooperativeClosureMessage_G.Builder getMuSigCooperativeClosureMessage_G(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigCooperativeClosureMessage_G.newBuilder()
                .setPeerOutputPrvKeyShare(ByteString.copyFrom(peerOutputPrvKeyShare));
    }

    public static MuSigCooperativeClosureMessage_G fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigCooperativeClosureMessage_G muSigMessageProto = proto.getMuSigTradeMessage().getMuSigCooperativeClosureMessageG();
        return new MuSigCooperativeClosureMessage_G(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                muSigMessageProto.getPeerOutputPrvKeyShare().toByteArray());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigCooperativeClosureMessage_G that)) return false;
        if (!super.equals(o)) return false;

        return Arrays.equals(peerOutputPrvKeyShare, that.peerOutputPrvKeyShare);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(peerOutputPrvKeyShare);
        return result;
    }
}
