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

package bisq.chat.bisqeasy.channel.open_trades;

import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.channel.priv.PrivateGroupChatChannel;
import bisq.common.observable.Observable;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * PrivateTradeChannel is either a 2 party channel of both traders or a 3 party channel with 2 traders and the mediator.
 * Depending on the case the fields are differently interpreted.
 * Maybe we should model a group chat channel for a cleaner API.
 */
@ToString(callSuper = true)
@Slf4j
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class BisqEasyPrivateTradeChatChannel extends PrivateGroupChatChannel<BisqEasyPrivateTradeChatMessage> {
    public static String createId(BisqEasyOffer bisqEasyOffer) {
        return ChatChannelDomain.BISQ_EASY_OPEN_TRADES.name().toLowerCase() + "." + bisqEasyOffer.getId();
    }

    public static BisqEasyPrivateTradeChatChannel createByTrader(BisqEasyOffer bisqEasyOffer,
                                                                 UserIdentity myUserIdentity,
                                                                 UserProfile peer,
                                                                 Optional<UserProfile> mediator) {
        return new BisqEasyPrivateTradeChatChannel(bisqEasyOffer,
                myUserIdentity,
                peer,
                mediator);
    }

    public static BisqEasyPrivateTradeChatChannel createByMediator(BisqEasyOffer bisqEasyOffer,
                                                                   UserIdentity myUserIdentity,
                                                                   UserProfile requestingTrader,
                                                                   UserProfile nonRequestingTrader) {
        return new BisqEasyPrivateTradeChatChannel(bisqEasyOffer,
                myUserIdentity,
                requestingTrader,
                nonRequestingTrader);
    }

    private final Observable<Boolean> isInMediationObservable = new Observable<>(false);
    @Getter
    private final BisqEasyOffer bisqEasyOffer;
    @Getter
    private final Set<UserProfile> traders;
    @Getter
    private final Optional<UserProfile> mediator;

    // Called from trader
    private BisqEasyPrivateTradeChatChannel(BisqEasyOffer bisqEasyOffer,
                                            UserIdentity myUserIdentity,
                                            UserProfile peer,
                                            Optional<UserProfile> mediator) {
        this(createId(bisqEasyOffer),
                bisqEasyOffer,
                myUserIdentity,
                Set.of(peer),
                mediator,
                new ArrayList<>(),
                false,
                ChatChannelNotificationType.ALL);
    }

    // Called from mediator
    private BisqEasyPrivateTradeChatChannel(BisqEasyOffer bisqEasyOffer,
                                            UserIdentity myUserIdentity,
                                            UserProfile requestingTrader,
                                            UserProfile nonRequestingTrader) {
        this(createId(bisqEasyOffer),
                bisqEasyOffer,
                myUserIdentity,
                Set.of(requestingTrader, nonRequestingTrader),
                Optional.of(myUserIdentity.getUserProfile()),
                new ArrayList<>(),
                true,
                ChatChannelNotificationType.ALL);
    }

    // From proto
    private BisqEasyPrivateTradeChatChannel(String id,
                                            BisqEasyOffer bisqEasyOffer,
                                            UserIdentity myUserIdentity,
                                            Set<UserProfile> traders,
                                            Optional<UserProfile> mediator,
                                            List<BisqEasyPrivateTradeChatMessage> chatMessages,
                                            boolean isInMediation,
                                            ChatChannelNotificationType chatChannelNotificationType) {
        super(id, ChatChannelDomain.BISQ_EASY_OPEN_TRADES, myUserIdentity, chatMessages, chatChannelNotificationType);

        this.bisqEasyOffer = bisqEasyOffer;
        this.traders = traders;
        this.mediator = mediator;

        setIsInMediation(isInMediation);

        traders.forEach(userProfile -> userProfileIdsOfSendingLeaveMessage.add(userProfile.getId()));
    }

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        bisq.chat.protobuf.BisqEasyPrivateTradeChatChannel.Builder builder = bisq.chat.protobuf.BisqEasyPrivateTradeChatChannel.newBuilder()
                .setBisqEasyOffer(bisqEasyOffer.toProto())
                .setMyUserIdentity(myUserIdentity.toProto())
                .addAllTraders(getTraders().stream()
                        .map(UserProfile::toProto)
                        .collect(Collectors.toList()))
                .setIsInMediation(isInMediation())
                .addAllChatMessages(chatMessages.stream()
                        .map(BisqEasyPrivateTradeChatMessage::toChatMessageProto)
                        .collect(Collectors.toList()));
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        return getChatChannelBuilder().setBisqEasyPrivateTradeChatChannel(builder).build();
    }

    public static BisqEasyPrivateTradeChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                            bisq.chat.protobuf.BisqEasyPrivateTradeChatChannel proto) {
        return new BisqEasyPrivateTradeChatChannel(
                baseProto.getId(),
                BisqEasyOffer.fromProto(proto.getBisqEasyOffer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getTradersList().stream()
                        .map(UserProfile::fromProto)
                        .collect(Collectors.toSet()),
                proto.hasMediator() ? Optional.of(UserProfile.fromProto(proto.getMediator())) : Optional.empty(),
                proto.getChatMessagesList().stream()
                        .map(BisqEasyPrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toList()),
                proto.getIsInMediation(),
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getDisplayString() {
        String shortOfferId = bisqEasyOffer.getId().substring(0, 4);

        if (isMediator()) {
            checkArgument(traders.size() == 2, "traders.size() need to be 2 but is " + traders.size());
            List<UserProfile> tradersAsList = new ArrayList<>(traders);
            return shortOfferId + ": " + tradersAsList.get(0).getUserName() + " - " + tradersAsList.get(1).getUserName();
        } else {
            String peer = getPeer().getUserName();
            String optionalMediatorPostfix = mediator
                    .filter(mediator -> isInMediation())
                    .map(mediator -> ", " + mediator.getUserName() + " (" + Res.get("bisqEasy.mediator") + ")")
                    .orElse("");
            return shortOfferId + ": " + peer + optionalMediatorPostfix;
        }
    }

    public boolean isMediator() {
        return mediator.filter(mediator -> mediator.getId().equals(myUserIdentity.getId())).isPresent();
    }

    public UserProfile getPeer() {
        checkArgument(traders.size() >= 1,
                "traders is expected to has at least size 1 at getPeer() in  BisqEasyPrivateTradeChatChannel");
        return new ArrayList<>(traders).get(0);
    }

    @Override
    public boolean addChatMessage(BisqEasyPrivateTradeChatMessage chatMessage) {
        boolean changed = super.addChatMessage(chatMessage);
        if (changed) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();

            // todo we get called from inside constructor at fromProto. should be redesigned
            // If we received a leave message the user got removed from userProfileIdsOfParticipants
            // In that case we remove them from userProfileIdsOfSendingLeaveMessage as well to avoid sending a 
            // leave message.
            if (!userProfileIdsOfParticipants.contains(authorUserProfileId)) {
                userProfileIdsOfSendingLeaveMessage.remove(authorUserProfileId);
            }
        }
        return changed;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getter, setter
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<Boolean> isInMediationObservable() {
        return isInMediationObservable;
    }

    public void setIsInMediation(boolean isInMediation) {
        isInMediationObservable.set(isInMediation);

        if (isInMediation) {
            mediator.ifPresent(userProfile -> userProfileIdsOfSendingLeaveMessage.add(userProfile.getId()));
        }
    }

    public boolean isInMediation() {
        return isInMediationObservable.get();
    }
}