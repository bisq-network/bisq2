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


package bisq.api.web_socket.domain.chat.public_chat;

import bisq.api.chat.common.PublicChatChannels;
import bisq.api.chat.common.PublicChatDtoFactory;
import bisq.api.dto.chat.common.CommonPublicChatMessageDto;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.api.web_socket.subscription.Topic.PUBLIC_CHAT_MESSAGES;

/**
 * Pushes the messages of the public chat channels. The subscribe snapshot is the whole visible history
 * of the subscribed channels (the mobile client searches messages locally, see
 * {@link PublicChatDtoFactory#visibleMessagesNewestFirst}; REST serves the same list for reloads).
 * After that, {@code ADDED} for every new message and {@code REMOVED} for every message that
 * leaves a channel — a deletion, the removal half of an edit, or the P2P store pruning expired data,
 * which comes in bursts every few minutes.
 * <p>
 * Structurally {@code TradeChatMessagesWebSocketService}, not the private chat sibling: none of the
 * channel ownership machinery private chat needs applies here.
 */
public class PublicChatMessagesWebSocketService extends ChannelScopedWebSocketService {
    private final List<Pin> messagesPins = new ArrayList<>();
    /**
     * Observers are notified from a copy-on-write snapshot, so a callback that began before the shutdown
     * unbound its pin still reaches this service afterwards; this keeps it from pushing.
     */
    private volatile boolean shutdownStarted;

    public PublicChatMessagesWebSocketService(SubscriberRepository subscriberRepository,
                                             PublicChatChannels channels,
                                             UserProfileService userProfileService,
                                             BannedUserService bannedUserService) {
        super(subscriberRepository, PUBLIC_CHAT_MESSAGES, channels, userProfileService, bannedUserService);
    }

    /**
     * The channel set is fixed by {@code CommonPublicChatChannelService.initialize}, which completes
     * before this service starts, and no public channel is added or removed afterwards, so the messages
     * of each channel are observed once and for all.
     */
    @Override
    public CompletableFuture<Boolean> initialize() {
        channels.getChannels().forEach(channel ->
                messagesPins.add(channel.getChatMessages().addObserver(new CollectionObserver<>() {
                    @Override
                    public void onAdded(CommonPublicChatMessage message) {
                        handleAddedMessage(message);
                    }

                    @Override
                    public void onRemoved(Object element) {
                        if (element instanceof CommonPublicChatMessage message) {
                            handleRemovedMessage(message);
                        }
                    }

                    @Override
                    public void onCleared() {
                        // Never happens: nothing calls clear or setAll on a public channel's messages,
                        // they are only removed one by one.
                    }
                })));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        messagesPins.forEach(Pin::unbind);
        messagesPins.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(Optional.empty());
    }

    @Override
    public Optional<String> getJsonPayload(Optional<String> parameter) {
        List<CommonPublicChatMessage> messages = channelsOf(parameter)
                .flatMap(channel -> channel.getChatMessages().stream())
                .toList();
        return toJson(dtoFactory.visibleMessageDtosNewestFirst(messages));
    }

    private void handleAddedMessage(CommonPublicChatMessage message) {
        if (shutdownStarted || !dtoFactory.isVisible(message)) {
            return;
        }
        push(message, ModificationType.ADDED);
    }

    /**
     * Not filtered by visibility, and it does not need to be: the domain gates both ends of the channel
     * on the same ban check, so a message from a banned author never entered it
     * ({@code ChatChannelService#addMessage}) and cannot arrive here. What does arrive is a message that
     * expired or was deleted while its author was still valid, and that has to be taken back.
     * <p>
     * The gate on the removal side is also a known limitation, and it is wider than the ban.
     * {@code PublicChatChannelService#processRemovedMessage} gates on {@code ChatChannelService#isValid},
     * which returns false for a banned author <em>and</em> for one who is currently rate limited. So a
     * message whose author was banned after delivery, or one pruned by the store while its author happens
     * to be over the limit, is not removed: the removal is offered once and dropped, the message never
     * leaves the channel and no {@code REMOVED} is pushed for it. A subscriber keeps it until it
     * reconnects and gets a snapshot without it. Desktop has the same shape — the message stays in its
     * list and is only dropped when the filter predicate next runs.
     * <p>
     * {@code push} leaves the same window for a different reason: it maps through {@code findDto},
     * which needs the author's profile, so an author pruned from the profile store after the message
     * was pushed produces no {@code REMOVED} either. See
     * {@code PublicChatReactionsWebSocketService#handleRemovedReaction}, which carries the same note.
     */
    private void handleRemovedMessage(CommonPublicChatMessage message) {
        if (shutdownStarted) {
            return;
        }
        push(message, ModificationType.REMOVED);
    }

    private void push(CommonPublicChatMessage message, ModificationType modificationType) {
        List<Subscriber> subscribers = findSubscribers(message.getChannelId());
        if (subscribers.isEmpty()) {
            return;
        }
        Optional<CommonPublicChatMessageDto> dto = dtoFactory.findDto(message);
        if (dto.isEmpty()) {
            return;
        }
        // The payload is defined as a list to support batch data delivery at subscribe.
        toJson(Collections.singletonList(dto.get())).ifPresent(json ->
                subscribers.forEach(subscriber -> send(json, subscriber, modificationType)));
    }
}
