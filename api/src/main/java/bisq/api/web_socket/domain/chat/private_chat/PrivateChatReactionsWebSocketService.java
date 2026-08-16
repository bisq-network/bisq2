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

package bisq.api.web_socket.domain.chat.private_chat;

import bisq.api.dto.chat.two_party.TwoPartyPrivateChatMessageReactionDto;
import bisq.api.dto.mappings.chat.two_party.TwoPartyPrivateChatMessageReactionDtoMapping;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.user.banned.BannedUserService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.PRIVATE_CHAT_REACTIONS;

/**
 * Pushes reactions on two-party private chat (DM) messages, mirroring
 * {@code ChatReactionsWebSocketService} for trade chat.
 * <p>
 * Un-reacting is two events on this topic, not one: {@code PrivateChatMessage#addPrivateChatMessageReaction}
 * drops the superseded reaction from the set and adds a fresh one carrying {@code isRemoved = true} —
 * provided the new one is the more recent of the two, since an out-of-order reaction is ignored outright
 * and emits nothing. So a subscriber sees {@link ModificationType#REMOVED} for the reaction going away, then
 * {@link ModificationType#ADDED} for the marker recording that it went — the marker is not itself pushed
 * as REMOVED, so a client routing on the modification type alone would re-add it.
 * {@link #getJsonPayload()} drops the markers rather than reproducing the pair, because the subscribe
 * snapshot has no modification type to carry the distinction.
 * <p>
 * Reactions from banned senders are dropped, matching {@code PrivateChatMessagesWebSocketService}: a peer
 * banned after the fact vanishes from the message stream, and letting their reactions through would leave
 * the client holding reactions against a {@code chatMessageId} it never received.
 */
@Slf4j
public class PrivateChatReactionsWebSocketService extends BaseWebSocketService {
    private final TwoPartyPrivateChatChannelService channelService;
    private final BannedUserService bannedUserService;
    @Nullable
    private Pin channelsPin;
    private final Map<String, Pin> chatMessagesPinsByChannelId = new ConcurrentHashMap<>();
    /**
     * Nested by channel, not a flat message-id map, so that leaving a channel can unbind exactly its
     * reaction observers. Leaving a DM is a routine user action, unlike leaving a trade channel, so a
     * flat map would accumulate pins for the lifetime of the process.
     */
    private final Map<String, Map<String, Pin>> chatMessageReactionsPinsByChannelId = new ConcurrentHashMap<>();

    public PrivateChatReactionsWebSocketService(SubscriberRepository subscriberRepository,
                                                TwoPartyPrivateChatChannelService channelService,
                                                BannedUserService bannedUserService) {
        super(subscriberRepository, PRIVATE_CHAT_REACTIONS);

        this.channelService = channelService;
        this.bannedUserService = bannedUserService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        channelsPin = channelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(TwoPartyPrivateChatChannel channel) {
                String channelId = channel.getId();
                // Atomic operation
                chatMessagesPinsByChannelId.compute(channelId, (key, oldPin) -> {
                    if (oldPin != null) {
                        oldPin.unbind();
                    }

                    return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void onAdded(TwoPartyPrivateChatMessage message) {
                            String messageId = message.getId();
                            Map<String, Pin> reactionPins = chatMessageReactionsPinsByChannelId
                                    .computeIfAbsent(channelId, key -> new ConcurrentHashMap<>());

                            reactionPins.compute(messageId, (key, oldReactionPin) -> {
                                if (oldReactionPin != null) {
                                    oldReactionPin.unbind();
                                }

                                return message.getChatMessageReactions().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void onAdded(TwoPartyPrivateChatMessageReaction reaction) {
                                        handleReaction(reaction, ModificationType.ADDED);
                                    }

                                    @Override
                                    public void onRemoved(Object element) {
                                        if (element instanceof TwoPartyPrivateChatMessageReaction reaction) {
                                            handleReaction(reaction, ModificationType.REMOVED);
                                        }
                                    }

                                    @Override
                                    public void onCleared() {
                                        throw new UnsupportedOperationException("Clear method is not supported for chatMessageReactions.");
                                    }
                                });
                            });
                        }

                        @Override
                        public void onRemoved(Object element) {
                            // Messages cannot be removed
                        }

                        @Override
                        public void onCleared() {
                            // Messages cannot be cleared
                        }
                    });
                });
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof TwoPartyPrivateChatChannel channel) {
                    unbindChannelPins(channel.getId());
                }
            }

            @Override
            public void onCleared() {
                unbindAllChannelPins();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Keyed off the message pins alone: a channel's reaction pins are only ever created inside its
     * message observer, so there is no channel that holds the second kind without the first.
     */
    private void unbindAllChannelPins() {
        new ArrayList<>(chatMessagesPinsByChannelId.keySet()).forEach(this::unbindChannelPins);
    }

    /** Unbinds only this channel's observers — the message pin and every reaction pin under it. */
    private void unbindChannelPins(String channelId) {
        // Atomic operations
        chatMessagesPinsByChannelId.computeIfPresent(channelId, (key, pin) -> {
            pin.unbind();
            return null;  // returning null removes the key
        });
        chatMessageReactionsPinsByChannelId.computeIfPresent(channelId, (key, reactionPins) -> {
            reactionPins.values().forEach(Pin::unbind);
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }
        unbindAllChannelPins();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(channelService.getChannels().stream());
    }

    private Optional<String> getJsonPayload(Stream<TwoPartyPrivateChatChannel> channels) {
        ArrayList<TwoPartyPrivateChatMessageReactionDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .flatMap(message -> message.getChatMessageReactions().stream()
                                        .filter(reaction -> !reaction.isRemoved())
                                        .filter(this::isNotFromBannedUser)
                                        .map(reaction -> {
                                            try {
                                                return toDto(reaction);
                                            } catch (Exception e) {
                                                log.error("Failed to create TwoPartyPrivateChatMessageReactionDto", e);
                                                return null;
                                            }
                                        })
                                        .filter(Objects::nonNull)))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void handleReaction(TwoPartyPrivateChatMessageReaction reaction, ModificationType modificationType) {
        if (!isNotFromBannedUser(reaction)) {
            return;
        }
        TwoPartyPrivateChatMessageReactionDto dto;
        try {
            dto = toDto(reaction);
        } catch (Exception e) {
            // Mirrors getJsonPayload: this runs inside a CollectionObserver callback, where an escaping
            // exception can take the observer down for every later reaction.
            log.error("Failed to create TwoPartyPrivateChatMessageReactionDto", e);
            return;
        }
        handleReactions(Collections.singletonList(dto), modificationType);
    }

    private void handleReactions(List<TwoPartyPrivateChatMessageReactionDto> reactions,
                                 ModificationType modificationType) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        List<Subscriber> subscribers = findSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }
        toJson(reactions).ifPresent(json ->
                subscribers.forEach(subscriber -> send(json, subscriber, modificationType)));
    }

    /**
     * Bisq 2 already rejects banned senders on the inbound path, so this only covers a peer banned
     * *after* their reactions arrived — the same window {@code PrivateChatMessagesWebSocketService}
     * guards for messages.
     */
    private boolean isNotFromBannedUser(TwoPartyPrivateChatMessageReaction reaction) {
        return !bannedUserService.isUserProfileBanned(reaction.getSenderUserProfile());
    }

    private TwoPartyPrivateChatMessageReactionDto toDto(TwoPartyPrivateChatMessageReaction reaction) {
        return TwoPartyPrivateChatMessageReactionDtoMapping.fromBisq2Model(reaction);
    }
}
