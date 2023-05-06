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

package bisq.chat.bisqeasy.channel.priv;

import bisq.chat.bisqeasy.message.BisqEasyOffer;
import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.channel.priv.PrivateChatChannelMember;
import bisq.chat.channel.priv.PrivateGroupChatChannel;
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
public final class BisqEasyPrivateTradeChatChannel extends PrivateGroupChatChannel<BisqEasyPrivateTradeChatMessage> {

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


    private final Observable<Boolean> isInMediation = new Observable<>(false);
    private final BisqEasyOffer bisqEasyOffer;

    private BisqEasyPrivateTradeChatChannel(BisqEasyOffer bisqEasyOffer,
                                            UserIdentity myUserIdentity,
                                            UserProfile peer,
                                            Optional<UserProfile> mediator) {
        super(ChatChannelDomain.TRADE, bisqEasyOffer.getId(), myUserIdentity, new ArrayList<>(), ChatChannelNotificationType.ALL);

        this.bisqEasyOffer = bisqEasyOffer;
        addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.TRADER, peer));
        mediator.ifPresent(mediatorProfile -> addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.MEDIATOR, mediatorProfile)));
    }

    // Mediator
    private BisqEasyPrivateTradeChatChannel(BisqEasyOffer bisqEasyOffer,
                                            UserIdentity myUserIdentity,
                                            UserProfile requestingTrader,
                                            UserProfile nonRequestingTrader) {
        super(ChatChannelDomain.TRADE, bisqEasyOffer.getId(), myUserIdentity, new ArrayList<>(), ChatChannelNotificationType.ALL);

        this.bisqEasyOffer = bisqEasyOffer;
        addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.TRADER, requestingTrader));
        addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.TRADER, nonRequestingTrader));
    }

    // From proto
    private BisqEasyPrivateTradeChatChannel(String channelName,
                                            BisqEasyOffer bisqEasyOffer,
                                            UserIdentity myUserIdentity,
                                            Set<UserProfile> traders,
                                            Optional<UserProfile> mediator,
                                            List<BisqEasyPrivateTradeChatMessage> chatMessages,
                                            boolean isInMediation,
                                            ChatChannelNotificationType chatChannelNotificationType,
                                            Set<String> seenChatMessageIds) {
        super(ChatChannelDomain.TRADE, channelName, myUserIdentity, chatMessages, chatChannelNotificationType);

        this.bisqEasyOffer = bisqEasyOffer;
        traders.forEach(trader -> addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.TRADER, trader)));
        mediator.ifPresent(mediatorProfile -> addChannelMember(new PrivateChatChannelMember(PrivateChatChannelMember.Type.MEDIATOR, mediatorProfile)));
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

        bisqEasyOffer = null;
      *//*  this.peerOrTrader1 = peerOrTrader1;
        this.myUserProfileOrTrader2 = myUserProfileOrTrader2;
        this.myUserIdentity = myUserIdentity;
        this.mediator = mediator;*//*
    }*/

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        bisq.chat.protobuf.BisqEasyPrivateTradeChatChannel.Builder builder = bisq.chat.protobuf.BisqEasyPrivateTradeChatChannel.newBuilder()
                .setBisqEasyOffer(bisqEasyOffer.toProto())
                .setMyUserIdentity(myUserIdentity.toProto())
                .addAllTraders(getTraders().stream()
                        .map(UserProfile::toProto)
                        .collect(Collectors.toList()))
                .setIsInMediation(isInMediation.get())
                .addAllChatMessages(chatMessages.stream()
                        .map(BisqEasyPrivateTradeChatMessage::toChatMessageProto)
                        .collect(Collectors.toList()));
        findMediator().ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        return getChannelBuilder().setPrivateBisqEasyTradeChatChannel(builder).build();
    }

    public static BisqEasyPrivateTradeChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                            bisq.chat.protobuf.BisqEasyPrivateTradeChatChannel proto) {
        return new BisqEasyPrivateTradeChatChannel(
                baseProto.getChannelName(),
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
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType()),
                new HashSet<>(baseProto.getSeenChatMessageIdsList()));
    }

    @Override
    public void addChatMessage(BisqEasyPrivateTradeChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(BisqEasyPrivateTradeChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<BisqEasyPrivateTradeChatMessage> messages) {
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
        checkArgument(getPeers().size() > 0, "getPeers().size() need to be > 0 at BisqEasyPrivateTradeChatChannel");
        return getPeers().get(0);
    }

    public Set<UserProfile> getTraders() {
        return getPrivateChatChannelMembers().stream()
                .filter(e -> e.getType() == PrivateChatChannelMember.Type.TRADER)
                .map(PrivateChatChannelMember::getUserProfile)
                .collect(Collectors.toSet());
    }

    public Optional<UserProfile> findMediator() {
        return getPrivateChatChannelMembers().stream()
                .filter(privateChatChannelMember -> privateChatChannelMember.getType() == PrivateChatChannelMember.Type.MEDIATOR)
                .map(PrivateChatChannelMember::getUserProfile).findAny();
    }
}