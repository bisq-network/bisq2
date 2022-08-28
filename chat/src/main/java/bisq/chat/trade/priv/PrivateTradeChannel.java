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
import bisq.common.observable.Observable;
import bisq.i18n.Res;
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
    private final Observable<Boolean> mediationActivated = new Observable<>(false);

    public PrivateTradeChannel(UserIdentity myUserIdentity, UserProfile trader1, UserProfile trader2, Optional<UserProfile> mediator) {
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

    private PrivateTradeChannel(String id,
                                UserProfile peer,
                                UserIdentity myUserIdentity,
                                Optional<UserProfile> mediator,
                                Set<PrivateTradeChatMessage> chatMessages,
                                ChannelNotificationType channelNotificationType) {
        super(id, peer, myUserIdentity, chatMessages, channelNotificationType);

        this.trader1 = peer;
        this.trader2 = myUserIdentity.getUserProfile();
        this.mediator = mediator;
        this.myUserIdentity = myUserIdentity;
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        bisq.chat.protobuf.PrivateTradeChannel.Builder builder = bisq.chat.protobuf.PrivateTradeChannel.newBuilder()
                .setPeer(peer.toProto())
                .setMyUserIdentity(this.myUserIdentity.toProto())
                .addAllChatMessages(chatMessages.stream()
                        .map(PrivateTradeChatMessage::toChatMessageProto)
                        .collect(Collectors.toList()))
                .setMediationActivated(mediationActivated.get());
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto()));
        return getChannelBuilder().setPrivateTradeChannel(builder).build();
    }

    public static PrivateTradeChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                bisq.chat.protobuf.PrivateTradeChannel proto) {
        PrivateTradeChannel privateTradeChannel = new PrivateTradeChannel(
                baseProto.getId(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.hasMediator() ? Optional.of(UserProfile.fromProto(proto.getMediator())) : Optional.empty(),
                proto.getChatMessagesList().stream()
                        .map(PrivateTradeChatMessage::fromProto)
                        .collect(Collectors.toSet()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
        privateTradeChannel.getMediationActivated().set(proto.getMediationActivated());
        return privateTradeChannel;
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

    public boolean isMediator() {
        return mediator.filter(mediator -> mediator.getId().equals(this.myUserIdentity.getId())).isPresent();
    }

    @Override
    public String getDisplayString() {
        String mediatorLabel = "";
        if (mediator.isPresent() && mediationActivated.get()) {
            mediatorLabel = " (" + Res.get("mediator") + ": " + mediator.get().getUserName() + ")";
        }
        return peer.getUserName() + " - " + myUserIdentity.getUserName() + mediatorLabel;
    }
}