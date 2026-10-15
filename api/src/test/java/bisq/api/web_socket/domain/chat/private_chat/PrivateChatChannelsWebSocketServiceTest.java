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

import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The notification observer fires for every chat domain, so this service only pushes when the DM
 * channel list actually changed. These tests pin the two ways that "actually changed" can go wrong,
 * plus the lock inversion that serialising the push paths with a lock would introduce.
 */
class PrivateChatChannelsWebSocketServiceTest {
    private static final String CHANNEL_ID = "discussion.a-b";

    private ObservableSet<TwoPartyPrivateChatChannel> channels;
    private Observable<ChatNotification> changedNotification;
    private ChatNotificationService chatNotificationService;
    private UserProfileService userProfileService;
    private SubscriberRepository subscriberRepository;
    private TwoPartyPrivateChatChannelService channelService;
    private Subscriber subscriber;
    private TwoPartyPrivateChatChannel channel;
    private UserProfile peer;
    private PrivateChatChannelsWebSocketService service;

    @BeforeEach
    void setUp() {
        channels = new ObservableSet<>();
        changedNotification = new Observable<>();

        channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        // Explicit: a deep stub would hand back null here and the enum mapping would throw, which the
        // service logs and swallows — the test would then silently compare two empty maps.
        when(channel.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        // The dto carries my own profile as well as the peer's, and this one comes off the channel.
        // Built into a local first: mockUserProfile stubs internally, and Mockito rejects stubbing
        // nested inside another thenReturn(...) argument.
        UserProfile myProfile = mockUserProfile("me");
        when(channel.getMyUserIdentity().getUserProfile()).thenReturn(myProfile);
        channels.add(channel);

        peer = mockUserProfile("peer");

        channelService = mock(TwoPartyPrivateChatChannelService.class, RETURNS_DEEP_STUBS);
        when(channelService.getChannels()).thenReturn(channels);

        chatNotificationService = mock(ChatNotificationService.class, RETURNS_DEEP_STUBS);
        when(chatNotificationService.getChangedNotification()).thenReturn(changedNotification);
        unreadCount(0);

        userProfileService = mock(UserProfileService.class, RETURNS_DEEP_STUBS);
        when(userProfileService.getManagedUserProfile(any())).thenReturn(peer);

        subscriber = mockSubscriber("subscriber-1");

        subscriberRepository = mock(SubscriberRepository.class);
        subscribed(false);

        service = new PrivateChatChannelsWebSocketService(subscriberRepository, channelService,
                chatNotificationService, userProfileService, new SameThreadExecutorService());
        service.initialize().join();
    }

    /**
     * Reproduces, with two real threads, the lock inversion described in the threading note on
     * {@link PrivateChatChannelsWebSocketService} — a notifier arriving while holding the store monitor
     * against a subscribe that needs it.
     * <p>
     * Uses a real single-threaded executor rather than the same-thread one, since the handoff is
     * exactly what is under test. Deterministic: latches, not sleeps, and both worker threads are
     * joined with a timeout so a regression fails the test instead of hanging the build.
     */
    @Test
    void aSubscriptionServedWhileANotificationFiresDoesNotDeadlock() throws Exception {
        subscribed(true);
        ExecutorService pushExecutor = Executors.newSingleThreadExecutor();
        try {
            // Drained before the blocking stub goes in, or the latch below is tripped by the wrong thread.
            PrivateChatChannelsWebSocketService serviceUnderTest = serviceOn(pushExecutor);

            Object storeMonitor = new Object();
            CountDownLatch pushThreadIsBuildingDtos = new CountDownLatch(1);
            CountDownLatch notifierHoldsTheStore = new CountDownLatch(1);
            when(chatNotificationService.getNumNotifications(any(ChatChannel.class))).thenAnswer(invocation -> {
                pushThreadIsBuildingDtos.countDown();
                synchronized (storeMonitor) {
                    return 1L;
                }
            });

            Thread notifier = new Thread(() -> {
                synchronized (storeMonitor) {
                    notifierHoldsTheStore.countDown();
                    await(pushThreadIsBuildingDtos);
                    // Publishing from inside the store monitor is what ChatNotificationService does.
                    fireNotificationChange();
                }
            }, "notifier");
            notifier.setDaemon(true);

            AtomicBoolean payloadServed = new AtomicBoolean();
            Thread subscriber = new Thread(() -> {
                await(notifierHoldsTheStore);
                serviceUnderTest.getJsonPayload();
                payloadServed.set(true);
            }, "subscriber");
            subscriber.setDaemon(true);

            notifier.start();
            subscriber.start();
            notifier.join(10_000);
            subscriber.join(10_000);

            assertThat(payloadServed).isTrue();
            assertThat(notifier.isAlive()).isFalse();
        } finally {
            pushExecutor.shutdownNow();
        }
    }

    /**
     * Pins the coalescing in {@code resendChannelsIfChanged}, which the notification observable makes
     * necessary by firing for every chat domain.
     * <p>
     * Counted through {@code getNumNotifications}: one channel means one call per rebuild. The push
     * thread is held on a latch so the burst really queues, which the same-thread executor the other
     * tests use could never show.
     */
    @Test
    void aBurstOfNotificationsCollapsesIntoOneRebuild() throws Exception {
        subscribed(true);
        ExecutorService pushExecutor = Executors.newSingleThreadExecutor();
        try {
            serviceOn(pushExecutor);
            clearInvocations(chatNotificationService);

            CountDownLatch releasePushThread = new CountDownLatch(1);
            pushExecutor.execute(() -> await(releasePushThread));
            for (int i = 0; i < 5; i++) {
                fireNotificationChange();
            }
            releasePushThread.countDown();
            drain(pushExecutor);

            verify(chatNotificationService, times(1)).getNumNotifications(any(ChatChannel.class));
        } finally {
            pushExecutor.shutdownNow();
        }
    }

    @Test
    void aChangedUnreadCountIsPushedToSubscribers() {
        subscribed(true);
        unreadCount(1);

        fireNotificationChange();

        verify(subscriber).send(anyString());
    }

    @Test
    void anUnchangedUnreadCountIsNotPushed() {
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();
        clearInvocations(subscriber);

        fireNotificationChange();

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The regression this class exists for. Skipping the bookkeeping while nobody is subscribed left
     * the service believing a stale count, so the next real change compared equal and was swallowed —
     * and the channel's badge stayed wrong for good. Background → foreground on mobile is exactly
     * this sequence.
     */
    @Test
    void aChangeThatHappenedWhileUnsubscribedDoesNotSwallowTheNextPush() {
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();
        clearInvocations(subscriber);

        // Client goes away; the user reads the conversation on desktop.
        subscribed(false);
        unreadCount(0);
        fireNotificationChange();

        // Client comes back and is served the current state, then a new DM arrives.
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();

        verify(subscriber).send(anyString());
    }

    /**
     * The multi-subscriber regression. {@code lastKnownChannels} is one map for all subscribers, so a
     * subscribe that snapshots a state newer than the one the others hold must push it to them before
     * recording it — otherwise the map claims a state only the new subscriber was served, and the
     * notification's own rebuild, arriving second, finds nothing changed and stays quiet.
     * <p>
     * The whole suite otherwise runs on a single subscriber, where the invariant holds and the bug is
     * invisible. Two subscribers is the realistic case: the API pairs several mobile devices.
     * <p>
     * Both are in the repository throughout, which is exactly the state {@code getJsonPayload} runs in —
     * {@code SubscriberRepository.add} has already returned by then, so the subscribing client is
     * registered while its snapshot is being built.
     */
    @Test
    void aSubscribeOnANewerStateStillPushesItToTheAlreadySubscribed() {
        Subscriber other = mockSubscriber("subscriber-2");
        subscribed(Set.of(subscriber, other));
        unreadCount(0);
        fireNotificationChange();
        clearInvocations(subscriber, other);

        // A DM lands, and the subscribe task reaches the push thread ahead of the notification's.
        unreadCount(1);
        assertThat(service.getJsonPayload()).isPresent();
        // The rebuild the subscribe raced. It now has nothing left to do, which is the point: the push
        // above is the only one either subscriber gets.
        fireNotificationChange();

        verify(subscriber).send(anyString());
        verify(other).send(anyString());
    }

    /**
     * The dto carries the re-resolved peer profile, so comparing whole dtos rather than just unread
     * counts is what lets a changed profile be detected at all. Not a rename: a Bisq 2 nickname is
     * immutable — {@code UserIdentityService.editUserProfile} only takes terms and statement, and
     * {@code UserProfile.forEdit} keeps the nickname. What moves is what
     * {@code getManagedUserProfile} returns: the republished profile's editable fields, or the
     * channel's embedded copy once the network-store entry is pruned.
     * Note what this does <i>not</i> claim: the comparison only runs because this test fires the
     * notification observable, and a profile update does not fire it — so the change reaches the
     * client on the back of the next unrelated chat event, not on its own.
     */
    @Test
    void aChangedPeerProfileIsDetectedWhenTheComparisonRunsEvenThoughTheUnreadCountIsUnchanged() {
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();
        clearInvocations(subscriber);

        UserProfile changed = mockUserProfile("peer-updated");
        when(userProfileService.getManagedUserProfile(any())).thenReturn(changed);

        fireNotificationChange();

        verify(subscriber).send(anyString());
    }

    /**
     * Pins <i>what</i> the subscribe snapshot records, not just that it records something. If
     * {@code getJsonPayload} stored an empty map — or anything other than the state it served — the
     * unchanged fire below would push. {@link #aSubscribeSnapshotIsRecordedSoALaterRevertIsStillPushed}
     * would still pass in that case, so both tests are needed.
     */
    @Test
    void theSubscribeSnapshotRecordsTheStateItServed() {
        subscribed(true);
        unreadCount(2);
        fireNotificationChange();

        assertThat(service.getJsonPayload()).isPresent();

        clearInvocations(subscriber);
        fireNotificationChange();

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The channel path is serialised against the comparison, and records what it sent. Without the
     * bookkeeping, leaving a channel leaves the map holding it, so the next unrelated notification
     * broadcasts the whole list purely to say what the REMOVED already said.
     * <p>
     * The interleaving this guards against — a REMOVED landing between the snapshot and the send —
     * needs two threads to demonstrate and is not covered here; this pins the bookkeeping half.
     */
    @Test
    void leavingAChannelDoesNotLeaveTheMapClaimingIt() {
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();

        channels.remove(channel);
        clearInvocations(subscriber);

        fireNotificationChange();

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The subscribe snapshot has to be recorded, not just served. The peer profile moves the dto with
     * no notification event behind it, and {@code getManagedUserProfile} is
     * {@code findUserProfile(id).orElse(userProfile)} — so the value falls back to the channel's
     * embedded copy when the profile is pruned, and the dto reverts on its own. A map still holding
     * the pre-subscribe state would then compare equal to the reverted state and swallow the push,
     * leaving the subscriber on the snapshot it was served.
     */
    @Test
    void aSubscribeSnapshotIsRecordedSoALaterRevertIsStillPushed() {
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();

        UserProfile changed = mockUserProfile("peer-updated");
        when(userProfileService.getManagedUserProfile(any())).thenReturn(changed);
        assertThat(service.getJsonPayload()).isPresent();

        when(userProfileService.getManagedUserProfile(any())).thenReturn(peer);
        clearInvocations(subscriber);
        fireNotificationChange();

        verify(subscriber).send(anyString());
    }

    /**
     * The per-channel path in isolation. {@link #leavingAChannelDoesNotLeaveTheMapClaimingIt} exercises
     * a removal but clears the invocations right after it, so without this test nothing would notice if
     * {@code send(channel, ...)} stopped emitting altogether.
     * <p>
     * Asserted on the event envelope rather than on {@code send} alone, because ADDED and REMOVED reach
     * the subscriber through the same method and only the payload tells them apart.
     */
    @Test
    void openingAndLeavingAChannelEmitAnAddedAndARemovedEvent() {
        subscribed(true);

        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        UserProfile myProfile = mockUserProfile("me");
        when(other.getMyUserIdentity().getUserProfile()).thenReturn(myProfile);

        channels.add(other);
        channels.remove(other);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(subscriber, times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(0)).contains(ModificationType.ADDED.name());
        assertThat(captor.getAllValues().get(1)).contains(ModificationType.REMOVED.name());
    }

    /**
     * A bulk clear reaches the service through {@code setAll}, whose observer contract is
     * {@code onCleared()} then {@code onAllAdded(values)}. It has to be reported as REMOVED, because the
     * only other push is a full list sent as ADDED and an upsert cannot express a deletion.
     */
    @Test
    void clearingTheChannelCollectionIsReportedAsRemoved() {
        subscribed(true);
        unreadCount(1);
        fireNotificationChange();
        clearInvocations(subscriber);

        channels.clear();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(subscriber).send(captor.capture());
        assertThat(captor.getValue()).contains(ModificationType.REMOVED.name());
        assertThat(captor.getValue()).contains(CHANNEL_ID);
    }

    /**
     * The rejection path of {@code submitPush}. {@code resendPending} is claimed before the task is
     * queued, so if the queueing fails the task that would release it never runs — and a flag stuck at
     * true silently suppresses every later resend for the life of the process.
     */
    @Test
    void aRejectedPushDoesNotWedgeTheCoalescingFlag() {
        subscribed(true);
        AtomicInteger attempts = new AtomicInteger();
        // Not serviceOn(...): that drains the executor, which a rejecting one cannot do.
        service.shutdown().join();
        ExecutorService rejecting = new SameThreadExecutorService() {
            @Override
            public void execute(Runnable command) {
                attempts.incrementAndGet();
                throw new RejectedExecutionException("rejected");
            }
        };
        PrivateChatChannelsWebSocketService serviceUnderTest = new PrivateChatChannelsWebSocketService(
                subscriberRepository, channelService, chatNotificationService, userProfileService, rejecting);
        serviceUnderTest.initialize().join();
        attempts.set(0);

        fireNotificationChange();
        fireNotificationChange();

        assertThat(attempts)
                .as("the second notification must still reach the executor; one attempt means the flag stayed set")
                .hasValue(2);
    }

    /**
     * A service on a real push thread, replacing the same-thread one from {@code setUp} — that one
     * observes the same mocks, so it has to be unbound or its inline pushes land on the shared
     * invocation counts. Returns with the push that {@code initialize} queues already drained.
     */
    private PrivateChatChannelsWebSocketService serviceOn(ExecutorService pushExecutor) throws Exception {
        service.shutdown().join();
        PrivateChatChannelsWebSocketService serviceUnderTest = new PrivateChatChannelsWebSocketService(
                subscriberRepository, channelService, chatNotificationService, userProfileService, pushExecutor);
        serviceUnderTest.initialize().join();
        drain(pushExecutor);
        return serviceUnderTest;
    }

    /** Waits for everything already queued on the push thread to finish. */
    private static void drain(ExecutorService pushExecutor) throws Exception {
        pushExecutor.submit(() -> {
        }).get(5, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other thread");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * A deep stub is not enough on its own: {@code DtoMappings.UserProfileMapping} runs the profile
     * through Base64 and a digest, and a stubbed {@code byte[]} getter hands back null. The resulting
     * NPE is caught and logged by the service, so without these the test would compare two empty
     * payloads and pass while asserting nothing.
     */
    private static UserProfile mockUserProfile(String nickName) {
        UserProfile profile = mock(UserProfile.class, RETURNS_DEEP_STUBS);
        when(profile.getNickName()).thenReturn(nickName);
        when(profile.getProofOfWork().getPayload()).thenReturn(new byte[0]);
        when(profile.getProofOfWork().getSolution()).thenReturn(new byte[0]);
        when(profile.getNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return profile;
    }

    private static Subscriber mockSubscriber(String subscriberId) {
        Subscriber subscriber = mock(Subscriber.class);
        when(subscriber.getTopic()).thenReturn(Topic.PRIVATE_CHAT_CHANNELS);
        when(subscriber.getSubscriberId()).thenReturn(subscriberId);
        AtomicInteger sequenceNumber = new AtomicInteger();
        when(subscriber.incrementAndGetSequenceNumber()).thenAnswer(i -> sequenceNumber.incrementAndGet());
        return subscriber;
    }

    private void subscribed(boolean subscribed) {
        subscribed(subscribed ? Set.of(subscriber) : Set.of());
    }

    private void subscribed(Set<Subscriber> subscribers) {
        Map<SubscriptionSpecifier, Set<Subscriber>> result = subscribers.isEmpty()
                ? Collections.emptyMap()
                : Map.of(new SubscriptionSpecifier(Topic.PRIVATE_CHAT_CHANNELS, Optional.empty()), subscribers);
        when(subscriberRepository.findSubscribers(Topic.PRIVATE_CHAT_CHANNELS)).thenReturn(result);
    }

    private void unreadCount(long value) {
        when(chatNotificationService.getNumNotifications(any(ChatChannel.class))).thenReturn(value);
    }

    /**
     * Emits a distinct value every time. {@code Observable.set} is a no-op when the new value equals
     * the current one, so repeatedly setting null — which is one of the things Bisq 2 does on this
     * observable — would only notify the first time. In production the value alternates between real
     * notifications and null, so every call is an event; a fresh mock reproduces that without
     * depending on which of the two the caller happened to publish.
     */
    private void fireNotificationChange() {
        changedNotification.set(mock(ChatNotification.class));
    }

    /**
     * Runs the service's push executor on the calling thread, which keeps most of these tests
     * deterministic — they assert on ordering and bookkeeping, not on the handoff itself.
     */
    private static class SameThreadExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }
    }
}
