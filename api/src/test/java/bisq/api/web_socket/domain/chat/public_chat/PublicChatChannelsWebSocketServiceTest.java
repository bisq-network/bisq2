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
import bisq.api.chat.common.PublicChatTestMocks.QueueingExecutorService;
import bisq.api.chat.common.PublicChatTestMocks.SameThreadExecutorService;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.SubDomain;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static bisq.api.chat.common.PublicChatTestMocks.DISCUSSION_ID;
import static bisq.api.chat.common.PublicChatTestMocks.SUPPORT_ID;
import static bisq.api.chat.common.PublicChatTestMocks.mockChannel;
import static bisq.api.chat.common.PublicChatTestMocks.mockSubscriber;
import static bisq.api.chat.common.PublicChatTestMocks.publicChatChannels;
import static bisq.api.chat.common.PublicChatTestMocks.sentJson;
import static bisq.api.chat.common.PublicChatTestMocks.subscribed;
import static bisq.api.chat.common.PublicChatTestMocks.subscriptionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The notification observer fires for every chat domain, so this service only pushes when the channel's
 * unread count actually moved. The lock inversion the threading note describes is covered by the private
 * chat sibling's test; this service follows the same rule with the same executor shape.
 */
class PublicChatChannelsWebSocketServiceTest {
    private CommonPublicChatChannel channel;
    private CommonPublicChatChannel supportChannel;
    private PublicChatChannels channels;
    private Observable<ChatNotification> changedNotification;
    private ChatNotificationService chatNotificationService;
    private SubscriberRepository subscriberRepository;
    private Subscriber subscriber;
    private PublicChatChannelsWebSocketService service;

    @BeforeEach
    void setUp() {
        channel = mockChannel(SubDomain.DISCUSSION_BISQ, new ObservableSet<>());
        supportChannel = mockChannel(SubDomain.SUPPORT_SUPPORT, new ObservableSet<>());
        channels = publicChatChannels(channel, supportChannel);

        changedNotification = new Observable<>();
        chatNotificationService = mock(ChatNotificationService.class, RETURNS_DEEP_STUBS);
        when(chatNotificationService.getChangedNotification()).thenReturn(changedNotification);
        unreadCount(0);

        subscriber = mockSubscriber(Topic.PUBLIC_CHAT_CHANNELS, "subscriber-1");
        subscriberRepository = mock(SubscriberRepository.class);
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_CHANNELS, Set.of(subscriber));

        service = new PublicChatChannelsWebSocketService(subscriberRepository, channels,
                chatNotificationService, new SameThreadExecutorService());
        service.initialize().join();
        // Observable notifies at registration, so initialising with a subscriber present already pushed
        // the current count once; the tests below only care about what happens after that.
        clearInvocations(subscriber);
    }

    @Test
    void aChangedUnreadCountIsPushedToSubscribers() {
        unreadCount(1);

        fireNotificationChange();

        // The event carries the payload as an escaped JSON string, hence the escaped quotes.
        assertThat(sentJson(subscriber)).contains("\"modificationType\":\"ADDED\"").contains("\\\"unreadCount\\\":1");
    }

    @Test
    void anUnchangedUnreadCountIsNotPushed() {
        unreadCount(1);
        fireNotificationChange();
        clearInvocations(subscriber);

        fireNotificationChange();

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The snapshot records what the client knows, so a notification for the same count stays quiet.
     * <p>
     * Taken with nobody subscribed on purpose. With a subscriber present the push one line earlier in
     * {@code getJsonPayload} has already recorded the same counts, so the {@code remember} this test
     * exists for could be deleted and the test would not notice.
     */
    @Test
    void theSnapshotCountsAsKnownToTheClient() {
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_CHANNELS, Set.of());
        unreadCount(2);

        String payload = service.getJsonPayload().orElseThrow();
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_CHANNELS, Set.of(subscriber));
        fireNotificationChange();

        assertThat(payload).contains("\"unreadCount\":2");
        verify(subscriber, never()).send(anyString());
    }

    /**
     * Interleaving: a second client subscribes while the count moves, and its snapshot runs before the
     * queued push. Recording the snapshot as "known" would swallow that push, and the already-connected
     * client would never learn the new count — so the snapshot pushes to it first.
     */
    @Test
    void aSnapshotDoesNotSwallowThePushToAnAlreadySubscribedClient() {
        Subscriber late = mockSubscriber(Topic.PUBLIC_CHAT_CHANNELS, "subscriber-2");
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_CHANNELS, Set.of(subscriber, late));
        unreadCount(1);

        service.getJsonPayload().orElseThrow();

        assertThat(sentJson(subscriber)).contains("\"modificationType\":\"ADDED\"").contains("\\\"unreadCount\\\":1");
        // The one subscribing is pushed too, and upserts the duplicate against its own snapshot; without
        // this the test would still pass on a push narrowed to whoever was there before.
        assertThat(sentJson(late)).contains("\\\"unreadCount\\\":1");
    }

    /** Only the channel whose count moved is re-sent; the other one is already known to the client. */
    @Test
    void aCountThatMovedOnOneChannelPushesOnlyThatChannel() {
        unreadCount(supportChannel, 4);

        fireNotificationChange();

        assertThat(sentJson(subscriber)).contains(SUPPORT_ID).doesNotContain(DISCUSSION_ID);
    }

    /**
     * Nothing was told, so nothing was recorded: the count that moved while the client was away is
     * pushed once it comes back.
     * <p>
     * Asserting only that nothing was sent while unsubscribed would prove nothing — with the subscriber
     * out of the repository that assertion holds even if the service does nothing at all. What is
     * asserted is the bookkeeping the empty-subscriber return skips.
     */
    @Test
    void aCountThatMovedWithoutSubscribersIsPushedOnceOneReturns() {
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_CHANNELS, Set.of());
        unreadCount(1);
        fireNotificationChange();

        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_CHANNELS, Set.of(subscriber));
        fireNotificationChange();

        assertThat(sentJson(subscriber)).contains("\\\"unreadCount\\\":1");
    }

    /**
     * Unscoped, so the inherited {@code validate} takes a subscription parameter and ignores it rather
     * than rejecting an id it has no list to check against. That is what lets a client scope all three
     * public topics alike and still be served every channel here; the two channel-scoped topics do
     * reject an unknown id, see
     * {@code PublicChatMessagesWebSocketServiceTest#subscribingToAnUnknownChannelIsRejected}.
     */
    @Test
    void aParameterOnTheChannelsTopicIsAcceptedAndIgnored() {
        assertThatCode(() -> service.validate(subscriptionRequest("discussion.nope")))
                .doesNotThrowAnyException();

        String payload = service.getJsonPayload(Optional.of(SUPPORT_ID)).orElseThrow();

        assertThat(payload).contains(SUPPORT_ID).contains(DISCUSSION_ID);
    }

    /**
     * A channel that cannot be mapped costs that channel and nothing else. Without the guard the throw
     * leaves {@code currentChannelDtos} on the way out and the whole rebuild is lost, so the channel
     * that did move is never sent either — and since the counts are only compared against what was last
     * sent, nothing later re-sends it. The same guard on {@code PublicChatDtoFactory#findDto} is covered
     * by {@code PublicChatRestApiTest#historySkipsAMessageWhoseAuthorVanishedMidMapping}; this is its
     * counterpart on the channel list.
     */
    @Test
    void aChannelThatCannotBeMappedCostsOnlyThatChannel() {
        when(chatNotificationService.getNumNotifications(channel)).thenThrow(new RuntimeException("boom"));
        unreadCount(supportChannel, 7);

        fireNotificationChange();

        assertThat(sentJson(subscriber)).contains(SUPPORT_ID).doesNotContain(DISCUSSION_ID);
    }

    /**
     * The subscribe path is the one that hands work to the push thread and waits for it, so a submit
     * that loses the race with a shutdown has to decline the subscription rather than let the rejection
     * out into the subscribe thread. Empty is what {@code SubscriptionService} turns into an answer the
     * client can act on.
     */
    @Test
    void aSnapshotRacingTheShutdownIsDeclinedRatherThanThrowing() {
        QueueingExecutorService executor = queueingService();
        executor.rejectNext();

        assertThat(service.getJsonPayload()).isEmpty();
    }

    @Test
    void shutdownStopsListeningToNotifications() {
        service.shutdown().join();
        unreadCount(1);

        fireNotificationChange();

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The observer fires for every chat domain, most of them trade chat, so a burst is the normal case
     * and not the exception: at startup the inventory sync delivers one notification per message. Each
     * rebuild scans the whole notification set once per channel, so queuing one per notification makes a
     * burst cost that scan every time, and a subscription arriving mid-burst waits behind all of them
     * until {@code getJsonPayload}'s timeout declines it.
     */
    @Test
    void aBurstOfNotificationsQueuesOneRebuild() {
        QueueingExecutorService executor = queueingService();

        fireNotificationChange();
        fireNotificationChange();
        fireNotificationChange();

        assertThat(executor.queued()).isEqualTo(1);
    }

    /**
     * A notification landing while the rebuild is in flight has to be able to queue its own. That is why
     * the rebuild clears its pending flag before taking the snapshot, not after: clearing it at the end
     * would fold this change into a run that had already read the state, and it would never be sent.
     */
    @Test
    void aChangeArrivingWhileARebuildRunsQueuesItsOwn() {
        QueueingExecutorService executor = queueingService();
        fireOnceWhileBuildingTheSnapshot();
        fireNotificationChange();

        executor.drain();

        assertThat(executor.queued()).isEqualTo(1);
    }

    /**
     * A submit that loses the race with shutdown never runs the task that would clear the pending flag,
     * so the caller has to clear it instead. Leaving it set would silence every later change.
     */
    @Test
    void aRejectedRebuildDoesNotSuppressTheNextOne() {
        QueueingExecutorService executor = queueingService();
        executor.rejectNext();

        fireNotificationChange();
        fireNotificationChange();

        assertThat(executor.queued()).isEqualTo(1);
    }

    /**
     * A service on an executor that only queues, already drained of the rebuild that {@code initialize}
     * queues: {@code Observable.addObserver} calls the observer at registration.
     */
    private QueueingExecutorService queueingService() {
        // The service set up for the other tests still observes, and its executor runs inline, so
        // leaving it bound would have it answer the change before the queued rebuild ever runs.
        service.shutdown().join();
        QueueingExecutorService executor = new QueueingExecutorService();
        service = new PublicChatChannelsWebSocketService(subscriberRepository, channels,
                chatNotificationService, executor);
        service.initialize().join();
        executor.drain();
        return executor;
    }

    /** Fires one notification change from inside the next snapshot, once. */
    private void fireOnceWhileBuildingTheSnapshot() {
        AtomicBoolean fired = new AtomicBoolean();
        when(chatNotificationService.getNumNotifications(channel)).thenAnswer(invocation -> {
            if (fired.compareAndSet(false, true)) {
                fireNotificationChange();
            }
            return 0L;
        });
    }

    private void unreadCount(long value) {
        unreadCount(channel, value);
        unreadCount(supportChannel, value);
    }

    private void unreadCount(CommonPublicChatChannel channel, long value) {
        when(chatNotificationService.getNumNotifications(channel)).thenReturn(value);
    }

    private void fireNotificationChange() {
        changedNotification.set(mock(ChatNotification.class));
    }
}
