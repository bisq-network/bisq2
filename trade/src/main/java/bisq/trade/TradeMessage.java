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

package bisq.trade;

import bisq.common.fsm.Event;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.trade.bisq_easy.messages.BisqEasyTakeOfferRequest;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class TradeMessage implements MailboxMessage, Event {
    public final static long TTL = TimeUnit.DAYS.toMillis(10);

    private final NetworkId sender;
    protected final MetaData metaData;

    protected TradeMessage(NetworkId sender, MetaData metaData) {
        this.sender = sender;
        this.metaData = metaData;
    }

    public bisq.trade.protobuf.TradeMessage.Builder getTradeMessageBuilder() {
        return bisq.trade.protobuf.TradeMessage.newBuilder()
                .setSender(sender.toProto())
                .setMetaData(metaData.toProto());
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
            case BISQEASYTAKEOFFERREQUEST: {
                return BisqEasyTakeOfferRequest.fromProto(proto);
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
                bisq.trade.protobuf.TradeMessage proto = any.unpack(bisq.trade.protobuf.TradeMessage.class);
                switch (proto.getMessageCase()) {
                    case BISQEASYTAKEOFFERREQUEST: {
                        return BisqEasyTakeOfferRequest.fromProto(proto);
                    }

                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

}