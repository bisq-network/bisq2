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

package bisq.chat.notifications;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.observable.Observable;
import bisq.common.proto.PersistableProto;
import bisq.presentation.notifications.Notification;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public class ChatNotification implements Notification, PersistableProto {
    public static String createId(String channelId, String messageId) {
        return channelId + "." + messageId;
    }

    private final String id;
    private final String title;
    private final String message;
    private final long date;
    private final String chatChannelId;
    private final ChatChannelDomain chatChannelDomain;
    private final String chatMessageId;
    private final Optional<String> tradeId;
    private final Optional<UserProfile> mediator;
    private final Optional<UserProfile> senderUserProfile;

    // Mutable field
    @EqualsAndHashCode.Exclude
    private final Observable<Boolean> isConsumed = new Observable<>();

    public ChatNotification(String id,
                            String title,
                            String message,
                            ChatChannel<? extends ChatMessage> chatChannel,
                            ChatMessage chatMessage,
                            Optional<UserProfile> senderUserProfile) {
        this(id,
                title,
                message,
                chatMessage.getDate(),
                chatChannel.getId(),
                chatChannel.getChatChannelDomain(),
                chatMessage.getId(),
                findTradeId(chatChannel),
                senderUserProfile,
                findMediator(chatChannel),
                false);
    }

    private ChatNotification(String id,
                             String title,
                             String message,
                             long date,
                             String chatChannelId,
                             ChatChannelDomain chatChannelDomain,
                             String chatMessageId,
                             Optional<String> tradeId,
                             Optional<UserProfile> senderUserProfile,
                             Optional<UserProfile> mediator,
                             boolean isConsumed
    ) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.date = date;
        this.chatChannelId = chatChannelId;
        this.chatChannelDomain = chatChannelDomain;
        this.chatMessageId = chatMessageId;
        this.tradeId = tradeId;
        this.senderUserProfile = senderUserProfile;
        this.mediator = mediator;
        this.isConsumed.set(isConsumed);
    }

    @Override
    public bisq.chat.protobuf.ChatNotification.Builder getBuilder(boolean serializeForHash) {
        bisq.chat.protobuf.ChatNotification.Builder builder = bisq.chat.protobuf.ChatNotification.newBuilder()
                .setId(id)
                .setTitle(title)
                .setMessage(message)
                .setDate(date)
                .setChatChannelId(chatChannelId)
                .setChatChannelDomain(chatChannelDomain.toProtoEnum())
                .setChatMessageId(chatMessageId)
                .setIsConsumed(isConsumed.get());
        tradeId.ifPresent(builder::setTradeId);
        senderUserProfile.ifPresent(e -> builder.setSenderUserProfile(e.toProto(serializeForHash)));
        mediator.ifPresent(e -> builder.setMediator(e.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.chat.protobuf.ChatNotification toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ChatNotification fromProto(bisq.chat.protobuf.ChatNotification proto) {
        return new ChatNotification(
                proto.getId(),
                proto.getTitle(),
                proto.getMessage(),
                proto.getDate(),
                proto.getChatChannelId(),
                ChatChannelDomain.fromProto(proto.getChatChannelDomain()),
                proto.getChatMessageId(),
                proto.hasTradeId() ? Optional.of(proto.getTradeId()) : Optional.empty(),
                proto.hasSenderUserProfile() ? Optional.of(UserProfile.fromProto(proto.getSenderUserProfile())) : Optional.empty(),
                proto.hasMediator() ? Optional.of(UserProfile.fromProto(proto.getMediator())) : Optional.empty(),
                proto.getIsConsumed()
        );
    }

    private static Optional<String> findTradeId(ChatChannel<? extends ChatMessage> chatChannel) {
        return chatChannel instanceof BisqEasyOpenTradeChannel ?
                Optional.of(((BisqEasyOpenTradeChannel) chatChannel).getTradeId()) :
                Optional.empty();
    }

    private static Optional<UserProfile> findMediator(ChatChannel<? extends ChatMessage> chatChannel) {
        return chatChannel instanceof BisqEasyOpenTradeChannel ?
                ((BisqEasyOpenTradeChannel) chatChannel).getMediator() :
                Optional.empty();
    }

    public void setConsumed(boolean consumed) {
        this.isConsumed.set(consumed);
    }

    public boolean isNotConsumed() {
        return !isConsumed.get();
    }
}