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

package bisq.support.dispute.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.support.dispute.SerializedSizeExceededException;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;
import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MuSigDisputeCaseDataMessage implements MailboxMessage, ExternalNetworkMessage, SenderPublicKeyProvidingPayload {
    public static final MetaData META_DATA = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, MuSigDisputeCaseDataMessage.class.getSimpleName());
    private final String tradeId;
    private final NetworkId senderNetworkId;
    private final byte[] contractHash;
    private final List<MuSigOpenTradeMessage> chatMessages;

    public MuSigDisputeCaseDataMessage(String tradeId,
                                       NetworkId senderNetworkId,
                                       byte[] contractHash,
                                       List<MuSigOpenTradeMessage> chatMessages) {
        this.tradeId = tradeId;
        this.senderNetworkId = senderNetworkId;
        this.contractHash = contractHash.clone();
        this.chatMessages = new ArrayList<>(chatMessages);

        Collections.sort(this.chatMessages);
        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        NetworkDataValidation.validateHash(contractHash);
        if (getSerializedSize() > MAX_SERIALIZED_SIZE) {
            throw new SerializedSizeExceededException(
                    "Serialized dispute case data size must not exceed " + MAX_SERIALIZED_SIZE + " bytes");
        }
    }

    @Override
    public bisq.support.protobuf.MuSigDisputeCaseDataMessage.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigDisputeCaseDataMessage.newBuilder()
                .setTradeId(tradeId)
                .setSenderNetworkId(senderNetworkId.toProto(serializeForHash))
                .setContractHash(ByteString.copyFrom(contractHash))
                .addAllChatMessages(chatMessages.stream()
                        .map(message -> message.toValueProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    public static MuSigDisputeCaseDataMessage fromProto(bisq.support.protobuf.MuSigDisputeCaseDataMessage proto)
            throws UnresolvableProtobufException {
        try {
            return new MuSigDisputeCaseDataMessage(
                    proto.getTradeId(),
                    NetworkId.fromProto(proto.getSenderNetworkId()),
                    proto.getContractHash().toByteArray(),
                    // The trade peers created the chat messages, and a node of an earlier version accepts a message which
                    // breaks a rule of this version. Such a message is skipped, so that it does not make the whole
                    // message unreadable.
                    ProtobufUtils.fromProtoStreamSkippingInvalid(proto.getChatMessagesList(),
                                    MuSigOpenTradeMessage::fromProto)
                            .collect(Collectors.toList())
            );
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigDisputeCaseDataMessage proto = any.unpack(bisq.support.protobuf.MuSigDisputeCaseDataMessage.class);
                return MuSigDisputeCaseDataMessage.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public PublicKey getSenderPublicKey() {
        return senderNetworkId.getPubKey().getPublicKey();
    }

    public byte[] getContractHash() {
        return contractHash.clone();
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }
}
