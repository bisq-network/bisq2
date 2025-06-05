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
public final class MuSigSendAccountPayloadAndDepositTxMessage extends MuSigTradeMessage {
    private final ByteArray depositTx;
    private final AccountPayload accountPayload;

    public MuSigSendAccountPayloadAndDepositTxMessage(String id,
                                                      String tradeId,
                                                      String protocolVersion,
                                                      NetworkId sender,
                                                      NetworkId receiver,
                                                      ByteArray depositTx,
                                                      AccountPayload accountPayload) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.depositTx = depositTx;
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
                .setMuSigSendAccountPayloadAndDepositTxMessage(toMuSigSendAccountPayloadAndDepositTxMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.MuSigSendAccountPayloadAndDepositTxMessage toMuSigSendAccountPayloadAndDepositTxMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigSendAccountPayloadAndDepositTxMessage.Builder builder = getMuSigSetupTradeMessage_D(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.MuSigSendAccountPayloadAndDepositTxMessage.Builder getMuSigSetupTradeMessage_D(boolean serializeForHash) {
        return bisq.trade.protobuf.MuSigSendAccountPayloadAndDepositTxMessage.newBuilder()
                .setDepositTx(depositTx.toProto(serializeForHash))
                .setAccountPayload(accountPayload.toProto(serializeForHash));
    }

    public static MuSigSendAccountPayloadAndDepositTxMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.MuSigSendAccountPayloadAndDepositTxMessage muSigMessageProto = proto.getMuSigTradeMessage().getMuSigSendAccountPayloadAndDepositTxMessage();
        return new MuSigSendAccountPayloadAndDepositTxMessage(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                ByteArray.fromProto(muSigMessageProto.getDepositTx()),
                AccountPayload.fromProto(muSigMessageProto.getAccountPayload()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
