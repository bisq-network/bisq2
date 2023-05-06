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

package bisq.chat.bisqeasy.message;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.MessageType;
import bisq.chat.message.PrivateChatMessage;
import bisq.chat.message.Quotation;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.network.protobuf.NetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class PrivateBisqEasyTradeChatMessage extends PrivateChatMessage implements BisqEasyOfferMessage {
    private final Optional<UserProfile> mediator;
    private final Optional<BisqEasyOffer> bisqEasyOffer;

    public PrivateBisqEasyTradeChatMessage(String messageId,
                                           String channelName,
                                           UserProfile sender,
                                           String receiversId,
                                           String text,
                                           Optional<Quotation> quotedMessage,
                                           long date,
                                           boolean wasEdited,
                                           Optional<UserProfile> mediator,
                                           MessageType messageType,
                                           Optional<BisqEasyOffer> bisqEasyOffer) {
        this(messageId,
                ChatChannelDomain.TRADE,
                channelName,
                sender,
                receiversId,
                text,
                quotedMessage,
                date,
                wasEdited,
                mediator,
                messageType,
                bisqEasyOffer,
                new MetaData(TTL, 100000, PrivateBisqEasyTradeChatMessage.class.getSimpleName()));
    }

    private PrivateBisqEasyTradeChatMessage(String messageId,
                                            ChatChannelDomain chatChannelDomain,
                                            String channelName,
                                            UserProfile sender,
                                            String receiversId,
                                            String text,
                                            Optional<Quotation> quotedMessage,
                                            long date,
                                            boolean wasEdited,
                                            Optional<UserProfile> mediator,
                                            MessageType messageType,
                                            Optional<BisqEasyOffer> bisqEasyOffer,
                                            MetaData metaData) {
        super(messageId, chatChannelDomain, channelName, sender, receiversId, text, quotedMessage, date, wasEdited, messageType, metaData);
        this.mediator = mediator;
        this.bisqEasyOffer = bisqEasyOffer;
    }

    @Override
    public boolean hasTradeChatOffer() {
        return bisqEasyOffer.isPresent();
    }

    @Override
    public NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    public bisq.chat.protobuf.ChatMessage toChatMessageProto() {
        bisq.chat.protobuf.PrivateBisqEasyTradeChatMessage.Builder builder = bisq.chat.protobuf.PrivateBisqEasyTradeChatMessage.newBuilder()
                .setReceiversId(receiversId)
                .setSender(sender.toProto());
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        bisqEasyOffer.ifPresent(offer -> builder.setBisqEasyOffer(offer.toProto()));
        return getChatMessageBuilder()
                .setPrivateBisqEasyTradeChatMessage(builder)
                .build();
    }

    public static PrivateBisqEasyTradeChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Quotation> quotedMessage = baseProto.hasQuotation() ?
                Optional.of(Quotation.fromProto(baseProto.getQuotation())) :
                Optional.empty();
        bisq.chat.protobuf.PrivateBisqEasyTradeChatMessage PrivateBisqEasyTradeChatMessage = baseProto.getPrivateBisqEasyTradeChatMessage();
        Optional<UserProfile> mediator = PrivateBisqEasyTradeChatMessage.hasMediator() ?
                Optional.of(UserProfile.fromProto(PrivateBisqEasyTradeChatMessage.getMediator())) :
                Optional.empty();
        Optional<BisqEasyOffer> bisqEasyOffer = baseProto.getPrivateBisqEasyTradeChatMessage().hasBisqEasyOffer() ?
                Optional.of(BisqEasyOffer.fromProto(baseProto.getPrivateBisqEasyTradeChatMessage().getBisqEasyOffer())) :
                Optional.empty();
        return new PrivateBisqEasyTradeChatMessage(
                baseProto.getMessageId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelName(),
                UserProfile.fromProto(PrivateBisqEasyTradeChatMessage.getSender()),
                PrivateBisqEasyTradeChatMessage.getReceiversId(),
                baseProto.getText(),
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                mediator,
                MessageType.fromProto(baseProto.getMessageType()),
                bisqEasyOffer,
                MetaData.fromProto(baseProto.getMetaData())
        );
    }
}