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

package bisq.support.mediation.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.message.ReceiverPublicKeyProvidingPayload;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.support.dispute.SerializedSizeExceededException;
import bisq.user.profile.UserProfile;
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
public final class MuSigMediationRequest implements MailboxMessage, ExternalNetworkMessage, AckRequestingMessage,
        SenderPublicKeyProvidingPayload, ReceiverPublicKeyProvidingPayload {
    public static String createMessageId(String tradeId) {
        return MuSigMediationRequest.class.getSimpleName() + "." + tradeId;
    }

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final MuSigContract contract;
    private final String tradeId;

    @EqualsAndHashCode.Exclude
    private final UserProfile requester;
    @EqualsAndHashCode.Exclude
    private final UserProfile peer;
    @EqualsAndHashCode.Exclude
    private final List<MuSigOpenTradeMessage> chatMessages;
    @EqualsAndHashCode.Exclude
    private final NetworkId mediatorNetworkId;

    public MuSigMediationRequest(String tradeId,
                                 MuSigContract contract,
                                 UserProfile requester,
                                 UserProfile peer,
                                 List<MuSigOpenTradeMessage> chatMessages,
                                 NetworkId mediatorNetworkId) {
        this.tradeId = tradeId;
        this.contract = contract;
        this.requester = requester;
        this.peer = peer;
        this.chatMessages = new ArrayList<>(chatMessages);
        this.mediatorNetworkId = mediatorNetworkId;

        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.chatMessages);

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        if (getSerializedSize() > MAX_SERIALIZED_SIZE) {
            throw new SerializedSizeExceededException(
                    "Serialized mediation request size must not exceed " + MAX_SERIALIZED_SIZE + " bytes");
        }
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MuSigMediationRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigMediationRequest.newBuilder()
                .setTradeId(tradeId)
                .setContract(contract.toProto(serializeForHash))
                .setRequester(requester.toProto(serializeForHash))
                .setPeer(peer.toProto(serializeForHash))
                .setMediatorNetworkId(mediatorNetworkId.toProto(serializeForHash))
                .addAllChatMessages(chatMessages.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.support.protobuf.MuSigMediationRequest toValueProto(boolean serializeForHash) {
        return resolveBuilder(this.getValueBuilder(serializeForHash), serializeForHash).build();
    }

    public static MuSigMediationRequest fromProto(bisq.support.protobuf.MuSigMediationRequest proto) {
        return new MuSigMediationRequest(proto.getTradeId(),
                MuSigContract.fromProto(proto.getContract()),
                UserProfile.fromProto(proto.getRequester()),
                UserProfile.fromProto(proto.getPeer()),
                proto.getChatMessagesList().stream()
                        .map(MuSigOpenTradeMessage::fromProto)
                        .collect(Collectors.toList()),
                NetworkId.fromProto(proto.getMediatorNetworkId()));
    }


    /* --------------------------------------------------------------------- */
    // AckRequestingMessage implementation
    /* --------------------------------------------------------------------- */

    @Override
    public String getAckRequestingMessageId() {
        return createMessageId(tradeId);
    }

    @Override
    public NetworkId getSender() {
        return requester.getNetworkId();
    }

    @Override
    public NetworkId getReceiver() {
        return mediatorNetworkId;
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigMediationRequest proto = any.unpack(bisq.support.protobuf.MuSigMediationRequest.class);
                return MuSigMediationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.25, 0.5);
    }

    @Override
    public PublicKey getSenderPublicKey() {
        return requester.getPublicKey();
    }

    @Override
    public PublicKey getReceiverPublicKey() {
        return mediatorNetworkId.getPubKey().getPublicKey();
    }

}
