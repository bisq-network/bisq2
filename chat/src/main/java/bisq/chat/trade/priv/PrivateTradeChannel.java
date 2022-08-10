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

package bisq.chat.trade.priv;

import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PrivateChannel;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateTradeChannel extends PrivateChannel<PrivateTradeChatMessage> {

    private final UserProfile trader1;
    private final UserProfile trader2;
    private final Optional<UserProfile> mediator;
    private final UserIdentity myUserIdentity;

    public PrivateTradeChannel(UserProfile trader1, UserProfile trader2, Optional<UserProfile> mediator, UserIdentity myUserIdentity) {
        super(PrivateChannel.createChannelId(trader1.getId(), trader2.getId()),
                trader1,
                myUserIdentity,
                new HashSet<>(),
                ChannelNotificationType.ALL);
        this.trader1 = trader1;
        this.trader2 = trader2;
        this.mediator = mediator;
        this.myUserIdentity = myUserIdentity;
    }

    public PrivateTradeChannel(UserProfile peer, UserIdentity myUserIdentity) {
        this(PrivateChannel.createChannelId(peer.getId(), myUserIdentity.getId()),
                peer,
                myUserIdentity,
                new HashSet<>(),
                ChannelNotificationType.ALL
        );
    }

    private PrivateTradeChannel(String id,
                                UserProfile peer,
                                UserIdentity myUserIdentity,
                                Set<PrivateTradeChatMessage> chatMessages,
                                ChannelNotificationType channelNotificationType) {
        super(id, peer, myUserIdentity, chatMessages, channelNotificationType);

        this.trader1 = peer;
        this.trader2 = myUserIdentity.getUserProfile();
        this.mediator = Optional.empty();
        this.myUserIdentity = myUserIdentity;
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateTradeChannel(bisq.chat.protobuf.PrivateTradeChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyUserIdentity(myProfile.toProto())
                        .addAllChatMessages(chatMessages.stream()
                                .map(PrivateTradeChatMessage::toChatMessageProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static PrivateTradeChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                bisq.chat.protobuf.PrivateTradeChannel proto) {
        return new PrivateTradeChannel(
                baseProto.getId(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getChatMessagesList().stream()
                        .map(PrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toSet()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
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
    public void removeChatMessages(Collection<PrivateTradeChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return peer.getUserName() + "-" + myProfile.getUserName();
    }
}