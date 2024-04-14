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

import bisq.network.identity.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyCancelTradeMessage extends BisqEasyTradeMessage {

    public BisqEasyCancelTradeMessage(String id,
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
    protected bisq.trade.protobuf.BisqEasyTradeMessage.Builder getBisqEasyTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyTradeMessage.newBuilder()
                .setBisqEasyCancelTradeMessage(toBisqEasyCancelTradeMessageProto(serializeForHash));
    }

    private bisq.trade.protobuf.BisqEasyCancelTradeMessage toBisqEasyCancelTradeMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.BisqEasyCancelTradeMessage.Builder builder = getBisqEasyCancelTradeMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    private bisq.trade.protobuf.BisqEasyCancelTradeMessage.Builder getBisqEasyCancelTradeMessageBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.BisqEasyCancelTradeMessage.newBuilder();
    }

    public static BisqEasyCancelTradeMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        return new BisqEasyCancelTradeMessage(
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