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
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.*;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyOpenTradeMessage extends PrivateChatMessage<BisqEasyOpenTradeMessageReaction> implements BisqEasyOfferMessage {
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

    // Metadata needs to be symmetric with BisqEasyOpenTradeMessageReaction.
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, HIGH_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);

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
                                    Optional<BisqEasyOffer> bisqEasyOffer,
                                    Set<BisqEasyOpenTradeMessageReaction> reactions) {
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
                bisqEasyOffer,
                reactions);
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
                                     Optional<BisqEasyOffer> bisqEasyOffer,
                                     Set<BisqEasyOpenTradeMessageReaction> reactions) {
        super(messageId, chatChannelDomain, channelId, senderUserProfile, receiverUserProfileId,
                receiverNetworkId, text, citation, date, wasEdited, chatMessageType, reactions);
        this.tradeId = tradeId;
        this.mediator = mediator;
        this.bisqEasyOffer = bisqEasyOffer;
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
                chatMessageType,
                new HashSet<>());
        this.tradeId = tradeId;
        this.mediator = mediator;
        this.bisqEasyOffer = Optional.of(bisqEasyOffer);
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getValueBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setBisqEasyOpenTradeMessage(toBisqEasyOpenTradeMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.BisqEasyOpenTradeMessage toBisqEasyOpenTradeMessageProto(boolean serializeForHash) {
        return resolveBuilder(getBisqEasyOpenTradeMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.BisqEasyOpenTradeMessage.Builder getBisqEasyOpenTradeMessageBuilder(boolean serializeForHash) {
        var builder = bisq.chat.protobuf.BisqEasyOpenTradeMessage.newBuilder()
                .setTradeId(tradeId)
                .setReceiverUserProfileId(receiverUserProfileId)
                .setReceiverNetworkId(receiverNetworkId.toProto(serializeForHash))
                .setSender(senderUserProfile.toProto(serializeForHash))
                .addAllChatMessageReactions(chatMessageReactions.stream()
                        .map(reaction -> reaction.getValueBuilder(serializeForHash).build())
                        .collect(Collectors.toList()));
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto(serializeForHash)));
        bisqEasyOffer.ifPresent(offer -> builder.setBisqEasyOffer(offer.toProto(serializeForHash)));
        return builder;
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
                bisqEasyOffer,
                protoMessage.getChatMessageReactionsList().stream()
                        .map(BisqEasyOpenTradeMessageReaction::fromProto)
                        .collect(Collectors.toSet())
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

    @Override
    public boolean canShowReactions() {
        return true;
    }

    @Override
    public void addChatMessageReaction(ChatMessageReaction newReaction) {
        BisqEasyOpenTradeMessageReaction newBisqEasyOpenTradeReaction = (BisqEasyOpenTradeMessageReaction) newReaction;
        addPrivateChatMessageReaction(newBisqEasyOpenTradeReaction);
    }
}
