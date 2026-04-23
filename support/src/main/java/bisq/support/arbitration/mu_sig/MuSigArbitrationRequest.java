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

package bisq.support.arbitration.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.support.dispute.ChatMessagePruning;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.user.profile.UserProfile;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;
import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;
import static bisq.support.dispute.ChatMessagePruning.MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MuSigArbitrationRequest implements
        MailboxMessage, ExternalNetworkMessage, AckRequestingMessage, SenderPublicKeyProvidingPayload {
    public static String createMessageId(String tradeId) {
        return MuSigArbitrationRequest.class.getSimpleName() + "." + tradeId;
    }

    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final MuSigContract contract;
    private final String tradeId;
    private final MuSigMediationResult muSigMediationResult;
    private final byte[] mediationResultSignature;
    @EqualsAndHashCode.Exclude
    private final UserProfile requester;
    @EqualsAndHashCode.Exclude
    private final UserProfile peer;
    @EqualsAndHashCode.Exclude
    private final List<MuSigOpenTradeMessage> chatMessages;
    @EqualsAndHashCode.Exclude
    private final NetworkId arbitratorNetworkId;

    public MuSigArbitrationRequest(String tradeId,
                                   MuSigContract contract,
                                   MuSigMediationResult muSigMediationResult,
                                   byte[] mediationResultSignature,
                                   UserProfile requester,
                                   UserProfile peer,
                                   List<MuSigOpenTradeMessage> chatMessages,
                                   NetworkId arbitratorNetworkId) {
        this.tradeId = tradeId;
        this.contract = contract;
        this.muSigMediationResult = muSigMediationResult;
        this.mediationResultSignature = mediationResultSignature.clone();
        this.requester = requester;
        this.peer = peer;
        this.arbitratorNetworkId = arbitratorNetworkId;
        this.chatMessages = maybePrune(tradeId,
                contract,
                muSigMediationResult,
                this.mediationResultSignature,
                requester,
                peer,
                arbitratorNetworkId,
                chatMessages);

        Collections.sort(this.chatMessages);
        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        NetworkDataValidation.validateECSignature(mediationResultSignature);
        checkArgument(getSerializedSize(tradeId,
                        contract,
                        muSigMediationResult,
                        mediationResultSignature,
                        requester,
                        peer,
                        arbitratorNetworkId,
                        chatMessages) <= MAX_SERIALIZED_SIZE,
                "Serialized arbitration request size must not exceed " + MAX_SERIALIZED_SIZE + " bytes");
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationRequest.Builder getValueBuilder(boolean serializeForHash) {
        return getValueBuilder(tradeId,
                contract,
                muSigMediationResult,
                mediationResultSignature,
                requester,
                peer,
                arbitratorNetworkId,
                chatMessages,
                serializeForHash);
    }

    private static bisq.support.protobuf.MuSigArbitrationRequest.Builder getValueBuilder(String tradeId,
                                                                                         MuSigContract contract,
                                                                                         MuSigMediationResult muSigMediationResult,
                                                                                         byte[] mediationResultSignature,
                                                                                         UserProfile requester,
                                                                                         UserProfile peer,
                                                                                         NetworkId arbitratorNetworkId,
                                                                                         List<MuSigOpenTradeMessage> chatMessages,
                                                                                         boolean serializeForHash) {
        return bisq.support.protobuf.MuSigArbitrationRequest.newBuilder()
                .setTradeId(tradeId)
                .setContract(contract.toProto(serializeForHash))
                .setMuSigMediationResult(muSigMediationResult.toProto(serializeForHash))
                .setMediationResultSignature(ByteString.copyFrom(mediationResultSignature))
                .setRequester(requester.toProto(serializeForHash))
                .setPeer(peer.toProto(serializeForHash))
                .addAllChatMessages(chatMessages.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()))
                .setArbitratorNetworkId(arbitratorNetworkId.toProto(serializeForHash));
    }

    private static int getSerializedSize(String tradeId,
                                         MuSigContract contract,
                                         MuSigMediationResult muSigMediationResult,
                                         byte[] mediationResultSignature,
                                         UserProfile requester,
                                         UserProfile peer,
                                         NetworkId arbitratorNetworkId,
                                         List<MuSigOpenTradeMessage> chatMessages) {
        return getValueBuilder(tradeId,
                contract,
                muSigMediationResult,
                mediationResultSignature,
                requester,
                peer,
                arbitratorNetworkId,
                chatMessages,
                false)
                .build()
                .getSerializedSize();
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationRequest toValueProto(boolean serializeForHash) {
        return resolveBuilder(this.getValueBuilder(serializeForHash), serializeForHash).build();
    }

    public static MuSigArbitrationRequest fromProto(bisq.support.protobuf.MuSigArbitrationRequest proto) {
        return new MuSigArbitrationRequest(proto.getTradeId(),
                MuSigContract.fromProto(proto.getContract()),
                MuSigMediationResult.fromProto(proto.getMuSigMediationResult()),
                proto.getMediationResultSignature().toByteArray(),
                UserProfile.fromProto(proto.getRequester()),
                UserProfile.fromProto(proto.getPeer()),
                proto.getChatMessagesList().stream()
                        .map(MuSigOpenTradeMessage::fromProto)
                        .collect(Collectors.toList()),
                NetworkId.fromProto(proto.getArbitratorNetworkId()));
    }

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
        return arbitratorNetworkId;
    }

    public byte[] getMediationResultSignature() {
        return mediationResultSignature.clone();
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigArbitrationRequest proto = any.unpack(bisq.support.protobuf.MuSigArbitrationRequest.class);
                return MuSigArbitrationRequest.fromProto(proto);
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

    private static List<MuSigOpenTradeMessage> maybePrune(String tradeId,
                                                          MuSigContract contract,
                                                          MuSigMediationResult muSigMediationResult,
                                                          byte[] mediationResultSignature,
                                                          UserProfile requester,
                                                          UserProfile peer,
                                                          NetworkId arbitratorNetworkId,
                                                          List<MuSigOpenTradeMessage> chatMessages) {
        return ChatMessagePruning.maybePrune(chatMessages,
                MAX_TOTAL_CHAT_MESSAGES_TEXT_BYTES,
                MAX_SERIALIZED_SIZE,
                messages -> getSerializedSize(tradeId,
                        contract,
                        muSigMediationResult,
                        mediationResultSignature,
                        requester,
                        peer,
                        arbitratorNetworkId,
                        messages),
                log,
                tradeId);
    }
}
