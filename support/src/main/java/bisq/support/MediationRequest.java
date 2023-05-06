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

package bisq.support;

import bisq.chat.bisqeasy.message.BisqEasyOffer;
import bisq.chat.bisqeasy.message.PrivateBisqEasyTradeChatMessage;
import bisq.common.data.ByteArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class MediationRequest implements MailboxMessage {
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            MediationRequest.class.getSimpleName());

    private final BisqEasyOffer bisqEasyOffer;
    private final UserProfile requester;
    private final UserProfile peer;
    private final List<PrivateBisqEasyTradeChatMessage> chatMessages;

    public MediationRequest(BisqEasyOffer bisqEasyOffer, UserProfile requester, UserProfile peer, List<PrivateBisqEasyTradeChatMessage> chatMessages) {
        this.bisqEasyOffer = bisqEasyOffer;
        this.requester = requester;
        this.peer = peer;
        this.chatMessages = chatMessages;

        // We need to sort deterministically as the data is used in the proof of work check
        this.chatMessages.sort(Comparator.comparing((PrivateBisqEasyTradeChatMessage e) -> new ByteArray(e.serialize())));
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toMediationRequestProto())))
                .build();
    }

    private bisq.support.protobuf.MediationRequest toMediationRequestProto() {
        return bisq.support.protobuf.MediationRequest.newBuilder()
                .setBisqEasyOffer(bisqEasyOffer.toProto())
                .setRequester(requester.toProto())
                .setPeer(peer.toProto())
                .addAllChatMessages(chatMessages.stream()
                        .map(PrivateBisqEasyTradeChatMessage::toChatMessageProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static MediationRequest fromProto(bisq.support.protobuf.MediationRequest proto) {
        return new MediationRequest(BisqEasyOffer.fromProto(proto.getBisqEasyOffer()),
                UserProfile.fromProto(proto.getRequester()),
                UserProfile.fromProto(proto.getPeer()),
                proto.getChatMessagesList().stream()
                        .map(PrivateBisqEasyTradeChatMessage::fromProto)
                        .collect(Collectors.toList()));
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MediationRequest proto = any.unpack(bisq.support.protobuf.MediationRequest.class);
                return MediationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}