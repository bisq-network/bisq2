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

import bisq.api.dto.DtoMappings;
import bisq.api.dto.chat.two_party.TwoPartyPrivateChatMessageDto;
import bisq.api.dto.mappings.chat.two_party.TwoPartyPrivateChatMessageDtoMapping;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.PRIVATE_CHAT_MESSAGES;

/**
 * Pushes two-party private chat (DM) messages. Structurally the trade-chat sibling
 * {@code TradeChatMessagesWebSocketService}, with one addition: messages from banned senders are
 * dropped, matching what Bisq 2 desktop and the mobile node facade do.
 * <p>
 * Beware that this filter and the channel's unread count disagree. The count comes from
 * {@code ChatNotificationService}, which drops *ignored* senders and keeps banned ones — the opposite
 * choice. So a peer banned after their messages arrived leaves a channel reporting unread messages
 * this stream no longer sends. Ignored senders are kept on purpose: the client hides them against its
 * own list of ignored ids, so un-ignoring brings the conversation back.
 */
@Slf4j
public class PrivateChatMessagesWebSocketService extends BaseWebSocketService {
    private final TwoPartyPrivateChatChannelService channelService;
    private final UserProfileService userProfileService;
    private final BannedUserService bannedUserService;
    @Nullable
    private Pin channelsPin;
    private final Map<String, ChannelBinding> bindingsByChannelId = new ConcurrentHashMap<>();
    /**
     * Bumped by every bulk teardown before it sweeps the map. A bind captures it before registering
     * anything and takes its own entry out again if it has moved by the time it has published, because
     * the owner check alone cannot see a bulk teardown: a shutdown does not touch the channel collection
     * at all, and a clear empties it outside the bin lock the publishing compute holds, so either can
     * fall between that read and the write, or entirely after it. See {@link #bindChannel}.
     */
    private final AtomicLong teardownGeneration = new AtomicLong();
    /**
     * Set first thing in {@link #shutdown} and never cleared. Observers are notified from a copy-on-write
     * snapshot, so a callback that began before the shutdown unbound its pin still reaches this service
     * afterwards. A bind starting that late captures the generation already bumped and finds its owner
     * live, so only this flag keeps it out of the map, and for that it must be set before the bump: a
     * bind that captured the bumped generation has to find the flag set, or its compare after publishing
     * reads unchanged and the entry stays. The emission path checks it too.
     */
    private volatile boolean shutdownStarted;

    public PrivateChatMessagesWebSocketService(SubscriberRepository subscriberRepository,
                                               TwoPartyPrivateChatChannelService channelService,
                                               UserProfileService userProfileService,
                                               BannedUserService bannedUserService) {
        super(subscriberRepository, PRIVATE_CHAT_MESSAGES);

        this.channelService = channelService;
        this.userProfileService = userProfileService;
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
                    unbindChannel(channel);
                }
            }

            @Override
            public void onCleared() {
                unbindAllChannels();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    private void bindChannel(TwoPartyPrivateChatChannel channel) {
        // An optimisation, not the check that keeps this bind out of the map: install reads the flag
        // again inside the compute. This only spares a bind that is already late the replay below.
        if (shutdownStarted) {
            return;
        }
        long generation = teardownGeneration.get();
        // Registered before the map is touched, because addObserver replays the messages already on the
        // channel and each replayed callback reaches findSubscribers and send. Doing it inside the compute
        // that publishes the binding would hold a ConcurrentHashMap bin lock across those sends.
        Pin messagesPin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(TwoPartyPrivateChatMessage message) {
                handleAddedMessage(message);
            }

            @Override
            public void onRemoved(Object element) {
                // Private chat messages cannot be removed
            }

            @Override
            public void onCleared() {
                // Private chat messages cannot be removed
            }
        });

        if (!install(channel.getId(), new ChannelBinding(channel, messagesPin))) {
            messagesPin.unbind();
            return;
        }
        // Compared only now, after publishing, because a bulk teardown can also land after the compute
        // and then its sweep walks a key snapshot this write may not be in. Reading the generation
        // unchanged here means the write preceded the bump, and so the snapshot; reading it moved means
        // only this bind can take the entry out, which is the right outcome whether the teardown ran
        // before or after the compute.
        if (teardownGeneration.get() != generation) {
            unbindChannel(channel);
        }
    }

    /**
     * Publishes the binding, but only while its owner is still in the channel collection and the
     * shutdown has not started.
     * <p>
     * The check and the write are one map update because the two can be separated by the same thing the
     * teardown guards against: observer callbacks are synchronous per operation but nothing serializes
     * them across threads, so this callback can be resumed after its channel was left and a successor
     * under the same id was already bound. An unconditional put would then displace the live successor's
     * binding and unbind it, and since the owner is no longer live the caller would discard its own —
     * leaving the live channel observed by nobody, with no further event to rebuild it.
     * <p>
     * Asking only whether the owner is live is enough, because at most one channel per id is ever live:
     * the collection is a set and a channel's equality is its id. It also relies on an instance being
     * added to the collection at most once, which every caller honours by constructing a new one; a
     * re-added instance would let the self-unbind in {@link #bindChannel} remove its own fresh binding.
     * A live owner means whatever is filed under the id belongs to an instance that is gone, and
     * displacing it is right; a dead owner must not touch what is there, which is either nothing or the
     * successor's. A clear landing between this read and the write is invisible here, and a shutdown
     * never touches the collection; the caller handles the first after publishing
     * ({@link #teardownGeneration}) and the flag covers the second ({@link #shutdownStarted}).
     * <p>
     * Both reads run under the bin lock of this key, so they must stay lock-free and must never reach
     * back into this map: {@link #isLive} is a plain scan of the channel set, and the domain service
     * holds its own monitor while it notifies the removal that takes this same lock in
     * {@link #unbindChannel}.
     *
     * @return false if the owner is gone or the shutdown has started, leaving the pin for the caller
     * to unbind.
     */
    private boolean install(String channelId, ChannelBinding binding) {
        AtomicReference<ChannelBinding> displaced = new AtomicReference<>();
        ChannelBinding current = bindingsByChannelId.compute(channelId, (id, present) -> {
            if (shutdownStarted || !isLive(binding.owner())) {
                return present;
            }
            displaced.set(present);
            return binding;
        });
        ChannelBinding stale = displaced.get();
        if (stale != null) {
            stale.messagesPin().unbind();
        }
        return current == binding;
    }

    /**
     * Unbinds this channel's message observer, and only if this exact instance still owns it.
     * <p>
     * Keying the teardown on the id alone would let a departing channel close its successor's observer.
     * Channel ids are deterministic, and {@code ObservableCollection#remove} drops the element from the
     * backing set before it notifies, so an inbound message from the same peer finds no channel, creates
     * a second instance under the same id and binds it — all while this callback is still pending. The
     * id would then resolve to the live channel's pin, and unbinding it would leave that channel in the
     * collection observed by nobody, with no further event to rebuild it.
     * <p>
     * Done as one map update, like {@link #install}, so that the two cannot cross: a bind that has read
     * its owner as live before the channel left the set, and a teardown that has looked here before that
     * bind published, would otherwise leave behind a binding for a channel that is gone.
     */
    private void unbindChannel(TwoPartyPrivateChatChannel channel) {
        AtomicReference<ChannelBinding> removed = new AtomicReference<>();
        bindingsByChannelId.computeIfPresent(channel.getId(), (id, present) -> {
            if (!present.isOwnedBy(channel)) {
                return present;
            }
            removed.set(present);
            return null;
        });
        ChannelBinding binding = removed.get();
        if (binding != null) {
            binding.messagesPin().unbind();
        }
    }

    /**
     * Unconditional, unlike {@link #unbindChannel}: nothing survives a clear or a shutdown. Swept key by
     * key rather than snapshot-then-clear, so that an entry published between the two is never dropped
     * with its pin still registered; the generation bump before the sweep is what makes a bind that
     * publishes after it take its own entry out again, see {@link #teardownGeneration}.
     */
    private void unbindAllChannels() {
        teardownGeneration.incrementAndGet();
        new ArrayList<>(bindingsByChannelId.keySet()).forEach(channelId -> {
            ChannelBinding binding = bindingsByChannelId.remove(channelId);
            if (binding != null) {
                binding.messagesPin().unbind();
            }
        });
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

    /**
     * A channel's message observer together with the instance it was registered for. The owner is what
     * lets {@link #install} and {@link #unbindChannel} tell this binding apart from one a newer channel
     * filed under the same id, and it is compared by reference: {@code equals} on a channel is its id,
     * which is exactly the thing that cannot tell the two apart.
     */
    private record ChannelBinding(TwoPartyPrivateChatChannel owner, Pin messagesPin) {
        private boolean isOwnedBy(TwoPartyPrivateChatChannel candidate) {
            return owner == candidate;
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }
        unbindAllChannels();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(channelService.getChannels().stream());
    }

    private Optional<String> getJsonPayload(Stream<TwoPartyPrivateChatChannel> channels) {
        ArrayList<TwoPartyPrivateChatMessageDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .filter(this::isNotFromBannedUser)
                                .map(message -> {
                                    try {
                                        return toDto(message);
                                    } catch (Exception e) {
                                        log.error("Failed to create TwoPartyPrivateChatMessageDto", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void handleAddedMessage(TwoPartyPrivateChatMessage message) {
        // Observer callbacks can still arrive after shutdown, see shutdownStarted: this keeps a push from
        // starting once the shutdown has returned (one already past the check still completes). A leave
        // gets no such guard: a callback already captured can still push one message for the channel,
        // which the client sees together with the channel's own removal.
        if (shutdownStarted || !isNotFromBannedUser(message)) {
            return;
        }
        TwoPartyPrivateChatMessageDto dto;
        try {
            dto = toDto(message);
        } catch (Exception e) {
            // Mirrors getJsonPayload: this runs inside a CollectionObserver callback, where an escaping
            // exception can take the observer down for every later message.
            log.error("Failed to create TwoPartyPrivateChatMessageDto", e);
            return;
        }
        handleAddedMessages(Collections.singletonList(dto));
    }

    private void handleAddedMessages(List<TwoPartyPrivateChatMessageDto> messages) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        List<Subscriber> subscribers = findSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }
        toJson(messages).ifPresent(json ->
                subscribers.forEach(subscriber -> send(json, subscriber, ModificationType.ADDED)));
    }

    /**
     * Bisq 2 already rejects banned senders on the inbound path, so this only covers a peer banned
     * *after* their messages arrived — which is why desktop re-checks it too.
     */
    private boolean isNotFromBannedUser(TwoPartyPrivateChatMessage message) {
        return !bannedUserService.isUserProfileBanned(message.getSenderUserProfile());
    }

    private TwoPartyPrivateChatMessageDto toDto(TwoPartyPrivateChatMessage message) {
        Optional<UserProfileDto> citationAuthorUserProfile = message.getCitation()
                .flatMap(citation -> userProfileService.findUserProfile(citation.getAuthorUserProfileId()))
                .map(DtoMappings.UserProfileMapping::fromBisq2Model);
        return TwoPartyPrivateChatMessageDtoMapping.fromBisq2Model(message, citationAuthorUserProfile);
    }
}
