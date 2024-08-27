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

package bisq.chat;

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class ChatChannel<M extends ChatMessage> implements PersistableProto {
    @EqualsAndHashCode.Include
    protected final String id;
    protected final ChatChannelDomain chatChannelDomain;
    protected final Observable<ChatChannelNotificationType> chatChannelNotificationType = new Observable<>();
    @Getter
    protected final transient ObservableSet<String> userProfileIdsOfActiveParticipants = new ObservableSet<>();
    protected final transient Map<String, AtomicInteger> numMessagesByAuthorId = new HashMap<>();
    @Getter
    protected final transient Set<String> userProfileIdsOfSendingLeaveMessage = new HashSet<>();

    public ChatChannel(String id,
                       ChatChannelDomain chatChannelDomain,
                       ChatChannelNotificationType chatChannelNotificationType) {
        this.id = id;
        this.chatChannelDomain = chatChannelDomain;
        this.chatChannelNotificationType.set(chatChannelNotificationType);
    }

    public bisq.chat.protobuf.ChatChannel.Builder getChatChannelBuilder() {
        return bisq.chat.protobuf.ChatChannel.newBuilder()
                .setId(id)
                .setChatChannelDomain(chatChannelDomain.toProtoEnum())
                .setChatChannelNotificationType(chatChannelNotificationType.get().toProtoEnum());
    }

    @Override
    public bisq.chat.protobuf.ChatChannel toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ChatChannel<? extends ChatMessage> fromProto(bisq.chat.protobuf.ChatChannel proto) {
        return switch (proto.getMessageCase()) {
            case TWOPARTYPRIVATECHATCHANNEL ->
                    TwoPartyPrivateChatChannel.fromProto(proto, proto.getTwoPartyPrivateChatChannel());
            case BISQEASYOPENTRADECHANNEL ->
                    BisqEasyOpenTradeChannel.fromProto(proto, proto.getBisqEasyOpenTradeChannel());
            case BISQEASYOFFERBOOKCHANNEL ->
                    BisqEasyOfferbookChannel.fromProto(proto, proto.getBisqEasyOfferbookChannel());
            case COMMONPUBLICCHATCHANNEL ->
                    CommonPublicChatChannel.fromProto(proto, proto.getCommonPublicChatChannel());
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean addChatMessage(M chatMessage) {
        boolean changed = getChatMessages().add(chatMessage);
        if (changed) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();
            numMessagesByAuthorId.putIfAbsent(authorUserProfileId, new AtomicInteger());
            numMessagesByAuthorId.get(authorUserProfileId).incrementAndGet();
        }
        return changed;
    }

    public boolean removeChatMessage(M chatMessage) {
        boolean changed = getChatMessages().remove(chatMessage);
        if (changed) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();
            if (numMessagesByAuthorId.containsKey(authorUserProfileId)) {
                AtomicInteger numMessages = numMessagesByAuthorId.get(authorUserProfileId);
                if (numMessages.get() > 0 && numMessages.decrementAndGet() == 0) {
                    // If no more messages of that user exist we remove them from userProfileIdsOfParticipants
                    userProfileIdsOfActiveParticipants.remove(chatMessage.getAuthorUserProfileId());
                }
            }
        }
        return changed;
    }

    public void removeChatMessages(Collection<M> messages) {
        messages.forEach(this::removeChatMessage);
    }

    public abstract String getDisplayString();

    public abstract ObservableSet<M> getChatMessages();
}