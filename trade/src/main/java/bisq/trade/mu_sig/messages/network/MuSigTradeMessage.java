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

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.trade.protocol.messages.TradeMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class MuSigTradeMessage extends TradeMessage {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());

    protected MuSigTradeMessage(String id,
                                String tradeId,
                                String protocolVersion,
                                NetworkId sender,
                                NetworkId receiver) {
        super(id, tradeId, protocolVersion, sender, receiver);
    }

    @Override
    public bisq.trade.protobuf.TradeMessage.Builder getValueBuilder(boolean serializeForHash) {
        return getTradeMessageBuilder(serializeForHash)
                .setMuSigTradeMessage(toMuSigTradeMessageProto(serializeForHash));
    }

    protected bisq.trade.protobuf.MuSigTradeMessage toMuSigTradeMessageProto(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigTradeMessage.Builder builder = getMuSigTradeMessageBuilder(serializeForHash);
        return resolveBuilder(builder, serializeForHash).build();
    }

    abstract protected bisq.trade.protobuf.MuSigTradeMessage.Builder getMuSigTradeMessageBuilder(boolean serializeForHash);

    public static MuSigTradeMessage fromProto(bisq.trade.protobuf.TradeMessage proto) {
        return switch (proto.getMuSigTradeMessage().getMessageCase()) {
            case SETUPTRADEMESSAGE_A -> SetupTradeMessage_A.fromProto(proto);
            case SETUPTRADEMESSAGE_B -> SetupTradeMessage_B.fromProto(proto);
            case SETUPTRADEMESSAGE_C -> SetupTradeMessage_C.fromProto(proto);
            case SETUPTRADEMESSAGE_D -> SetupTradeMessage_D.fromProto(proto);
            case SENDACCOUNTPAYLOADANDDEPOSITTXMESSAGE -> SendAccountPayloadAndDepositTxMessage.fromProto(proto);
            case SENDACCOUNTPAYLOADMESSAGE -> SendAccountPayloadMessage.fromProto(proto);
            case PAYMENTINITIATEDMESSAGE_E -> PaymentInitiatedMessage_E.fromProto(proto);
            case PAYMENTRECEIVEDMESSAGE_F -> PaymentReceivedMessage_F.fromProto(proto);
            case COOPERATIVECLOSUREMESSAGE_G -> CooperativeClosureMessage_G.fromProto(proto);
            case MUSIGREPORTERRORMESSAGE -> MuSigReportErrorMessage.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}