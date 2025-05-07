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

package bisq.chat.mu_sig.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.MuSigOpenTradeMessageReaction;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.mu_sig.MuSigOffer;
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

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class MuSigOpenTradeMessage extends PrivateChatMessage<MuSigOpenTradeMessageReaction> {
    public static final String ACK_REQUESTING_MESSAGE_ID_SEPARATOR = "_";

    // Metadata needs to be symmetric with MuSigOpenTradeMessageReaction.
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, HIGH_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final String tradeId;
    private final Optional<UserProfile> mediator;
    private final Optional<MuSigOffer> muSigOffer;

    public MuSigOpenTradeMessage(String tradeId,
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
                                 Optional<MuSigOffer> muSigOffer,
                                 Set<MuSigOpenTradeMessageReaction> chatMessageReactions) {
        this(tradeId,
                messageId,
                ChatChannelDomain.MU_SIG_OPEN_TRADES,
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
                muSigOffer,
                chatMessageReactions);
    }

    public MuSigOpenTradeMessage(String tradeId,
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
                                 Optional<MuSigOffer> muSigOffer,
                                 Set<MuSigOpenTradeMessageReaction> chatMessageReactions) {
        super(messageId, chatChannelDomain, channelId, senderUserProfile, receiverUserProfileId,
                receiverNetworkId, text, citation, date, wasEdited, chatMessageType, chatMessageReactions);
        this.tradeId = tradeId;
        this.mediator = mediator;
        this.muSigOffer = muSigOffer;
    }

    private MuSigOpenTradeMessage(String tradeId,
                                  String channelId,
                                  UserProfile senderUserProfile,
                                  String receiverUserProfileId,
                                  NetworkId receiverNetworkId,
                                  Optional<UserProfile> mediator,
                                  ChatMessageType chatMessageType,
                                  MuSigOffer muSigOffer) {
        super(StringUtils.createUid(),
                ChatChannelDomain.MU_SIG_OPEN_TRADES,
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
        this.muSigOffer = Optional.of(muSigOffer);
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getValueBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setMuSigOpenTradeMessage(toMuSigOpenTradeMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.MuSigOpenTradeMessage toMuSigOpenTradeMessageProto(boolean serializeForHash) {
        return resolveBuilder(getMuSigOpenTradeMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.MuSigOpenTradeMessage.Builder getMuSigOpenTradeMessageBuilder(boolean serializeForHash) {
        var builder = bisq.chat.protobuf.MuSigOpenTradeMessage.newBuilder()
                .setTradeId(tradeId)
                .setReceiverUserProfileId(receiverUserProfileId)
                .setReceiverNetworkId(receiverNetworkId.toProto(serializeForHash))
                .setSender(senderUserProfile.toProto(serializeForHash))
                .addAllChatMessageReactions(chatMessageReactions.stream()
                        .map(reaction -> reaction.getValueBuilder(serializeForHash).build())
                        .collect(Collectors.toList()));
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto(serializeForHash)));
        muSigOffer.ifPresent(offer -> builder.setMuSigOffer(offer.toProto(serializeForHash)));
        return builder;
    }

    public static MuSigOpenTradeMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        bisq.chat.protobuf.MuSigOpenTradeMessage protoMessage = baseProto.getMuSigOpenTradeMessage();
        Optional<UserProfile> mediator = protoMessage.hasMediator() ?
                Optional.of(UserProfile.fromProto(protoMessage.getMediator())) :
                Optional.empty();
        Optional<MuSigOffer> muSigOffer = protoMessage.hasMuSigOffer() ?
                Optional.of(MuSigOffer.fromProto(protoMessage.getMuSigOffer())) :
                Optional.empty();
        return new MuSigOpenTradeMessage(
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
                muSigOffer,
                protoMessage.getChatMessageReactionsList().stream()
                        .map(MuSigOpenTradeMessageReaction::fromProto)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }

    @Override
    public boolean canShowReactions() {
        return true;
    }

    @Override
    public boolean addChatMessageReaction(ChatMessageReaction chatMessageReaction) {
        return addPrivateChatMessageReaction((MuSigOpenTradeMessageReaction) chatMessageReaction);
    }

    @Override
    public String getAckRequestingMessageId() {
        return id + ACK_REQUESTING_MESSAGE_ID_SEPARATOR + receiverUserProfileId;
    }
}
