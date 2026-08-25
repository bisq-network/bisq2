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
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatChannelDomain;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
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
 * Covers the two things this service does that its trade-chat sibling
 * ({@code ChatReactionsWebSocketService}) cannot be relied on to cover: the per-channel ownership of
 * the reaction pins, which is the whole reason this class is not a copy, and the two-event shape an
 * un-reaction takes on the wire.
 */
class PrivateChatReactionsWebSocketServiceTest {
    private static final String CHANNEL_ID = "discussion.a-b";
    private static final String MESSAGE_ID = "message-1";

    private ObservableSet<TwoPartyPrivateChatChannel> channels;
    private final AtomicReference<CollectionObserver<TwoPartyPrivateChatChannel>> channelsObserver =
            new AtomicReference<>();
    /** Run once by the next scan of the channel collection, the last thing install reads before its put. */
    private final AtomicReference<Runnable> onNextChannelScan = new AtomicReference<>();
    private ObservableSet<TwoPartyPrivateChatMessage> messages;
    private ObservableSet<TwoPartyPrivateChatMessageReaction> reactions;
    private TwoPartyPrivateChatChannel channel;
    private Subscriber subscriber;
    private BannedUserService bannedUserService;
    private PrivateChatReactionsWebSocketService service;

    @BeforeEach
    void setUp() {
        reactions = new ObservableSet<>();
        messages = new ObservableSet<>();
        messages.add(mockMessage(MESSAGE_ID, reactions));

        channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        when(channel.getChatMessages()).thenReturn(messages);

        // The channel and its message exist before initialize(), because addObserver replays what is
        // already there — which is how the reaction observer of an existing message gets bound at all.
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

        subscriber = mock(Subscriber.class);
        when(subscriber.getTopic()).thenReturn(Topic.PRIVATE_CHAT_REACTIONS);
        when(subscriber.getSubscriberId()).thenReturn("subscriber-1");
        AtomicInteger sequenceNumber = new AtomicInteger();
        when(subscriber.incrementAndGetSequenceNumber()).thenAnswer(i -> sequenceNumber.incrementAndGet());

        SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
        when(subscriberRepository.findSubscribers(Topic.PRIVATE_CHAT_REACTIONS)).thenReturn(
                Map.of(new SubscriptionSpecifier(Topic.PRIVATE_CHAT_REACTIONS, Optional.empty()), Set.of(subscriber)));

        bannedUserService = mock(BannedUserService.class);

        service = new PrivateChatReactionsWebSocketService(subscriberRepository, channelService, bannedUserService);
        service.initialize().join();
    }

    @Test
    void aNewReactionIsPushedAsAdded() {
        reactions.add(reaction("reaction-1", false));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(subscriber).send(captor.capture());
        assertThat(captor.getValue()).contains(ModificationType.ADDED.name());
    }

    /**
     * The shape an un-reaction actually takes, which is not one event but two: {@code
     * PrivateChatMessage#addPrivateChatMessageReaction} drops the superseded reaction from the set and
     * adds a fresh one carrying {@code isRemoved = true}. So the client sees REMOVED for the reaction
     * that is going away and ADDED for the marker that says it went away — the marker is not itself
     * pushed as REMOVED, and a client that routes on the modification type alone would re-add it.
     */
    @Test
    void unReactingPushesTheSupersededReactionAsRemovedAndTheMarkerAsAdded() {
        TwoPartyPrivateChatMessageReaction original = reaction("reaction-1", false);
        reactions.add(original);

        reactions.remove(original);
        reactions.add(reaction("reaction-2", true));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(subscriber, times(3)).send(captor.capture());
        List<String> payloads = captor.getAllValues();
        assertThat(payloads.get(1)).contains(ModificationType.REMOVED.name());
        // The reaction list is a json string nested in the event envelope, hence the escaped quotes.
        assertThat(payloads.get(2)).contains(ModificationType.ADDED.name(), "\\\"isRemoved\\\":true");
    }

    /**
     * The subscribe snapshot has no modification type to carry the distinction, so a removal marker left
     * in it would come back as a live reaction on every fresh subscription.
     */
    @Test
    void theSubscribeSnapshotDropsRemovalMarkers() {
        reactions.add(reaction("reaction-1", true));

        Optional<String> payload = service.getJsonPayload();

        assertThat(payload).isPresent();
        // The payload is a list, so an empty one is the only shape that says "nothing survived".
        assertThat(payload.orElseThrow()).isEqualTo("[]");
    }

    /**
     * What holding the reaction pins per channel buys over the flat message-id map of the trade-chat
     * sibling. Leaving a DM is routine, so a channel's reaction observers have to go with it — otherwise
     * they outlive the channel and keep pushing for a conversation the node dropped.
     */
    @Test
    void leavingAChannelUnbindsTheReactionObserversOfItsMessages() {
        channels.remove(channel);

        reactions.add(reaction("reaction-1", false));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The bulk path to the same unbinding, and reachable for the same reason as in the sibling services:
     * {@code ChannelStore#applyPersisted} replaces the set through {@code setAll}, whose observer contract
     * is {@code onCleared()} then {@code onAllAdded(values)}. Covered separately from the single-channel
     * case because it reaches the pins through {@code unbindAllChannelPins} rather than through the
     * per-channel removal.
     */
    @Test
    void clearingTheChannelCollectionUnbindsTheReactionObserversToo() {
        channels.clear();

        reactions.add(reaction("reaction-1", false));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * A message arriving as the channel is being left. {@link bisq.common.observable.Pin#unbind} only
     * drops an observer from a copy-on-write list, so the callback already running when the leave lands
     * still finishes — and what it does next decides whether anything is left behind.
     * <p>
     * Forced deterministically on one thread by having the reaction set perform the leave inside
     * {@code addObserver}, which puts it exactly between creating the reaction observer and storing its
     * pin. That is the window in which a version keyed by channel id recreates the entry the teardown
     * has just removed, leaving an observer that nothing enumerates and that keeps pushing reactions for
     * a channel the node dropped.
     */
    @Test
    void aMessageArrivingWhileTheChannelIsLeftBindsNoReactionObserver() {
        ObservableSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessageReaction> observer) {
                Pin pin = super.addObserver(observer);
                channels.remove(channel);
                return pin;
            }
        };
        // Added after initialize(), so the callback runs on the path an inbound message takes rather
        // than inside the replay addObserver does while the channel is still being bound.
        messages.add(mockMessage("message-2", lateReactions));

        lateReactions.add(reaction("reaction-1", false));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The same window at the other end: the channel is left while it is still being bound, so its
     * message pin is created after the teardown has already run and has to be discarded rather than
     * stored. A guard rather than a reproducer — a version that binds inside a {@code compute} on the
     * channel key reenters that {@code compute} from within itself here, and what
     * {@code ConcurrentHashMap} does with that is not something to assert on.
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
        ObservableSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservableSet<>();
        lateMessages.add(mockMessage("message-2", lateReactions));

        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);
        leftChannel.set(other);

        channels.add(other);

        lateReactions.add(reaction("reaction-1", false));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The ABA at the teardown end: leaving a channel and an inbound message from the same peer produce
     * two channel instances under one deterministic id, because {@code ObservableCollection#remove}
     * drops the element before it notifies — so the message finds nothing, creates a second channel and
     * binds it while the removal callback is still pending. A teardown keyed on the id alone then closes
     * the observers of the channel that is live, and reactions on it stop reaching the client for good.
     * <p>
     * Single-threaded here because the mocks make it so: Mockito leaves {@code equals} as reference
     * identity, so both instances sit in the set at once and the leave can be issued after the newer one
     * is already bound.
     */
    @Test
    void leavingAChannelLeavesANewerBindingUnderTheSameIdAlone() {
        ObservableSet<TwoPartyPrivateChatMessageReaction> newerReactions = new ObservableSet<>();
        ObservableSet<TwoPartyPrivateChatMessage> newerMessages = new ObservableSet<>();
        newerMessages.add(mockMessage("message-2", newerReactions));

        TwoPartyPrivateChatChannel newer = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(newer.getId()).thenReturn(CHANNEL_ID);
        when(newer.getChatMessages()).thenReturn(newerMessages);
        channels.add(newer);

        channels.remove(channel);

        newerReactions.add(reaction("reaction-1", false));

        verify(subscriber).send(anyString());
    }

    /**
     * The same ABA at the bind end. Observer callbacks are synchronous per operation but nothing
     * serializes them across threads, so the callback binding a channel can be resumed after that channel
     * was left and its same-id successor was already bound. A bind that publishes unconditionally then
     * displaces the successor's pins and closes them, notices its own channel is gone, and discards its
     * own — and the live channel is left observed by nobody.
     * <p>
     * Forced on one thread by having the older channel's message set perform the leave and the successor's
     * arrival inside {@code addObserver}: after the older bind has registered its observer, before it has
     * published anything.
     */
    @Test
    void aChannelBoundAfterItsSuccessorLeavesTheSuccessorBound() {
        channels.remove(channel);

        ObservableSet<TwoPartyPrivateChatMessageReaction> newerReactions = new ObservableSet<>();
        ObservableSet<TwoPartyPrivateChatMessage> newerMessages = new ObservableSet<>();
        newerMessages.add(mockMessage("message-2", newerReactions));
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

        newerReactions.add(reaction("reaction-1", false));

        verify(subscriber).send(anyString());
    }

    /**
     * The channel is bound while the service shuts down. Unlike a leave, a shutdown does not touch the
     * channel collection, so the bind still finds its owner live and publishes — after the teardown has
     * already swept the map. Pins published after that sweep would otherwise survive it and keep pushing
     * for a service that is gone.
     */
    @Test
    void aChannelBoundWhileTheServiceShutsDownBindsNothing() {
        ObservableSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservableSet<>();
        ObservableSet<TwoPartyPrivateChatMessage> lateMessages = new ObservableSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                Pin pin = super.addObserver(observer);
                service.shutdown().join();
                return pin;
            }
        };
        lateMessages.add(mockMessage("message-2", lateReactions));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        channels.add(other);

        lateReactions.add(reaction("reaction-1", false));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The inverse of the test above: the bind starts only after the shutdown has finished. Observers
     * are notified from a copy-on-write snapshot, so an add that began before the pin was unbound
     * still reaches this service afterwards, with the channel live and every teardown already done.
     */
    @Test
    void aChannelBoundAfterTheServiceShutDownBindsNothing() {
        ObservedSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservedSet<>();
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        lateMessages.add(mockMessage("message-2", lateReactions));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        service.shutdown().join();
        channels.add(other); // so that the bind finds its owner live; the real observer is unbound by now
        channelsObserver.get().onAdded(other);

        // The leak is a registered observer, not a push: the emission guard alone would keep the push
        // from showing while the pins stay behind.
        assertThat(lateMessages.hasObservers()).isFalse();
        assertThat(lateReactions.hasObservers()).isFalse();
        lateReactions.add(reaction("reaction-1", false));
        verify(subscriber, never()).send(anyString());
    }

    /**
     * Same as above, but the late channel's message already holds a reaction, which registering the
     * observers replays. Refusing at publish time alone would push it before the refusal.
     */
    @Test
    void aChannelBoundAfterTheServiceShutDownReplaysNothing() {
        ObservedSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservedSet<>();
        lateReactions.add(reaction("reaction-1", false));
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        lateMessages.add(mockMessage("message-2", lateReactions));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        service.shutdown().join();
        channels.add(other); // so that the bind finds its owner live; the real observer is unbound by now
        channelsObserver.get().onAdded(other);

        assertThat(lateMessages.hasObservers()).isFalse();
        assertThat(lateReactions.hasObservers()).isFalse();
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
        lateMessages.add(mockMessage("message-2", new ObservableSet<>()));
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
        sweptMessages.add(mockMessage("message-3", new ObservableSet<>()));
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
        ObservedSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservedSet<>();
        lateReactions.add(reaction("reaction-1", false));
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>() {
            @Override
            public Pin addObserver(CollectionObserver<TwoPartyPrivateChatMessage> observer) {
                service.shutdown().join();
                return super.addObserver(observer);
            }
        };
        lateMessages.add(mockMessage("message-2", lateReactions));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);

        channels.add(other);

        assertThat(lateMessages.hasObservers()).isFalse();
        assertThat(lateReactions.hasObservers()).isFalse();
        verify(subscriber, never()).send(anyString());
    }

    /**
     * The shutdown lands inside the publishing compute, after the generation was read and before the pins
     * are written. The sweep then runs over a key snapshot that does not hold this channel, so only the
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
        ObservedSet<TwoPartyPrivateChatMessageReaction> lateReactions = new ObservedSet<>();
        ObservedSet<TwoPartyPrivateChatMessage> lateMessages = new ObservedSet<>();
        lateMessages.add(mockMessage("message-2", lateReactions));
        TwoPartyPrivateChatChannel other = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(other.getId()).thenReturn("discussion.a-c");
        when(other.getChatMessages()).thenReturn(lateMessages);
        onNextChannelScan.set(teardown);

        channels.add(other);

        assertThat(lateMessages.hasObservers()).isFalse();
        assertThat(lateReactions.hasObservers()).isFalse();
        lateReactions.add(reaction("reaction-1", false));
        verify(subscriber, never()).send(anyString());
        // Once for the key install published under, once for the key the self-unbind took out. A single
        // call means install refused instead, and the re-check after publishing went untested.
        verify(other, times(2).description("install published the binding and the self-unbind took it out again"))
                .getId();
    }

    /**
     * The peer is banned after the fact, so the message that carries this reaction is already gone from
     * the message stream. Letting the reaction through would leave the client holding one against a
     * {@code chatMessageId} it never received.
     */
    @Test
    void aReactionFromABannedSenderIsNotPushed() {
        reactions.add(bannedReaction("reaction-1"));

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void theSubscribeSnapshotDropsBannedSendersToo() {
        reactions.add(bannedReaction("reaction-1"));

        assertThat(service.getJsonPayload().orElseThrow()).isEqualTo("[]");
    }

    private TwoPartyPrivateChatMessageReaction bannedReaction(String id) {
        TwoPartyPrivateChatMessageReaction reaction = reaction(id, false);
        // Resolved out of the when(...) argument: a mock call nested there leaves Mockito with an
        // unfinished stubbing, the same trap the reaction() factory documents.
        UserProfile sender = reaction.getSenderUserProfile();
        when(bannedUserService.isUserProfileBanned(sender)).thenReturn(true);
        return reaction;
    }

    private static TwoPartyPrivateChatMessage mockMessage(String id,
                                                          ObservableSet<TwoPartyPrivateChatMessageReaction> reactions) {
        TwoPartyPrivateChatMessage message = mock(TwoPartyPrivateChatMessage.class, RETURNS_DEEP_STUBS);
        when(message.getId()).thenReturn(id);
        when(message.getChatMessageReactions()).thenReturn(reactions);
        return message;
    }

    private static TwoPartyPrivateChatMessageReaction reaction(String id, boolean isRemoved) {
        // Built before the stubbing below starts: mocking inside a when(...) argument leaves Mockito
        // with an unfinished stubbing.
        UserProfile sender = mockUserProfile();
        TwoPartyPrivateChatMessageReaction reaction =
                mock(TwoPartyPrivateChatMessageReaction.class, RETURNS_DEEP_STUBS);
        when(reaction.getId()).thenReturn(id);
        when(reaction.getSenderUserProfile()).thenReturn(sender);
        when(reaction.getReceiverUserProfileId()).thenReturn("receiver");
        when(reaction.getChatChannelId()).thenReturn(CHANNEL_ID);
        when(reaction.getChatMessageId()).thenReturn(MESSAGE_ID);
        when(reaction.isRemoved()).thenReturn(isRemoved);
        // Everything a deep stub cannot supply: a null enum and a null key both throw inside the
        // mapping, which the service catches and logs — so a missing stub here would look exactly like
        // the service deciding not to push. See mockUserProfile for the same problem on the sender side.
        when(reaction.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        when(reaction.getReceiverNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return reaction;
    }
}
