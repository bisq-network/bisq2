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
import bisq.api.dto.chat.common.CommonPublicChatChannelDto;
import bisq.api.dto.mappings.chat.common.CommonPublicChatChannelDtoMapping;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
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

import static bisq.api.web_socket.subscription.Topic.PUBLIC_CHAT_CHANNELS;

/**
 * Pushes the public chat channels and their unread counts. A channel is re-sent as
 * {@link ModificationType#ADDED} whenever its unread count changes, which clients treat as an upsert by
 * id — the count is the only value on the dto that moves.
 * <p>
 * <b>Threading.</b> Same rule as {@code PrivateChatChannelsWebSocketService}, and for the same reason:
 * a lock held across a dto build would invert against the {@code ChatNotificationService} store monitor
 * the notifying thread already holds. The pushes are serialised on a single-threaded
 * {@code pushExecutor} instead and nothing here takes a lock, and the notification-driven rebuilds are
 * coalesced onto it — see {@link #resendChannelsIfChanged()}.
 * <p>
 * Unscoped, unlike the two channel-scoped public topics: the channel list is the thing a client reads
 * to learn the ids, so there is nothing to scope it by. The inherited {@code validate} accepts a
 * subscription parameter and ignores it — a client that scopes all three topics alike is served every
 * channel here rather than being told its id is unknown. Same shape as the private sibling.
 */
@Slf4j
public class PublicChatChannelsWebSocketService extends BaseWebSocketService {
    /** Bound on the one path that waits for the push thread, so a shutdown race declines the subscription instead of parking it. */
    private static final int PAYLOAD_TIMEOUT_SEC = 10;

    private final PublicChatChannels channels;
    private final ChatNotificationService chatNotificationService;
    private final ExecutorService pushExecutor;
    @Nullable
    private Pin notificationsPin;
    /**
     * The unread count each channel was last pushed or served with, keyed by channel id. Confined to
     * the {@code pushExecutor} thread, so it needs no guard.
     */
    private final Map<String, Long> lastSentUnreadCountByChannelId = new HashMap<>();
    private final AtomicBoolean resendPending = new AtomicBoolean();

    public PublicChatChannelsWebSocketService(SubscriberRepository subscriberRepository,
                                             PublicChatChannels channels,
                                             ChatNotificationService chatNotificationService) {
        this(subscriberRepository, channels, chatNotificationService,
                ExecutorFactory.newSingleThreadExecutor("PublicChatChannels-push"));
    }

    /**
     * Visible for testing, which passes a same-thread executor so the pushes are observable without
     * draining. The executor must be single-threaded — see the threading note on the class.
     */
    PublicChatChannelsWebSocketService(SubscriberRepository subscriberRepository,
                                      PublicChatChannels channels,
                                      ChatNotificationService chatNotificationService,
                                      ExecutorService pushExecutor) {
        super(subscriberRepository, PUBLIC_CHAT_CHANNELS);

        this.channels = channels;
        this.chatNotificationService = chatNotificationService;
        this.pushExecutor = pushExecutor;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // Bisq 2 publishes null here to force observers to re-evaluate, so the payload cannot be
        // inspected and the count has to be rebuilt to find out whether it moved.
        notificationsPin = chatNotificationService.getChangedNotification()
                .addObserver(notification -> resendChannelsIfChanged());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (notificationsPin != null) {
            notificationsPin.unbind();
            notificationsPin = null;
        }
        // Drained after the pin is unbound, so no new work can be queued behind us.
        ExecutorFactory.shutdownAndAwaitTermination(pushExecutor);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Coalesced, because the observable behind it fires for every chat domain and most of those are
     * trade chat: {@code ChatNotificationService} sets it once per message, whether the notification is
     * kept or consumed. Queuing one rebuild per notification would make a burst cost a full scan of the
     * notification set per channel each time, and a subscription arriving mid-burst would wait behind
     * all of them until {@link #getJsonPayload()} times out and declines it. Safe precisely because the
     * snapshot is taken when the task runs, not when it is queued, so the survivor sees the newest state.
     */
    private void resendChannelsIfChanged() {
        if (!resendPending.compareAndSet(false, true)) {
            return;
        }
        boolean submitted = submitPush(() -> {
            // Cleared first, not last: a change arriving while this rebuild is in flight has to be able
            // to queue its own. Clearing at the end would collapse it into the run that already passed
            // its snapshot, and it would never be sent.
            resendPending.set(false);
            pushIfChanged(currentChannelDtos());
        });
        if (!submitted) {
            // The task that would have cleared the flag will never run; leaving it set would suppress
            // every later resend.
            resendPending.set(false);
        }
    }

    /**
     * Runs inside an observer callback, so a rejection at shutdown must not escape into the notifying
     * thread.
     *
     * @return whether the task was accepted, so callers holding state on its behalf can undo it
     */
    private boolean submitPush(Runnable task) {
        try {
            pushExecutor.execute(task);
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("Public chat channels push executor rejected a task; the service is shutting down");
            return false;
        }
    }

    /**
     * The one path that has to return a value, so it waits for the push thread rather than handing work
     * to it. Safe: the subscribe thread holds nothing while waiting and the push thread never waits on
     * anything it could hold.
     */
    @Override
    public Optional<String> getJsonPayload() {
        try {
            return CompletableFuture.supplyAsync(() -> {
                        List<CommonPublicChatChannelDto> current = currentChannelDtos();
                        // Pushed before it is recorded: a count that moved while this subscriber was being
                        // registered is queued behind us, and would find it "already known" otherwise —
                        // served to the new subscriber, never told to the ones already connected.
                        // The new subscriber is registered too, so it can see this push arrive just before
                        // its own SubscriptionResponse with the same content; clients upsert by id, so the
                        // duplicate is harmless.
                        pushIfChanged(current);
                        remember(current);
                        return toJson(current);
                    }, pushExecutor)
                    .orTimeout(PAYLOAD_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .join();
        } catch (RejectedExecutionException | CompletionException e) {
            log.error("Failed to build the public chat channels payload", e);
            return Optional.empty();
        }
    }

    /**
     * Runs on the push thread.
     * <p>
     * Returns without recording when nobody is subscribed, where the private sibling records anyway.
     * The bookkeeping can be skipped here because every subscription is served the whole list by
     * {@link #getJsonPayload()}, which remembers what it served: a stale record can only make that
     * snapshot push one duplicate to the subscriber that is about to receive the same list anyway, and
     * clients upsert by id. It cannot swallow an update.
     */
    private void pushIfChanged(List<CommonPublicChatChannelDto> current) {
        List<Subscriber> subscribers = findSubscribers();
        if (subscribers.isEmpty()) {
            return;
        }
        List<CommonPublicChatChannelDto> changed = current.stream()
                .filter(dto -> !Objects.equals(lastSentUnreadCountByChannelId.get(dto.id()), dto.unreadCount()))
                .toList();
        if (changed.isEmpty()) {
            return;
        }
        toJson(changed).ifPresent(json -> {
            subscribers.forEach(subscriber -> send(json, subscriber, ModificationType.ADDED));
            remember(changed);
        });
    }

    private void remember(List<CommonPublicChatChannelDto> dtos) {
        dtos.forEach(dto -> lastSentUnreadCountByChannelId.put(dto.id(), dto.unreadCount()));
    }

    private List<CommonPublicChatChannelDto> currentChannelDtos() {
        return channels.getChannels()
                .map(this::findDto)
                .flatMap(Optional::stream)
                .toList();
    }

    /** Empty for a channel that could not be mapped, as {@code PublicChatDtoFactory#findDto} is. */
    private Optional<CommonPublicChatChannelDto> findDto(CommonPublicChatChannel channel) {
        try {
            return Optional.of(CommonPublicChatChannelDtoMapping.fromBisq2Model(channel,
                    chatNotificationService.getNumNotifications(channel)));
        } catch (Exception e) {
            // A mapping failure must not kill the push thread, which would take the service down for
            // every later event.
            log.error("Failed to create CommonPublicChatChannelDto", e);
            return Optional.empty();
        }
    }
}
