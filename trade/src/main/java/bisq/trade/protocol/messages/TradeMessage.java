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

package bisq.trade.protocol.messages;

import bisq.common.fsm.Event;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.p2p.vo.NetworkId;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.trade.submarine.messages.SubmarineTradeMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class TradeMessage implements MailboxMessage, AckRequestingMessage, Event {
    private final String id;
    private final String tradeId;
    private final NetworkId sender;
    protected final NetworkId receiver;

    protected TradeMessage(String id, String tradeId, NetworkId sender, NetworkId receiver) {
        this.id = id;
        this.tradeId = tradeId;
        this.sender = sender;
        this.receiver = receiver;

        NetworkDataValidation.validateText(tradeId, 200); // For private channels we combine user profile IDs for channelId
    }

    public bisq.trade.protobuf.TradeMessage.Builder getTradeMessageBuilder() {
        return bisq.trade.protobuf.TradeMessage.newBuilder()
                .setId(id)
                .setTradeId(tradeId)
                .setSender(sender.toProto())
                .setReceiver(receiver.toProto());
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toTradeMessageProto())))
                .build();
    }

    protected abstract bisq.trade.protobuf.TradeMessage toTradeMessageProto();

    public static TradeMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYTRADEMESSAGE: {
                return BisqEasyTradeMessage.fromProto(proto);
            }
            case SUBMARINETRADEMESSAGE: {
                return SubmarineTradeMessage.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static ProtoResolver<NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.trade.protobuf.TradeMessage.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}