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
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MediationRequest implements MailboxMessage {
    private final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName());
    private final BisqEasyOffer bisqEasyOffer;
    private final String tradeId;
    private final UserProfile requester;
    private final UserProfile peer;
    private final List<BisqEasyOpenTradeMessage> chatMessages;

    public MediationRequest(String tradeId,
                            BisqEasyOffer bisqEasyOffer,
                            UserProfile requester,
                            UserProfile peer,
                            List<BisqEasyOpenTradeMessage> chatMessages) {
        this.tradeId = tradeId;
        this.bisqEasyOffer = bisqEasyOffer;
        this.requester = requester;
        this.peer = peer;
        this.chatMessages = maybePrune(chatMessages);

        // We need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.chatMessages);

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); // 3729 -> can be much more if lot of messages!
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toMediationRequestProto())))
                .build();
    }

    private bisq.support.protobuf.MediationRequest toMediationRequestProto() {
        return bisq.support.protobuf.MediationRequest.newBuilder()
                .setTradeId(tradeId)
                .setBisqEasyOffer(bisqEasyOffer.toProto())
                .setRequester(requester.toProto())
                .setPeer(peer.toProto())
                .addAllChatMessages(chatMessages.stream()
                        .map(BisqEasyOpenTradeMessage::toChatMessageProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static MediationRequest fromProto(bisq.support.protobuf.MediationRequest proto) {
        return new MediationRequest(proto.getTradeId(),
                BisqEasyOffer.fromProto(proto.getBisqEasyOffer()),
                UserProfile.fromProto(proto.getRequester()),
                UserProfile.fromProto(proto.getPeer()),
                proto.getChatMessagesList().stream()
                        .map(BisqEasyOpenTradeMessage::fromProto)
                        .collect(Collectors.toList()));
    }

    public static ProtoResolver<EnvelopePayloadMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MediationRequest proto = any.unpack(bisq.support.protobuf.MediationRequest.class);
                return MediationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
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