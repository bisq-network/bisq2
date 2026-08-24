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
import java.util.HashMap;
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
    /**
     * Held per channel, not as a flat message-id map, so that leaving a channel can unbind exactly its
     * reaction observers. Leaving a DM is a routine user action, unlike leaving a trade channel, so a
     * flat map would accumulate pins for the lifetime of the process.
     */
    private final Map<String, ChannelPins> pinsByChannelId = new ConcurrentHashMap<>();

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
                bindChannel(channel);
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof TwoPartyPrivateChatChannel channel) {
                    unbindChannelPins(channel);
                }
            }

            @Override
            public void onCleared() {
                unbindAllChannelPins();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    private void bindChannel(TwoPartyPrivateChatChannel channel) {
        String channelId = channel.getId();
        ChannelPins pins = new ChannelPins(channel);
        // Published before anything is observed, because addObserver replays the messages already on
        // the channel and each of those replayed callbacks stores its reaction pin into this instance.
        ChannelPins previous = pinsByChannelId.put(channelId, pins);
        if (previous != null) {
            previous.close();
        }

        Pin messagesPin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(TwoPartyPrivateChatMessage message) {
                bindMessageReactions(message, pins);
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
        if (!pins.setMessagesPin(messagesPin)) {
            messagesPin.unbind();
        }

        // A channel is in the collection before its add is notified, so it is missing here only if it
        // was removed while we were registering — and that removal ran past pinsByChannelId before we
        // published to it, so nobody else will collect this.
        if (!isLive(channel)) {
            pinsByChannelId.remove(channelId, pins);
            pins.close();
        }
    }

    /**
     * Reference identity rather than {@code contains}, which compares by {@code equals}: only the channel
     * id is included there, and ids are deterministic
     * ({@link TwoPartyPrivateChatChannel#createId}), so a re-created channel is equal to the one it
     * replaced. Asking by equality would answer for the successor instead of for this instance.
     */
    private boolean isLive(TwoPartyPrivateChatChannel channel) {
        return channelService.getChannels().stream().anyMatch(live -> live == channel);
    }

    private void bindMessageReactions(TwoPartyPrivateChatMessage message, ChannelPins pins) {
        Pin reactionsPin = message.getChatMessageReactions().addObserver(new CollectionObserver<>() {
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
        // Created before the store rather than inside it: addObserver replays the reactions already on
        // the message, and those callbacks reach findSubscribers and send. Holding the ChannelPins
        // monitor across that would be a lock around a callback.
        if (!pins.putReactionPin(message.getId(), reactionsPin)) {
            reactionsPin.unbind();
        }
    }

    /** Unconditional, unlike {@link #unbindChannelPins}: nothing survives a clear or a shutdown. */
    private void unbindAllChannelPins() {
        new ArrayList<>(pinsByChannelId.keySet()).forEach(channelId -> {
            ChannelPins pins = pinsByChannelId.remove(channelId);
            if (pins != null) {
                pins.close();
            }
        });
    }

    /**
     * Unbinds only this channel's observers — the message pin and every reaction pin under it — and only
     * if this exact instance still owns them.
     * <p>
     * Keying the teardown on the id alone would let a departing channel close its successor's observers.
     * Channel ids are deterministic, and {@code ObservableCollection#remove} drops the element from the
     * backing set before it notifies, so an inbound message from the same peer finds no channel, creates
     * a second instance under the same id and binds it — all while this callback is still pending. The
     * id would then resolve to the live channel's pins, and closing them would leave it in the collection
     * observed by nobody, with no further event to rebuild it.
     */
    private void unbindChannelPins(TwoPartyPrivateChatChannel channel) {
        String channelId = channel.getId();
        ChannelPins pins = pinsByChannelId.get(channelId);
        // The conditional remove closes the gap after the read: a bind that publishes its own instance in
        // between keeps it, and we leave with nothing to close, because that bind already closed ours.
        if (pins != null && pins.isOwnedBy(channel) && pinsByChannelId.remove(channelId, pins)) {
            pins.close();
        }
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

    /**
     * One channel's observers, held together so that binding them and tearing them down cannot
     * interleave into a state nothing collects. {@link Pin#unbind} only drops the observer from a
     * copy-on-write list, so a callback already in flight when the channel is removed still runs to
     * completion. Such a callback holds this instance directly rather than looking it up by channel id,
     * so it finds the state that was closed underneath it, is told so, and unbinds the pin it just
     * created — where a map lookup would instead recreate the entry the teardown had removed.
     * <p>
     * The reaction pins live here rather than being resolved back out of {@code pinsByChannelId},
     * because {@code addObserver} replays the collection synchronously: binding the message observer
     * runs the reaction binding inside the enclosing call, so a lookup there would be a recursive
     * update on the key that call is already working on — which {@code ConcurrentHashMap} does not
     * support. For the same reason the class holds no {@code compute} at all; {@link #closed} is what
     * makes the plain {@code put} and {@code remove} safe, since whichever of two racing binds loses is
     * closed by the winner and cleans up after itself.
     * <p>
     * Every field is guarded by the instance monitor, but pins are always created by the caller and
     * only handed over here, so the monitor is never held across an observer callback.
     */
    private static final class ChannelPins {
        /**
         * Which channel these pins belong to, so a teardown can tell this binding apart from a later one
         * filed under the same id. Compared by reference and never by {@code equals}, which for channels
         * is the id and would treat a successor as this instance.
         */
        private final TwoPartyPrivateChatChannel owner;
        private final Map<String, Pin> reactionPinsByMessageId = new HashMap<>();
        @Nullable
        private Pin messagesPin;
        private boolean closed;

        private ChannelPins(TwoPartyPrivateChatChannel owner) {
            this.owner = owner;
        }

        private boolean isOwnedBy(TwoPartyPrivateChatChannel candidate) {
            return owner == candidate;
        }

        /** @return false if the channel is already gone, leaving the pin for the caller to unbind. */
        private synchronized boolean setMessagesPin(Pin pin) {
            if (closed) {
                return false;
            }
            messagesPin = pin;
            return true;
        }

        /** @return false if the channel is already gone, leaving the pin for the caller to unbind. */
        private synchronized boolean putReactionPin(String messageId, Pin pin) {
            if (closed) {
                return false;
            }
            Pin previous = reactionPinsByMessageId.put(messageId, pin);
            if (previous != null) {
                previous.unbind();
            }
            return true;
        }

        private synchronized void close() {
            closed = true;
            if (messagesPin != null) {
                messagesPin.unbind();
                messagesPin = null;
            }
            reactionPinsByMessageId.values().forEach(Pin::unbind);
            reactionPinsByMessageId.clear();
        }
    }
}
