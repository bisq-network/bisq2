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

package bisq.chat.bisqeasy.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.priv.PrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.vo.NetworkId;
import bisq.network.protobuf.ExternalNetworkMessage;
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
public final class BisqEasyOpenTradeMessage extends PrivateChatMessage implements BisqEasyOfferMessage {
    public static BisqEasyOpenTradeMessage createTakeOfferMessage(String tradeId,
                                                                  String channelId,
                                                                  UserProfile senderUserProfile,
                                                                  UserProfile receiverUserProfile,
                                                                  Optional<UserProfile> mediator,
                                                                  BisqEasyOffer bisqEasyOffer) {
        return new BisqEasyOpenTradeMessage(tradeId,
                channelId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                mediator,
                ChatMessageType.TAKE_BISQ_EASY_OFFER,
                bisqEasyOffer);
    }

    private final MetaData metaData = new MetaData(TTL_30_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final String tradeId;
    private final Optional<UserProfile> mediator;
    private final Optional<BisqEasyOffer> bisqEasyOffer;

    public BisqEasyOpenTradeMessage(String tradeId,
                                    String messageId,
                                    String channelId,
                                    UserProfile senderUserProfile,
                                    String receiverUserProfileId,
                                    NetworkId receiverNetworkId,
                                    @Nullable String text,
                                    Optional<Citation> citation,
                                    long date,
                                    boolean wasEdited,
                                    Optional<UserProfile> mediator,
                                    ChatMessageType chatMessageType,
                                    Optional<BisqEasyOffer> bisqEasyOffer) {
        this(tradeId,
                messageId,
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                channelId,
                senderUserProfile,
                receiverUserProfileId,
                receiverNetworkId,
                text,
                citation,
                date,
                wasEdited,
                mediator,
                chatMessageType,
                bisqEasyOffer);
    }

    private BisqEasyOpenTradeMessage(String tradeId,
                                     String messageId,
                                     ChatChannelDomain chatChannelDomain,
                                     String channelId,
                                     UserProfile senderUserProfile,
                                     String receiverUserProfileId,
                                     NetworkId receiverNetworkId,
                                     @Nullable String text,
                                     Optional<Citation> citation,
                                     long date,
                                     boolean wasEdited,
                                     Optional<UserProfile> mediator,
                                     ChatMessageType chatMessageType,
                                     Optional<BisqEasyOffer> bisqEasyOffer) {
        super(messageId, chatChannelDomain, channelId, senderUserProfile, receiverUserProfileId,
                receiverNetworkId, text, citation, date, wasEdited, chatMessageType);
        this.tradeId = tradeId;
        this.mediator = mediator;
        this.bisqEasyOffer = bisqEasyOffer;

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //908
    }

    private BisqEasyOpenTradeMessage(String tradeId,
                                     String channelId,
                                     UserProfile senderUserProfile,
                                     String receiverUserProfileId,
                                     NetworkId receiverNetworkId,
                                     Optional<UserProfile> mediator,
                                     ChatMessageType chatMessageType,
                                     BisqEasyOffer bisqEasyOffer) {
        super(StringUtils.createUid(),
                ChatChannelDomain.BISQ_EASY_OPEN_TRADES,
                channelId,
                senderUserProfile,
                receiverUserProfileId,
                receiverNetworkId,
                Optional.empty(),
                Optional.empty(),
                new Date().getTime(),
                false,
                chatMessageType);
        this.tradeId = tradeId;
        this.mediator = mediator;
        this.bisqEasyOffer = Optional.of(bisqEasyOffer);
        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //884
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    public bisq.chat.protobuf.ChatMessage toChatMessageProto() {
        bisq.chat.protobuf.BisqEasyOpenTradeMessage.Builder builder = bisq.chat.protobuf.BisqEasyOpenTradeMessage.newBuilder()
                .setTradeId(tradeId)
                .setReceiverUserProfileId(receiverUserProfileId)
                .setReceiverNetworkId(receiverNetworkId.toProto())
                .setSender(senderUserProfile.toProto());
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        bisqEasyOffer.ifPresent(offer -> builder.setBisqEasyOffer(offer.toProto()));
        return getChatMessageBuilder()
                .setBisqEasyOpenTradeMessage(builder)
                .build();
    }

    public static BisqEasyOpenTradeMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        bisq.chat.protobuf.BisqEasyOpenTradeMessage protoMessage = baseProto.getBisqEasyOpenTradeMessage();
        Optional<UserProfile> mediator = protoMessage.hasMediator() ?
                Optional.of(UserProfile.fromProto(protoMessage.getMediator())) :
                Optional.empty();
        Optional<BisqEasyOffer> bisqEasyOffer = protoMessage.hasBisqEasyOffer() ?
                Optional.of(BisqEasyOffer.fromProto(protoMessage.getBisqEasyOffer())) :
                Optional.empty();
        return new BisqEasyOpenTradeMessage(
                protoMessage.getTradeId(),
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                UserProfile.fromProto(protoMessage.getSender()),
                protoMessage.getReceiverUserProfileId(),
                NetworkId.fromProto(protoMessage.getReceiverNetworkId()),
                baseProto.getText(),
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                mediator,
                ChatMessageType.fromProto(baseProto.getChatMessageType()),
                bisqEasyOffer
        );
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }

    @Override
    public boolean hasBisqEasyOffer() {
        return bisqEasyOffer.isPresent();
    }

}