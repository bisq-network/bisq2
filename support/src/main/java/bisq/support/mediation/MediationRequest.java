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

package bisq.support.mediation;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MediationRequest implements MailboxMessage, ExternalNetworkMessage {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final BisqEasyContract contract;
    private final String tradeId;

    @EqualsAndHashCode.Exclude
    private final UserProfile requester;
    @EqualsAndHashCode.Exclude
    private final UserProfile peer;
    @EqualsAndHashCode.Exclude
    private final List<BisqEasyOpenTradeMessage> chatMessages;

    public MediationRequest(String tradeId,
                            BisqEasyContract contract,
                            UserProfile requester,
                            UserProfile peer,
                            List<BisqEasyOpenTradeMessage> chatMessages) {
        this.tradeId = tradeId;
        this.contract = contract;
        this.requester = requester;
        this.peer = peer;
        this.chatMessages = maybePrune(chatMessages);

        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.chatMessages);

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        checkArgument(chatMessages.size() < 1000);
    }
    @Override
    public bisq.support.protobuf.MediationRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MediationRequest.newBuilder()
                .setTradeId(tradeId)
                .setContract(contract.toProto(serializeForHash))
                .setRequester(requester.toProto(serializeForHash))
                .setPeer(peer.toProto(serializeForHash))
                .addAllChatMessages(chatMessages.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.support.protobuf.MediationRequest toValueProto(boolean serializeForHash) {
        return resolveBuilder(this.getValueBuilder(serializeForHash), serializeForHash).build();
    }

    public static MediationRequest fromProto(bisq.support.protobuf.MediationRequest proto) {
        return new MediationRequest(proto.getTradeId(),
                BisqEasyContract.fromProto(proto.getContract()),
                UserProfile.fromProto(proto.getRequester()),
                UserProfile.fromProto(proto.getPeer()),
                proto.getChatMessagesList().stream()
                        .map(BisqEasyOpenTradeMessage::fromProto)
                        .collect(Collectors.toList()));
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MediationRequest proto = any.unpack(bisq.support.protobuf.MediationRequest.class);
                return MediationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.25, 0.5);
    }

    private List<BisqEasyOpenTradeMessage> maybePrune(List<BisqEasyOpenTradeMessage> chatMessages) {
        StringBuilder sb = new StringBuilder();
        List<BisqEasyOpenTradeMessage> result = chatMessages.stream()
                .filter(message -> {
                    sb.append(message.getText());
                    return sb.toString().length() < 10_000;
                })
                .collect(Collectors.toList());
        if (result.size() != chatMessages.size()) {
            log.warn("chatMessages have been pruned as total text size exceeded 10 000 characters. ");
            log.warn("chatMessages={}", chatMessages);
        }
        return result;
    }
}