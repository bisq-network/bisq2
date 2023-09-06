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
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.chat.message.PrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.network.protobuf.NetworkMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyPrivateTradeChatMessage extends PrivateChatMessage implements BisqEasyOfferMessage {
    private final MetaData metaData = new MetaData(TTL_30_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final Optional<UserProfile> mediator;
    private final Optional<BisqEasyOffer> bisqEasyOffer;

    public BisqEasyPrivateTradeChatMessage(String messageId,
                                           String channelId,
                                           UserProfile sender,
                                           String receiverUserProfileId,
                                           @Nullable String text,
                                           Optional<Citation> citation,
                                           long date,
                                           boolean wasEdited,
                                           Optional<UserProfile> mediator,
                                           ChatMessageType chatMessageType,
                                           Optional<BisqEasyOffer> bisqEasyOffer) {
        this(messageId,
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                channelId,
                sender,
                receiverUserProfileId,
                text,
                citation,
                date,
                wasEdited,
                mediator,
                chatMessageType,
                bisqEasyOffer);
    }

    private BisqEasyPrivateTradeChatMessage(String messageId,
                                            ChatChannelDomain chatChannelDomain,
                                            String channelId,
                                            UserProfile sender,
                                            String receiverUserProfileId,
                                            @Nullable String text,
                                            Optional<Citation> citation,
                                            long date,
                                            boolean wasEdited,
                                            Optional<UserProfile> mediator,
                                            ChatMessageType chatMessageType,
                                            Optional<BisqEasyOffer> bisqEasyOffer) {
        super(messageId, chatChannelDomain, channelId, sender, receiverUserProfileId, text, citation, date, wasEdited, chatMessageType);
        this.mediator = mediator;
        this.bisqEasyOffer = bisqEasyOffer;


        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //908
    }

    public static BisqEasyPrivateTradeChatMessage createTakeOfferMessage(String channelId,
                                                                         UserProfile sender,
                                                                         String receiverUserProfileId,
                                                                         Optional<UserProfile> mediator,
                                                                         BisqEasyOffer bisqEasyOffer) {
        return new BisqEasyPrivateTradeChatMessage(channelId,
                sender,
                receiverUserProfileId,
                mediator,
                ChatMessageType.TAKE_BISQ_EASY_OFFER,
                bisqEasyOffer);
    }

    public BisqEasyPrivateTradeChatMessage(String channelId,
                                           UserProfile sender,
                                           String receiverUserProfileId,
                                           Optional<UserProfile> mediator,
                                           ChatMessageType chatMessageType,
                                           BisqEasyOffer bisqEasyOffer) {
        super(StringUtils.createShortUid(),
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                channelId,
                sender,
                receiverUserProfileId,
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false,
                chatMessageType);
        this.mediator = mediator;
        this.bisqEasyOffer = Optional.of(bisqEasyOffer);

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //884
    }

    @Override
    public boolean hasBisqEasyOffer() {
        return bisqEasyOffer.isPresent();
    }

    @Override
    public NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    public bisq.chat.protobuf.ChatMessage toChatMessageProto() {
        bisq.chat.protobuf.BisqEasyPrivateTradeChatMessage.Builder builder = bisq.chat.protobuf.BisqEasyPrivateTradeChatMessage.newBuilder()
                .setReceiverUserProfileId(receiverUserProfileId)
                .setSender(sender.toProto());
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        bisqEasyOffer.ifPresent(offer -> builder.setBisqEasyOffer(offer.toProto()));
        return getChatMessageBuilder()
                .setPrivateBisqEasyTradeChatMessage(builder)
                .build();
    }

    public static BisqEasyPrivateTradeChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        bisq.chat.protobuf.BisqEasyPrivateTradeChatMessage protoMessage = baseProto.getPrivateBisqEasyTradeChatMessage();
        Optional<UserProfile> mediator = protoMessage.hasMediator() ?
                Optional.of(UserProfile.fromProto(protoMessage.getMediator())) :
                Optional.empty();
        Optional<BisqEasyOffer> bisqEasyOffer = baseProto.getPrivateBisqEasyTradeChatMessage().hasBisqEasyOffer() ?
                Optional.of(BisqEasyOffer.fromProto(baseProto.getPrivateBisqEasyTradeChatMessage().getBisqEasyOffer())) :
                Optional.empty();
        return new BisqEasyPrivateTradeChatMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                UserProfile.fromProto(protoMessage.getSender()),
                protoMessage.getReceiverUserProfileId(),
                baseProto.getText(),
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                mediator,
                ChatMessageType.fromProto(baseProto.getChatMessageType()),
                bisqEasyOffer
        );
    }
}