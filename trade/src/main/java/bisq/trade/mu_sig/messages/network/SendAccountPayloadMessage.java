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

import bisq.account.accounts.AccountPayload;
import bisq.network.identity.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class SendAccountPayloadMessage extends MuSigTradeMessage {
    private final AccountPayload accountPayload;

    public SendAccountPayloadMessage(String id,
                                     String tradeId,
                                     String protocolVersion,
                                     NetworkId sender,
                                     NetworkId receiver,
                                     AccountPayload accountPayload) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.accountPayload = accountPayload;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigTradeMessage.newBuilder()
                .setSendAccountPayloadMessage(toSendAccountPayloadMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.SendAccountPayloadMessage toSendAccountPayloadMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.SendAccountPayloadMessage.Builder builder = getSendAccountPayloadMessage(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.SendAccountPayloadMessage.Builder getSendAccountPayloadMessage(boolean serializeForHash) {
        return bisq.trade.protobuf.SendAccountPayloadMessage.newBuilder()
                .setAccountPayload(accountPayload.toProto(serializeForHash));
    }

    public static SendAccountPayloadMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.SendAccountPayloadMessage muSigMessageProto = proto.getMuSigTradeMessage().getSendAccountPayloadMessage();
        return new SendAccountPayloadMessage(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                AccountPayload.fromProto(muSigMessageProto.getAccountPayload()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
