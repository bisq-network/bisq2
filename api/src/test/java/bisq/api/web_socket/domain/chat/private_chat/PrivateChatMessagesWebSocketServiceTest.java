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

import bisq.api.web_socket.domain.chat.private_chat.PrivateChatTestMocks.ObservedSet;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatMessageType;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static bisq.api.web_socket.domain.chat.private_chat.PrivateChatTestMocks.mockReaction;
import static bisq.api.web_socket.domain.chat.private_chat.PrivateChatTestMocks.mockUserProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the two things this service does that its trade-chat sibling does not. Dropping messages whose
 * sender has been banned: Bisq 2 already rejects banned senders inbound, so the filter only covers a
 * peer banned <i>after</i> their messages arrived — a state no other test in this package produces. And
 * the per-channel ownership of the message observer, which is what leaving, re-creating and shutting
 * down have to respect.
 */
class PrivateChatMessagesWebSocketServiceTest {
    private static final String CHANNEL_ID = "discussion.a-b";

    private ObservableSet<TwoPartyPrivateChatChannel> channels;
    private final AtomicReference<CollectionObserver<TwoPartyPrivateChatChannel>> channelsObserver =
            new AtomicReference<>();
    /** Run once by the next scan of the channel collection, the last thing install reads before its put. */
    private final AtomicReference<Runnable> onNextChannelScan = new AtomicReference<>();
    private ObservableSet<TwoPartyPrivateChatMessage> messages;
    private TwoPartyPrivateChatChannel channel;
    private BannedUserService bannedUserService;
    private Subscriber subscriber;
    private PrivateChatMessagesWebSocketService service;

    @BeforeEach
    void setUp() {
        messages = new ObservableSet<>();
        channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        when(channel.getChatMessages()).thenReturn(messages);
        // Keeps hold of the service's channel observer so a test can invoke it after the pin that
        // registered it was unbound, which is what a notification already iterating the observer
        // snapshot does.
        channels = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatChannel> observer) {
                channelsObserver.set(observer);
                return super.addObserver(observer);
            }

            @Override
            public Stream<TwoPartyPrivateChatChannel> stream() {
                Runnable hook = onNextChannelScan.getAndSet(null);
                if (hook == null) {
                    return super.stream();
                }
                // Scanned first, torn down second: the caller gets the answer it would have read before
                // the teardown, and then writes on the strength of it.
                List<TwoPartyPrivateChatChannel> scanned = super.stream().toList();
                hook.run();
                return scanned.stream();
            }
        };
        channels.add(channel);

        TwoPartyPrivateChatChannelService channelService =
                mock(TwoPartyPrivateChatChannelService.class, RETURNS_DEEP_STUBS);
        when(channelService.getChannels()).thenReturn(channels);

        UserProfileService userProfileService = mock(UserProfileService.class, RETURNS_DEEP_STUBS);
        bannedUserService = mock(BannedUserService.class);

        subscriber = mock(Subscriber.class);
        when(subscriber.getTopic()).thenReturn(Topic.PRIVATE_CHAT_MESSAGES);
        when(subscriber.getSubscriberId()).thenReturn("subscriber-1");
        AtomicInteger sequenceNumber = new AtomicInteger();
        when(subscriber.incrementAndGetSequenceNumber()).thenAnswer(i -> sequenceNumber.incrementAndGet());

        SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
        when(subscriberRepository.findSubscribers(Topic.PRIVATE_CHAT_MESSAGES)).thenReturn(
                Map.of(new SubscriptionSpecifier(Topic.PRIVATE_CHAT_MESSAGES, Optional.empty()), Set.of(subscriber)));

        service = new PrivateChatMessagesWebSocketService(subscriberRepository, channelService,
                userProfileService, bannedUserService);
        service.initialize().join();
    }

    @Test
    void aMessageFromABannedSenderIsNotPushed() {
        messages.add(messageFrom(banned(true)));

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aMessageFromAnUnbannedSenderIsPushed() {
        messages.add(messageFrom(banned(false)));

        verify(subscriber).send(anyString());
    }

    /**
     * The reactions a message carries are filtered like the reactions topic's snapshot, so a client
     * reading either topic sees the same set: no removal markers, none from a sender banned after the
     * fact. The message itself is mine, so it is delivered; only its embedded reactions are pruned.
     */
    @Test
    void theEmbeddedReactionsAreFilteredLikeTheReactionsTopic() {
        ObservableSet<TwoPartyPrivateChatMessageReaction> reactions = new ObservableSet<>(Set.of(
                mockReaction("reaction-visible", banned(false), CHANNEL_ID, "message-1", false),
                mockReaction("reaction-banned", banned(true), CHANNEL_ID, "message-1", false),
                mockReaction("reaction-removed", banned(false), CHANNEL_ID, "message-1", true)));

        messages.add(messageFrom(banned(false), reactions));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(subscriber).send(json.capture());
        assertThat(json.getValue())
                .contains("reaction-visible")
                .doesNotContain("reaction-banned")
                .doesNotContain("reaction-removed");
    }

    @Test
    void theSubscribeSnapshotDropsBannedSendersToo() {
        messages.add(messageFrom(banned(true)));

        Optional<String> payload = service.getJsonPayload();

        assertThat(payload).isPresent();
        // The payload is a list, so an empty one is the only shape that says "nothing survived".
        assertThat(payload.orElseThrow()).isEqualTo("[]");
    }

    /**
     * {@code onCleared} is reachable: {@code ChannelStore#applyPersisted} replaces the set through
     * {@code setAll}, whose observer contract is {@code onCleared()} then {@code onAllAdded(values)}.
     * A channel dropped that way must stop reporting, or its observer outlives it and pushes messages
     * for a conversation the node no longer has.
     */
    @Test
    void clearingTheChannelCollectionUnbindsItsMessageObservers() {
        channels.clear();

        messages.add(messageFrom(banned(false)));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The ABA at the teardown end: leaving a channel and an inbound message from the same peer produce
     * two channel instances under one deterministic id, because {@code ObservableCollection#remove}
     * drops the element before it notifies — so the message finds nothing, creates a second channel and
     * binds it while the removal callback is still pending. A teardown keyed on the id alone then unbinds
     * the observer of the channel that is live, and its messages stop reaching the client for good.
     * <p>
     * Single-threaded here because the mocks make it so: Mockito leaves {@code equals} as reference
     * identity, so both instances sit in the set at once and the leave can be issued after the newer one
     * is already bound.
     */
    @Test
    void leavingAChannelLeavesANewerBindingUnderTheSameIdAlone() {
        ObservableSet<TwoPartyPrivateChatMessage> newerMessages = new ObservableSet<>();
        TwoPartyPrivateChatChannel newer = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(newer.getId()).thenReturn(CHANNEL_ID);
        when(newer.getChatMessages()).thenReturn(newerMessages);
        channels.add(newer);

        channels.remove(channel);

        newerMessages.add(messageFrom(banned(false)));

        verify(subscriber).send(anyString());
    }

    /**
     * The same ABA at the bind end. Observer callbacks are synchronous per operation but nothing
     * serializes them across threads, so the callback binding a channel can be resumed after that channel
     * was left and its same-id successor was already bound. A bind that publishes unconditionally then
     * displaces the successor's binding and unbinds it, notices its own channel is gone, and discards its
     * own — and the live channel is left observed by nobody.
     * <p>
     * Forced on one thread by having the older channel's message set perform the leave and the successor's
     * arrival inside {@code addObserver}: after the older bind has registered its observer, before it has
     * published anything.
     */
    @Test
    void aChannelBoundAfterItsSuccessorLeavesTheSuccessorBound() {
        channels.remove(channel);

        ObservableSet<TwoPartyPrivateChatMessage> newerMessages = new ObservableSet<>();
        TwoPartyPrivateChatChannel newer = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(newer.getId()).thenReturn(CHANNEL_ID);
        when(newer.getChatMessages()).thenReturn(newerMessages);

        AtomicReference<TwoPartyPrivateChatChannel> older = new AtomicReference<>();
        ObservableSet<TwoPartyPrivateChatMessage> staleMessages = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                Pin pin = super.addObserver(observer);
                channels.remove(older.get());
                channels.add(newer);
                return pin;
            }
        };
        TwoPartyPrivateChatChannel stale = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(stale.getId()).thenReturn(CHANNEL_ID);
        when(stale.getChatMessages()).thenReturn(staleMessages);
        older.set(stale);

        channels.add(stale);

        newerMessages.add(messageFrom(banned(false)));

        verify(subscriber).send(anyString());
    }

    /**
     * The channel is left while it is still being bound, so its message pin is created after the teardown
     * has already run and has to be discarded rather than stored. The reactions sibling has the same
     * guard; it is repeated here because the two services publish through separate maps.
     */
    @Test
    void aChannelLeftWhileItIsBeingBoundBindsNothing() {
        AtomicReference<TwoPartyPrivateChatChannel> leftChannel = new AtomicReference<>();
        ObservableSet<TwoPartyPrivateChatMessage> lateMessages = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                Pin pin = super.addObserver(observer);
                channels.remove(leftChannel.get());
                return pin;
            }
        };
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);
        leftChannel.set(other);

        channels.add(other);

        lateMessages.add(messageFrom(banned(false)));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The channel is bound while the service shuts down. Unlike a leave, a shutdown does not touch the
     * channel collection, so the bind still finds its owner live and publishes — after the teardown has
     * already swept the map. A sweep that only unbinds what it saw then leaves that observer registered
     * with nothing holding a reference to it, still pushing for a service that is gone.
     */
    @Test
    void aChannelBoundWhileTheServiceShutsDownBindsNothing() {
        ObservableSet<TwoPartyPrivateChatMessage> lateMessages = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                Pin pin = super.addObserver(observer);
                service.shutdown().join();
                return pin;
            }
        };
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        channels.add(other);

        lateMessages.add(messageFrom(banned(false)));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The inverse of the test above: the bind starts only after the shutdown has finished. Observers
     * are notified from a copy-on-write snapshot, so an add that began before the pin was unbound
     * still reaches this service afterwards, with the channel live and every teardown already done.
     */
    @Test
    void aChannelBoundAfterTheServiceShutDownBindsNothing() {
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        service.shutdown().join();
        channels.add(other); // so that the bind finds its owner live; the real observer is unbound by now
        channelsObserver.get().onAdded(other);

        // The leak is a registered observer, not a push: the emission guard alone would keep the push
        // from showing while the pin stays behind.
        assertThat(lateMessages.hasObservers()).isFalse();
        lateMessages.add(messageFrom(banned(false)));
        verify(subscriber, never()).send(anyString());
    }

    /**
     * Same as above, but the late channel already holds a message, which registering the observer
     * replays. Refusing at publish time alone would push it before the refusal.
     */
    @Test
    void aChannelBoundAfterTheServiceShutDownReplaysNothing() {
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        lateMessages.add(messageFrom(banned(false)));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        service.shutdown().join();
        channels.add(other); // so that the bind finds its owner live; the real observer is unbound by now
        channelsObserver.get().onAdded(other);

        assertThat(lateMessages.hasObservers()).isFalse();
        verify(subscriber, never()).send(anyString());
    }

    /**
     * The bind starts between the generation bump and the sweep, from inside the sweep itself: it
     * captures the bumped generation, so its compare after publishing would read unchanged, and only the
     * flag can refuse it. Pins that the flag is set before the sweep; that it precedes the bump too is
     * argued at the field, there being no seam between the bump and the key snapshot to hook. The sweep
     * reaches this bind through a pin it unbinds.
     */
    @Test
    void aChannelBoundWhileTheSweepIsRunningBindsNothing() {
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        AtomicBoolean fired = new AtomicBoolean();
        ObservableSet<TwoPartyPrivateChatMessage> sweptMessages = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                Pin pin = super.addObserver(observer);
                return () -> {
                    pin.unbind();
                    if (fired.compareAndSet(false, true)) {
                        channels.add(other);
                        channelsObserver.get().onAdded(other);
                    }
                };
            }
        };
        TwoPartyPrivateChatChannel swept = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(swept.getId()).thenReturn("discussion.a-d");
        when(swept.getChatMessages()).thenReturn(sweptMessages);
        channels.add(swept);

        service.shutdown().join();

        assertThat(fired).as("the sweep reached the late bind").isTrue();
        assertThat(lateMessages.hasObservers()).isFalse();
    }

    /**
     * The shutdown lands while the bind is registering, after its early check and before the replay.
     * Nothing at the bind site can stop that replay, so the emission path has to.
     */
    @Test
    void aReplayStartedBeforeTheShutdownPushesNothingAfterIt() {
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                service.shutdown().join();
                return super.addObserver(observer);
            }
        };
        lateMessages.add(messageFrom(banned(false)));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        channels.add(other);

        assertThat(lateMessages.hasObservers()).isFalse();
        verify(subscriber, never()).send(anyString());
    }

    /**
     * The shutdown lands inside the publishing compute, after the generation was read and before the binding
     * is written. The sweep then runs over a key snapshot that does not hold this channel, so only the
     * bind itself can notice that it published into a service that is gone.
     * <p>
     * Relies on install scanning the collection last, which is where the hook fires: this bind must publish
     * rather than be refused, or the re-check after publishing goes untested. The helper pins that.
     */
    @Test
    void aChannelWhoseBindPublishesAfterTheSweepUnbindsItself() {
        assertThatABindPublishingDuringATeardownUnbindsItself(() -> service.shutdown().join());
    }

    /**
     * The clear counterpart: {@code setAll} on the channel collection, which is how the persisted store
     * is applied, clears it and notifies {@code onCleared}. The owner check cannot see it either, since
     * it reads the collection outside the lock the publishing compute holds.
     */
    @Test
    void aChannelWhoseBindPublishesAfterAClearUnbindsItself() {
        assertThatABindPublishingDuringATeardownUnbindsItself(() -> channels.setAll(Set.of()));
    }

    private void assertThatABindPublishingDuringATeardownUnbindsItself(Runnable teardown) {
        // Leaves the map empty before the hook fires: the teardown runs while install holds this key's
        // bin lock, and its sweep must not find another key to remove through the same lock.
        channels.remove(channel);
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);
        onNextChannelScan.set(teardown);

        channels.add(other);

        assertThat(lateMessages.hasObservers()).isFalse();
        lateMessages.add(messageFrom(banned(false)));
        verify(subscriber, never()).send(anyString());
        // Once for the key install published under, once for the key the self-unbind took out. A single
        // call means install refused instead, and the re-check after publishing went untested.
        verify(other, times(2).description("install published the binding and the self-unbind took it out again"))
                .getId();
    }

    private UserProfile banned(boolean isBanned) {
        UserProfile sender = mockUserProfile();
        when(bannedUserService.isUserProfileBanned(sender)).thenReturn(isBanned);
        return sender;
    }

    private static TwoPartyPrivateChatMessage messageFrom(UserProfile sender) {
        return messageFrom(sender, new ObservableSet<>());
    }

    private static TwoPartyPrivateChatMessage messageFrom(UserProfile sender,
                                                          ObservableSet<TwoPartyPrivateChatMessageReaction> reactions) {
        TwoPartyPrivateChatMessage message = mock(TwoPartyPrivateChatMessage.class, RETURNS_DEEP_STUBS);
        when(message.getSenderUserProfile()).thenReturn(sender);
        when(message.getCitation()).thenReturn(Optional.empty());
        when(message.getText()).thenReturn(Optional.of("hi"));
        when(message.getChatMessageReactions()).thenReturn(reactions);
        // Everything a deep stub cannot supply: a null enum and a null key both throw inside the
        // mapping, which the service catches and logs — so a missing stub here would look exactly like
        // the filter dropping the message. See mockUserProfile for the same problem on the profile side.
        when(message.getChatMessageType()).thenReturn(ChatMessageType.TEXT);
        when(message.getReceiverNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return message;
    }
}
