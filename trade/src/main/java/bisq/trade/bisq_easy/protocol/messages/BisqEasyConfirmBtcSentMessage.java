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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyConfirmBtcSentMessage extends BisqEasyTradeMessage {
    private final Optional<String> paymentProof;

    public BisqEasyConfirmBtcSentMessage(String id,
                                         String tradeId,
                                         String protocolVersion,
                                         NetworkId sender,
                                         NetworkId receiver,
                                         Optional<String> paymentProof) {
        super(id, tradeId, protocolVersion, sender, receiver);

        this.paymentProof = paymentProof;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        // We tolerate non-btc txId data as well
        paymentProof.ifPresent(paymentProof -> NetworkDataValidation.validateText(paymentProof, 1000));
    }

    @Override
    protected bisq.trade.protobuf.BisqEasyTradeMessage.Builder getBisqEasyTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                .setBisqEasyConfirmBtcSentMessage(toBisqEasyConfirmBtcSentMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage toBisqEasyConfirmBtcSentMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage.Builder builder = getBisqEasyConfirmBtcSentMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage.Builder getBisqEasyConfirmBtcSentMessageBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage.Builder builder = bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage.newBuilder();
        paymentProof.ifPresent(builder::setPaymentProof);
        return builder;
    }

    public static BisqEasyConfirmBtcSentMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyConfirmBtcSentMessage bisqEasyConfirmBtcSentMessage = proto.getBisqEasyTradeMessage().getBisqEasyConfirmBtcSentMessage();
        return new BisqEasyConfirmBtcSentMessage(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                bisqEasyConfirmBtcSentMessage.hasPaymentProof() ? Optional.of(bisqEasyConfirmBtcSentMessage.getPaymentProof()) : Optional.empty());
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}