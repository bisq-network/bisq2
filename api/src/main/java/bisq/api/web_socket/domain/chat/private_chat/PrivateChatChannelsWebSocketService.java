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

import bisq.api.dto.chat.two_party.TwoPartyPrivateChatChannelDto;
import bisq.api.dto.mappings.chat.two_party.TwoPartyPrivateChatChannelDtoMapping;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.threading.ExecutorFactory;
import bisq.user.profile.UserProfileService;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static bisq.api.web_socket.subscription.Topic.PRIVATE_CHAT_CHANNELS;

/**
 * Pushes the two-party private chat channels (DMs) and their unread counts.
 * <p>
 * A channel is re-sent as {@link ModificationType#ADDED} whenever its unread count changes, which
 * clients treat as an upsert by id — {@code unreadCount} is the only mutable value on the dto, and
 * giving it a topic of its own would buy nothing.
 * <p>
 * <b>Threading.</b> All three push paths — the subscribe snapshot, the notification comparison and the
 * per-channel collection observer — run on a single-threaded {@code pushExecutor}. That is what keeps
 * them from interleaving; it must not be replaced by a lock. Building a dto calls
 * {@code ChatNotificationService.getNumNotifications}, which takes that service's store monitor, while
 * {@code ChatNotificationService} publishes {@code changedNotification} from <i>inside</i> that same
 * monitor and {@link bisq.common.observable.Observable#set} notifies synchronously on the caller's
 * thread. So a lock held across a dto build inverts against the store monitor: the notifying thread
 * holds the store and waits for our lock, we hold our lock and wait for the store. Both hang, and the
 * store monitor is shared by every chat domain, so trade chat and the WebSocket thread hang with them.
 * A single thread gives the ordering with nothing held.
 */
@Slf4j
public class PrivateChatChannelsWebSocketService extends BaseWebSocketService {
    /**
     * Bound on the one path that waits for the push thread. Generous, because exceeding it means
     * something is already wrong — it exists so that "wrong" is a declined subscription rather than a
     * thread parked forever; see {@link #getJsonPayload()}.
     */
    private static final int PAYLOAD_TIMEOUT_SEC = 10;

    private final TwoPartyPrivateChatChannelService channelService;
    private final ChatNotificationService chatNotificationService;
    private final UserProfileService userProfileService;
    @Nullable
    private Pin channelsPin;
    @Nullable
    private Pin notificationsPin;
    /**
     * The dtos as last known to be on the client, keyed by channel id. Compared against on every
     * notification change so that a change in another chat domain — which is most of them — is a
     * no-op here instead of re-serialising the whole DM list to every subscriber.
     * <p>
     * Keyed on the whole dto rather than just the unread count on purpose: the dto also carries the
     * re-resolved peer profile, so a rename is picked up by the same comparison — though only when
     * something else fires the notification observable, since a profile update does not.
     * <p>
     * Kept current even while nobody is subscribed, so it means "the state we know of" rather than
     * "the last thing we sent". Skipping the update while unsubscribed would let it go stale and
     * silently swallow the next real change — background → foreground on mobile is exactly that
     * sequence.
     * <p>
     * Confined to the {@code pushExecutor} thread, so it needs no guard; see the threading note on the
     * class.
     */
    private final Map<String, TwoPartyPrivateChatChannelDto> lastKnownChannels = new HashMap<>();
    private final ExecutorService pushExecutor;
    /**
     * Whether a full rebuild is already queued. Not confined to the push thread — it is set by whichever
     * thread publishes the notification — hence the atomic.
     */
    private final AtomicBoolean resendPending = new AtomicBoolean();

    public PrivateChatChannelsWebSocketService(SubscriberRepository subscriberRepository,
                                               TwoPartyPrivateChatChannelService channelService,
                                               ChatNotificationService chatNotificationService,
                                               UserProfileService userProfileService) {
        this(subscriberRepository, channelService, chatNotificationService, userProfileService,
                ExecutorFactory.newSingleThreadExecutor("PrivateChatChannels-push"));
    }

    /**
     * Visible for testing, which passes a same-thread executor so the pushes are observable without
     * draining. The executor must be single-threaded — see the threading note on the class.
     */
    PrivateChatChannelsWebSocketService(SubscriberRepository subscriberRepository,
                                        TwoPartyPrivateChatChannelService channelService,
                                        ChatNotificationService chatNotificationService,
                                        UserProfileService userProfileService,
                                        ExecutorService pushExecutor) {
        super(subscriberRepository, PRIVATE_CHAT_CHANNELS);

        this.channelService = channelService;
        this.chatNotificationService = chatNotificationService;
        this.userProfileService = userProfileService;
        this.pushExecutor = pushExecutor;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        channelsPin = channelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(TwoPartyPrivateChatChannel channel) {
                send(channel, ModificationType.ADDED);
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof TwoPartyPrivateChatChannel channel) {
                    send(channel, ModificationType.REMOVED);
                }
            }

            @Override
            public void onCleared() {
                sendClearedAsRemoved();
            }
        });

        // Bisq 2 deliberately publishes null here to force observers to re-evaluate (a notification
        // excludes isConsumed from equals), so the payload cannot be inspected and every channel has to
        // be rebuilt to find out what moved.
        notificationsPin = chatNotificationService.getChangedNotification()
                .addObserver(notification -> resendChannelsIfChanged());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (channelsPin != null) {
            channelsPin.unbind();
            channelsPin = null;
        }
        if (notificationsPin != null) {
            notificationsPin.unbind();
            notificationsPin = null;
        }
        // Drained after the pins are unbound, so no new work can be queued behind us. lastKnownChannels
        // is deliberately not cleared: the drain gives up after 100ms and a push task can outlive it,
        // so clearing here would be the off-thread write the field's confinement rules out. Nothing to
        // reclaim anyway — shutdown is terminal.
        ExecutorFactory.shutdownAndAwaitTermination(pushExecutor);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Records the snapshot it serves, so the map keeps tracking what the client knows. Without that a
     * dto that moved without a notification event — the peer profile is re-resolved on every build —
     * and then moved back would compare equal to the pre-subscribe state and swallow the push.
     * <p>
     * Which is why it pushes that snapshot to the already-subscribed first; see {@link #pushIfChanged}.
     * <p>
     * The one path that has to return a value, so it waits for the push thread rather than handing work
     * to it. Safe: the subscribe thread holds nothing while waiting ({@code SubscriberRepository.add}
     * released its lock already) and the push thread never waits on anything it could hold.
     */
    @Override
    public Optional<String> getJsonPayload() {
        try {
            return CompletableFuture.supplyAsync(() -> {
                        Map<String, TwoPartyPrivateChatChannelDto> current = currentChannelDtos();
                        pushIfChanged(current);
                        remember(current);
                        return toJson(new ArrayList<>(current.values()));
                    }, pushExecutor)
                    // Bounded because the wait has a second way to end badly, one the catch below cannot
                    // reach on its own: shutdown drains the queue, so a task accepted but not yet started
                    // is discarded and its future never completes. Unbounded, that parks this thread for
                    // the life of the process. The timeout arrives as a CompletionException, which is
                    // already handled.
                    .orTimeout(PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .join();
        } catch (RejectedExecutionException | CompletionException e) {
            // Reachable when a subscription races shutdown, either by being rejected outright or by
            // timing out above. Returning empty declines the subscription rather than letting it through
            // with no data.
            log.error("Failed to build the private chat channels payload", e);
            return Optional.empty();
        }
    }

    private void resendChannelsIfChanged() {
        // See the threading note on the class before turning any of this into a lock.
        //
        // Coalesced, because this fires for every chat domain and most of those are trade chat. Queuing
        // one rebuild per notification would make a burst cost a full rebuild each, and a subscription
        // arriving mid-burst waits behind all of them. Safe precisely because the snapshot is taken when
        // the task runs, not when it is queued, so the survivor sees the newest state.
        if (!resendPending.compareAndSet(false, true)) {
            return;
        }
        boolean submitted = submitPush(() -> {
            // Cleared first, not last: a change arriving while this rebuild is in flight has to be able
            // to queue its own. Clearing at the end would collapse it into the run that already passed
            // its snapshot, and it would never be sent.
            resendPending.set(false);

            Map<String, TwoPartyPrivateChatChannelDto> current = currentChannelDtos();
            pushIfChanged(current);
            // Recorded even when nothing was sent: with nobody subscribed there is nothing to send, but
            // the state still moved, and skipping the bookkeeping would let the next real change compare
            // equal and be swallowed.
            remember(current);
        });
        if (!submitted) {
            // The task that would have cleared the flag will never run; leaving it set would suppress
            // every later resend.
            resendPending.set(false);
        }
    }

    /**
     * Both observer-driven paths funnel through here: they run inside observer callbacks, so a
     * rejection at shutdown must not escape into the notifying thread.
     *
     * @return whether the task was accepted, so callers holding state on its behalf can undo it
     */
    private boolean submitPush(Runnable task) {
        try {
            pushExecutor.execute(task);
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("Private chat channels push executor rejected a task; the service is shutting down");
            return false;
        }
    }

    private Map<String, TwoPartyPrivateChatChannelDto> currentChannelDtos() {
        return channelService.getChannels().stream()
                .map(channel -> {
                    try {
                        return toDto(channel);
                    } catch (Exception e) {
                        log.error("Failed to create TwoPartyPrivateChatChannelDto", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                // Merge function only so a future duplicate id cannot throw inside an observer callback.
                .collect(Collectors.toMap(TwoPartyPrivateChatChannelDto::id, dto -> dto, (a, b) -> a));
    }

    /** Records the dtos as what the client now knows. Runs on the push thread. */
    private void remember(Map<String, TwoPartyPrivateChatChannelDto> current) {
        lastKnownChannels.clear();
        lastKnownChannels.putAll(current);
    }

    /**
     * Sends the full list to every subscriber if it differs from what they are known to hold. Both
     * whole-list paths funnel through here, and both call it before {@link #remember}, because
     * {@code lastKnownChannels} is one map for all subscribers: recording a snapshot that only one of
     * them received is what loses an update.
     * <p>
     * Concretely, without this in the subscribe path: a second device subscribes while a DM arrives, its
     * task wins the push thread, and the state it snapshots is recorded as known to everyone while only
     * that device is served it. The notification's own task then finds nothing changed and stays quiet,
     * so the first device never learns its unread count moved. It recovers on the next change — that
     * produces a different map and pushes to all — but until then a badge sits short.
     * <p>
     * On the subscribe path the subscribing subscriber is already registered (see
     * {@link #getJsonPayload()}), so it gets this event ahead of its own subscription response. Harmless:
     * both carry the same {@code current}, and the client treats the list as an upsert by id.
     * <p>
     * Runs on the push thread.
     */
    private void pushIfChanged(Map<String, TwoPartyPrivateChatChannelDto> current) {
        if (current.equals(lastKnownChannels)) {
            return;
        }
        List<Subscriber> subscribers = findSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }
        toJson(new ArrayList<>(current.values()))
                .ifPresent(json -> subscribers.forEach(subscriber ->
                        send(json, subscriber, ModificationType.ADDED)));
    }

    /**
     * The per-channel path, driven by the collection observer. Shares the push thread with
     * {@link #resendChannelsIfChanged()} so the two cannot interleave: a REMOVED slipping between that
     * method's snapshot and its send would be undone by the full list landing afterwards and
     * resurrecting the channel as an upsert — and that does not self-correct, since leaving a quiet
     * channel emits no notification to trigger the next comparison.
     */
    private void send(TwoPartyPrivateChatChannel channel, ModificationType modificationType) {
        submitPush(() -> {
            List<Subscriber> subscribers = findSubscribers();
            if (subscribers.isEmpty()) {
                // Nothing was told, so nothing to record. The next comparison sees the difference and
                // pushes, which is the safe direction.
                return;
            }
            TwoPartyPrivateChatChannelDto dto;
            try {
                dto = toDto(channel);
            } catch (Exception e) {
                // Mirrors getJsonPayload: a mapping failure must not kill the push thread, which would
                // take the service down for every later event.
                log.error("Failed to create TwoPartyPrivateChatChannelDto", e);
                return;
            }
            // The payload is defined as a list to support batch data delivery at subscribe.
            toJson(Collections.singletonList(dto))
                    .ifPresent(json -> {
                        subscribers.forEach(subscriber -> send(json, subscriber, modificationType));
                        if (modificationType == ModificationType.REMOVED) {
                            lastKnownChannels.remove(dto.id());
                        } else {
                            lastKnownChannels.put(dto.id(), dto);
                        }
                    });
        });
    }

    /**
     * The bulk-clear path. It exists because {@code ChannelStore#applyPersisted} replaces the whole set
     * through {@code setAll}, and {@link CollectionObserver#onAllSet} is defined as {@code onCleared()}
     * followed by {@code onAllAdded(values)} — so a clear does reach us, contrary to what a "channels
     * are only removed one by one" reading suggests.
     * <p>
     * Today that only happens at start-up, empty to full, where the re-add covers everything and this is
     * a no-op over an empty map. It is still reported, because a full-list push is an
     * {@link ModificationType#ADDED} and an upsert cannot express a deletion: the per-channel REMOVED is
     * the only signal that can. A shrinking {@code setAll} would otherwise strand the dropped channels on
     * the client for good, since the next comparison only ever adds.
     * <p>
     * Reported from the last known state rather than from the collection, which is already empty by now.
     */
    private void sendClearedAsRemoved() {
        submitPush(() -> {
            List<Subscriber> subscribers = findSubscribers();
            if (!subscribers.isEmpty() && !lastKnownChannels.isEmpty()) {
                toJson(new ArrayList<>(lastKnownChannels.values()))
                        .ifPresent(json -> subscribers.forEach(subscriber ->
                                send(json, subscriber, ModificationType.REMOVED)));
            }
            // Cleared either way: with no subscribers nothing was told, and the next comparison sees the
            // difference and pushes, which is the safe direction.
            lastKnownChannels.clear();
        });
    }

    private TwoPartyPrivateChatChannelDto toDto(TwoPartyPrivateChatChannel channel) {
        // The profile embedded in the persisted channel is a stale snapshot; always re-resolve, or a
        // peer's nickname change never reaches the client.
        //
        // One getNumNotifications per channel, each taking the notification store's monitor and streaming
        // the whole store. Grouping a single pass by channel id would collapse that to one, but it would
        // also move what the tests stub from a count to a stream of notifications, and the numbers here
        // are small — a user has a handful of DMs. Revisit if the store or the channel count grows.
        return TwoPartyPrivateChatChannelDtoMapping.fromBisq2Model(channel,
                userProfileService.getManagedUserProfile(channel.getPeer()),
                chatNotificationService.getNumNotifications(channel));
    }
}
