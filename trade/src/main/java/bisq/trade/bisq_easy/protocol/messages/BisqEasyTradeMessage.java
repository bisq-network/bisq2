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

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.Priority;
import bisq.network.p2p.services.data.storage.StoragePolicy;
import bisq.network.p2p.services.data.storage.Ttl;
import bisq.trade.protocol.messages.TradeMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
@StoragePolicy(ttl = Ttl.DAYS_10, priority = Priority.HIGH)
public abstract class BisqEasyTradeMessage extends TradeMessage {
    protected BisqEasyTradeMessage(String id, String tradeId, String protocolVersion, NetworkId sender, NetworkId receiver) {
        super(id, tradeId, protocolVersion, sender, receiver);
    }

    @Override
    public bisq.trade.protobuf.TradeMessage.Builder getValueBuilder(boolean serializeForHash) {
        return getTradeMessageBuilder(serializeForHash)
                .setBisqEasyTradeMessage(toBisqEasyTradeMessageProto(serializeForHash));
    }

    protected bisq.trade.protobuf.BisqEasyTradeMessage toBisqEasyTradeMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.BisqEasyTradeMessage.Builder builder = getBisqEasyTradeMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    abstract protected bisq.trade.protobuf.BisqEasyTradeMessage.Builder getBisqEasyTradeMessageBuilder(boolean serializeForHash);

    public static BisqEasyTradeMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        return switch (proto.getBisqEasyTradeMessage().getMessageCase()) {
            case BISQEASYTAKEOFFERREQUEST -> BisqEasyTakeOfferRequest.fromProto(proto);
            case BISQEASYTAKEOFFERRESPONSE -> BisqEasyTakeOfferResponse.fromProto(proto);
            case BISQEASYACCOUNTDATAMESSAGE -> BisqEasyAccountDataMessage.fromProto(proto);
            case BISQEASYCONFIRMFIATSENTMESSAGE -> BisqEasyConfirmFiatSentMessage.fromProto(proto);
            case BISQEASYBTCADDRESSMESSAGE -> BisqEasyBtcAddressMessage.fromProto(proto);
            case BISQEASYCONFIRMBTCSENTMESSAGE -> BisqEasyConfirmBtcSentMessage.fromProto(proto);
            case BISQEASYCONFIRMFIATRECEIPTMESSAGE -> BisqEasyConfirmFiatReceiptMessage.fromProto(proto);
            case BISQEASYREJECTTRADEMESSAGE -> BisqEasyRejectTradeMessage.fromProto(proto);
            case BISQEASYCANCELTRADEMESSAGE -> BisqEasyCancelTradeMessage.fromProto(proto);
            case BISQEASYREPORTERRORMESSAGE -> BisqEasyReportErrorMessage.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
