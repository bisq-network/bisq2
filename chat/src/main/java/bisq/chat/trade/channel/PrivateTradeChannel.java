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

package bisq.chat.trade.channel;

import bisq.chat.channel.ChannelDomain;
import bisq.chat.channel.ChannelMember;
import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PrivateGroupChannel;
import bisq.chat.trade.message.PrivateTradeChatMessage;
import bisq.chat.trade.message.TradeChatOffer;
import bisq.common.observable.Observable;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * PrivateTradeChannel is either a 2 party channel of both traders or a 3 party channel with 2 traders and the mediator.
 * Depending on the case the fields are differently interpreted.
 * Maybe we should model a group chat channel for a cleaner API.
 */
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateTradeChannel extends PrivateGroupChannel<PrivateTradeChatMessage> {

    public static PrivateTradeChannel createByTrader(TradeChatOffer tradeChatOffer,
                                                     UserIdentity myUserIdentity,
                                                     UserProfile peer,
                                                     Optional<UserProfile> mediator) {
        return new PrivateTradeChannel(tradeChatOffer,
                myUserIdentity,
                peer,
                mediator);
    }

    public static PrivateTradeChannel createByMediator(TradeChatOffer tradeChatOffer,
                                                       UserIdentity myUserIdentity,
                                                       UserProfile requestingTrader,
                                                       UserProfile nonRequestingTrader) {
        return new PrivateTradeChannel(tradeChatOffer,
                myUserIdentity,
                requestingTrader,
                nonRequestingTrader);
    }


    private final Observable<Boolean> isInMediation = new Observable<>(false);
    private final TradeChatOffer tradeChatOffer;

    private PrivateTradeChannel(TradeChatOffer tradeChatOffer,
                                UserIdentity myUserIdentity,
                                UserProfile peer,
                                Optional<UserProfile> mediator) {
        super(ChannelDomain.TRADE, tradeChatOffer.getId(), myUserIdentity, new ArrayList<>(), ChannelNotificationType.ALL);

        this.tradeChatOffer = tradeChatOffer;
        addChannelMember(new ChannelMember(ChannelMember.Type.TRADER, peer));
        mediator.ifPresent(mediatorProfile -> addChannelMember(new ChannelMember(ChannelMember.Type.MEDIATOR, mediatorProfile)));
    }

    // Mediator
    private PrivateTradeChannel(TradeChatOffer tradeChatOffer,
                                UserIdentity myUserIdentity,
                                UserProfile requestingTrader,
                                UserProfile nonRequestingTrader) {
        super(ChannelDomain.TRADE, tradeChatOffer.getId(), myUserIdentity, new ArrayList<>(), ChannelNotificationType.ALL);

        this.tradeChatOffer = tradeChatOffer;
        addChannelMember(new ChannelMember(ChannelMember.Type.TRADER, requestingTrader));
        addChannelMember(new ChannelMember(ChannelMember.Type.TRADER, nonRequestingTrader));
    }

    // From proto
    private PrivateTradeChannel(String channelName,
                                TradeChatOffer tradeChatOffer,
                                UserIdentity myUserIdentity,
                                Set<UserProfile> traders,
                                Optional<UserProfile> mediator,
                                List<PrivateTradeChatMessage> chatMessages,
                                boolean isInMediation,
                                ChannelNotificationType channelNotificationType,
                                Set<String> seenChatMessageIds) {
        super(ChannelDomain.TRADE, channelName, myUserIdentity, chatMessages, channelNotificationType);

        this.tradeChatOffer = tradeChatOffer;
        traders.forEach(trader -> addChannelMember(new ChannelMember(ChannelMember.Type.TRADER, trader)));
        mediator.ifPresent(mediatorProfile -> addChannelMember(new ChannelMember(ChannelMember.Type.MEDIATOR, mediatorProfile)));
        this.isInMediation.set(isInMediation);
        getSeenChatMessageIds().addAll(seenChatMessageIds);
    }

   /* private PrivateTradeChannel(UserProfile peerOrTrader1,
                                UserProfile myUserProfileOrTrader2,
                                UserIdentity myUserIdentity,
                                Optional<UserProfile> mediator) {
        this(PrivateChannel.createChannelName(new Pair<>(peerOrTrader1.getId(), myUserProfileOrTrader2.getId())),
                peerOrTrader1,
                myUserProfileOrTrader2,
                myUserIdentity,
                mediator,
                new ArrayList<>(),
                ChannelNotificationType.ALL);
    }*/

   /* private PrivateTradeChannel(String channelName,
                                UserProfile peerOrTrader1,
                                UserProfile myUserProfileOrTrader2,
                                UserIdentity myUserIdentity,
                                Optional<UserProfile> mediator,
                                List<PrivateTradeChatMessage> chatMessages,
                                ChannelNotificationType channelNotificationType) {
        super(ChannelDomain.TRADE, channelName, myUserIdentity, chatMessages, channelNotificationType);

        tradeChatOffer = null;
      *//*  this.peerOrTrader1 = peerOrTrader1;
        this.myUserProfileOrTrader2 = myUserProfileOrTrader2;
        this.myUserIdentity = myUserIdentity;
        this.mediator = mediator;*//*
    }*/

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        bisq.chat.protobuf.PrivateTradeChannel.Builder builder = bisq.chat.protobuf.PrivateTradeChannel.newBuilder()
                .setTradeChatOffer(tradeChatOffer.toProto())
                .setMyUserIdentity(myUserIdentity.toProto())
                .addAllTraders(getTraders().stream()
                        .map(UserProfile::toProto)
                        .collect(Collectors.toList()))
                .setIsInMediation(isInMediation.get())
                .addAllChatMessages(chatMessages.stream()
                        .map(PrivateTradeChatMessage::toChatMessageProto)
                        .collect(Collectors.toList()));
        findMediator().ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        return getChannelBuilder().setPrivateTradeChannel(builder).build();
    }

    public static PrivateTradeChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                bisq.chat.protobuf.PrivateTradeChannel proto) {
        return new PrivateTradeChannel(
                baseProto.getChannelName(),
                TradeChatOffer.fromProto(proto.getTradeChatOffer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getTradersList().stream()
                        .map(UserProfile::fromProto)
                        .collect(Collectors.toSet()),
                proto.hasMediator() ? Optional.of(UserProfile.fromProto(proto.getMediator())) : Optional.empty(),
                proto.getChatMessagesList().stream()
                        .map(PrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toList()),
                proto.getIsInMediation(),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()),
                new HashSet<>(baseProto.getSeenChatMessageIdsList()));
    }

    @Override
    public void addChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PrivateTradeChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PrivateTradeChatMessage> messages) {
        chatMessages.removeAll(messages);
    }

    public boolean isMediator() {
        return findMediator().filter(mediator -> mediator.getId().equals(myUserIdentity.getId())).isPresent();
    }

    @Override
    public String getDisplayString() {
        String mediatorLabel = "";
        Optional<UserProfile> mediator = findMediator();
        if (mediator.isPresent() && isInMediation.get()) {
            mediatorLabel = " (" + Res.get("mediator") + ": " + mediator.get().getUserName() + ")";
        }
        String peer = getPeer().getUserName();
        if (isMediator()) {
            checkArgument(getPeers().size() == 2, "getPeers().size() need to 2");
            return peer + " - " + getPeers().get(1).getUserName() + mediatorLabel;
        } else {
            return peer + " - " + myUserIdentity.getUserName() + mediatorLabel;
        }
    }

    public String getChannelSelectionDisplayString() {
        String peer = getPeer().getUserName();
        if (!isInMediation.get()) {
            return peer;
        }

        Optional<UserProfile> mediator = findMediator();
        if (isMediator()) {
            checkArgument(getPeers().size() == 2, "getPeers().size() need to 2");
            return peer + ", " + getPeers().get(1).getUserName();
        } else if (mediator.isPresent()) {
            return peer + ", " + mediator.get().getUserName();
        } else {
            return peer;
        }
    }

    public UserProfile getPeer() {
        checkArgument(getPeers().size() > 0, "getPeers().size() need to be > 0 at PrivateTradeChannel");
        return getPeers().get(0);
    }

    public Set<UserProfile> getTraders() {
        return getChannelMembers().stream()
                .filter(e -> e.getType() == ChannelMember.Type.TRADER)
                .map(ChannelMember::getUserProfile)
                .collect(Collectors.toSet());
    }

    public Optional<UserProfile> findMediator() {
        return getChannelMembers().stream()
                .filter(channelMember -> channelMember.getType() == ChannelMember.Type.MEDIATOR)
                .map(ChannelMember::getUserProfile).findAny();
    }
}