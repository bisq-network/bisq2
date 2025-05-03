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

package bisq.trade.mu_sig.messages;

import bisq.network.identity.NetworkId;
import bisq.trade.mu_sig.grpc.PartialSignaturesMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class MuSigSetupTradeMessage_D extends MuSigTradeMessage {
    public final static int MAX_LENGTH = 1000;
    private final PartialSignaturesMessage partialSignaturesMessage;

    public MuSigSetupTradeMessage_D(String id,
                                    String tradeId,
                                    String protocolVersion,
                                    NetworkId sender,
                                    NetworkId receiver,
                                    PartialSignaturesMessage partialSignaturesMessage) {
        super(id, tradeId, protocolVersion, sender, receiver);
        this.partialSignaturesMessage = partialSignaturesMessage;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

    }

    @Override
    protected bisq.trade.protobuf.BisqEasyTradeMessage.Builder getBisqEasyTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                .setBisqEasyAccountDataMessage(toBisqEasyAccountDataMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.BisqEasyAccountDataMessage toBisqEasyAccountDataMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.BisqEasyAccountDataMessage.Builder builder = getBisqEasyAccountDataMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.BisqEasyAccountDataMessage.Builder getBisqEasyAccountDataMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyAccountDataMessage.newBuilder();
    }

    /*public static MuSigMessageA fromProto(bisq.trade.protobuf.TradeMessage proto) {
        bisq.trade.protobuf.BisqEasyAccountDataMessage bisqEasyAccountDataMessage = proto.getBisqEasyTradeMessage().getBisqEasyAccountDataMessage();
        return new MuSigMessageA(
                proto.getId(),
                proto.getTradeId(),
                proto.getProtocolVersion(),
                NetworkId.fromProto(proto.getSender()),
                NetworkId.fromProto(proto.getReceiver()),
                bisqEasyAccountDataMessage.getPaymentAccountData(),
                BisqEasyOffer.fromProto(bisqEasyAccountDataMessage.getBisqEasyOffer()));
    }*/

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}
