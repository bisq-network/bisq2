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
import bisq.api.chat.common.PublicChatTestMocks.ObservedSet;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static bisq.api.chat.common.PublicChatTestMocks.DISCUSSION_ID;
import static bisq.api.chat.common.PublicChatTestMocks.SUPPORT_ID;
import static bisq.api.chat.common.PublicChatTestMocks.allSentJson;
import static bisq.api.chat.common.PublicChatTestMocks.event;
import static bisq.api.chat.common.PublicChatTestMocks.knownProfile;
import static bisq.api.chat.common.PublicChatTestMocks.messageInChannel;
import static bisq.api.chat.common.PublicChatTestMocks.mockChannel;
import static bisq.api.chat.common.PublicChatTestMocks.mockMessage;
import static bisq.api.chat.common.PublicChatTestMocks.mockReaction;
import static bisq.api.chat.common.PublicChatTestMocks.mockSubscriber;
import static bisq.api.chat.common.PublicChatTestMocks.observedProfiles;
import static bisq.api.chat.common.PublicChatTestMocks.profileArrives;
import static bisq.api.chat.common.PublicChatTestMocks.publicChatChannels;
import static bisq.api.chat.common.PublicChatTestMocks.reactionInChannel;
import static bisq.api.chat.common.PublicChatTestMocks.sentJson;
import static bisq.api.chat.common.PublicChatTestMocks.subscribed;
import static bisq.api.chat.common.PublicChatTestMocks.subscribedToChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the two-level ban filter and the one thing neither trade nor private chat needs: when a message
 * leaves the channel, its reaction observer goes with it. Also what a reaction binding is filed under,
 * from both sides — the channel and the message id are each load-bearing, and dropping either lets one
 * message unbind another's observer.
 */
class PublicChatReactionsWebSocketServiceTest {
    private static final String AUTHOR = "author";
    private static final String REACTOR = "reactor";
    /** Real messages go through {@code verify()}, which wants a 40 character profile id. */
    private static final String REDELIVERED_AUTHOR = "a".repeat(40);
    private static final String REDELIVERED_ID = "redelivered";
    private static final long REDELIVERED_DATE = System.currentTimeMillis();

    private ObservedSet<CommonPublicChatMessage> messages;
    private ObservableSet<CommonPublicChatMessage> supportMessages;
    private ObservedSet<ChatMessageReaction> reactions;
    private ObservedSet<ChatMessageReaction> supportReactions;
    private CommonPublicChatMessage message;
    private ObservableHashMap<String, UserProfile> profiles;
    private UserProfileService userProfileService;
    private BannedUserService bannedUserService;
    private SubscriberRepository subscriberRepository;
    private Subscriber subscriber;
    private PublicChatReactionsWebSocketService service;

    @BeforeEach
    void setUp() {
        messages = new ObservedSet<>();
        supportMessages = new ObservableSet<>();
        reactions = new ObservedSet<>();
        supportReactions = new ObservedSet<>();
        message = mockMessage("m", AUTHOR, 1, reactions);
        PublicChatChannels channels = publicChatChannels(mockChannel(SubDomain.DISCUSSION_BISQ, messages),
                mockChannel(SubDomain.SUPPORT_SUPPORT, supportMessages));

        userProfileService = mock(UserProfileService.class);
        profiles = observedProfiles(userProfileService);
        knownProfile(userProfileService, AUTHOR);
        knownProfile(userProfileService, REACTOR);
        knownProfile(userProfileService, REDELIVERED_AUTHOR);
        bannedUserService = mock(BannedUserService.class);

        subscriber = mockSubscriber(Topic.PUBLIC_CHAT_REACTIONS, "subscriber-1");
        subscriberRepository = mock(SubscriberRepository.class);
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_REACTIONS, Set.of(subscriber));

        service = new PublicChatReactionsWebSocketService(subscriberRepository, channels,
                userProfileService, bannedUserService);
        service.initialize().join();
        messages.add(message);
        supportMessages.add(messageInChannel(SUPPORT_ID, "s", AUTHOR, 1, supportReactions));
    }

    @Test
    void aVisibleReactionIsPushedAsAdded() {
        reactions.add(mockReaction("r", REACTOR, "m", 0));

        assertThat(sentJson(subscriber)).contains(event("ADDED")).contains(reactionId("r"));
    }

    @Test
    void aReactionFromABannedSenderIsNotPushed() {
        when(bannedUserService.isUserProfileBanned(REACTOR)).thenReturn(true);

        reactions.add(mockReaction("r", REACTOR, "m", 0));

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aReactionOnAMessageFromABannedAuthorIsNotPushed() {
        when(bannedUserService.isUserProfileBanned(AUTHOR)).thenReturn(true);

        reactions.add(mockReaction("r", REACTOR, "m", 0));

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aRemovedReactionIsPushedAsRemoved() {
        CommonPublicChatMessageReaction reaction = mockReaction("r", REACTOR, "m", 0);
        reactions.add(reaction);

        reactions.remove(reaction);

        assertThat(allSentJson(subscriber).getLast()).contains(event("REMOVED")).contains(reactionId("r"));
    }

    /**
     * The add path filters by visibility and the removal path must not: {@code removeMessageReaction} is
     * not gated on the ban, so a removal does arrive for a reaction whose sender was banned after the
     * push — which is exactly the one that has to be taken back. Filtering removals like additions
     * would leave that reaction on every client.
     */
    @Test
    void aReactionWhoseSenderWasBannedAfterThePushIsStillTakenBack() {
        CommonPublicChatMessageReaction reaction = mockReaction("r", REACTOR, "m", 0);
        reactions.add(reaction);
        when(bannedUserService.isUserProfileBanned(REACTOR)).thenReturn(true);

        reactions.remove(reaction);

        // Sized as well as read from the end, so this cannot pass on a service that pushed the removal
        // and nothing else — the add has to have gone out before the ban for the removal to matter.
        List<String> sent = allSentJson(subscriber);
        assertThat(sent).hasSize(2);
        assertThat(sent.getLast()).contains(event("REMOVED")).contains(reactionId("r"));
    }

    @Test
    void theSnapshotAppliesTheSameFilter() {
        reactions.add(mockReaction("visible", REACTOR, "m", 0));
        reactions.add(mockReaction("banned", "banned-reactor", "m", 0));
        // Registered as known on purpose: an unresolvable sender is dropped by findDto's catch, so
        // without this the ban below would carry no weight and the filter could be deleted unnoticed.
        knownProfile(userProfileService, "banned-reactor");
        when(bannedUserService.isUserProfileBanned("banned-reactor")).thenReturn(true);

        String payload = service.getJsonPayload().orElseThrow();

        assertThat(payload).contains("\"id\":\"visible\"").doesNotContain("\"id\":\"banned\"");
    }

    /** The snapshot's filter is two-level: a banned message author hides the reactions too. */
    @Test
    void theSnapshotAlsoFiltersByTheMessageAuthor() {
        reactions.add(mockReaction("r", REACTOR, "m", 0));
        when(bannedUserService.isUserProfileBanned(AUTHOR)).thenReturn(true);

        String payload = service.getJsonPayload().orElseThrow();

        assertThat(payload).doesNotContain("\"id\":\"r\"");
    }

    @Test
    void aSnapshotScopedToOneChannelLeavesTheOtherOut() {
        reactions.add(mockReaction("d", REACTOR, "m", 0));
        supportReactions.add(reactionInChannel(SUPPORT_ID, "s", REACTOR, "s", 0));

        String payload = service.getJsonPayload(Optional.of(SUPPORT_ID)).orElseThrow();

        assertThat(payload).contains("\"id\":\"s\"").doesNotContain("\"id\":\"d\"");
    }

    @Test
    void aReactionOnAnotherChannelDoesNotReachAChannelScopedSubscriber() {
        Subscriber scoped = mockSubscriber(Topic.PUBLIC_CHAT_REACTIONS, "subscriber-2");
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_REACTIONS, Set.of());
        subscribedToChannel(subscriberRepository, Topic.PUBLIC_CHAT_REACTIONS, DISCUSSION_ID, Set.of(scoped));

        supportReactions.add(reactionInChannel(SUPPORT_ID, "s", REACTOR, "s", 0));
        reactions.add(mockReaction("d", REACTOR, "m", 0));

        // sentJson verifies a single send, so the support reaction never reached this subscriber.
        assertThat(sentJson(scoped)).contains(reactionId("d"));
    }

    /**
     * Message ids are not unique across channels. {@code ChatMessage.verify} bounds the id's length and
     * nothing else, so a peer publishing to Support picks any id it likes, including one already in use
     * on Discussions. Keying the reaction bindings by message id alone lets that message evict the
     * observer of an unrelated one, and the reactions on the evicted message stop reaching subscribers
     * for the life of the process — a peer choosing the id it wants silenced.
     */
    @Test
    void aMessageInAnotherChannelReusingAnIdDoesNotUnbindTheOriginalsObserver() {
        supportMessages.add(messageInChannel(SUPPORT_ID, "m", AUTHOR, 2));

        reactions.add(mockReaction("r", REACTOR, "m", 0));

        assertThat(sentJson(subscriber)).contains(event("ADDED")).contains(reactionId("r"));
    }

    /**
     * The other side of the same key. Widening it to the channel is only half the answer: a channel
     * holds thousands of messages, so the id has to stay in it or every message in a channel files
     * under the same entry and each arrival unbinds the one before it. Found by mutation — keying on
     * the channel alone left the rest of this class green.
     */
    @Test
    void aSecondMessageInTheSameChannelDoesNotUnbindTheFirstsObserver() {
        messages.add(mockMessage("m2", AUTHOR, 2));

        reactions.add(mockReaction("r", REACTOR, "m", 0));

        assertThat(sentJson(subscriber)).contains(reactionId("r"));
    }

    /**
     * The other half: the id collides only while the second message is in its channel, so the teardown
     * has to leave the first one's binding alone as well. Under a map keyed by the id, the second
     * message's removal takes out the single entry that is left and nothing rebinds the first.
     */
    @Test
    void removingThatMessageDoesNotLeaveTheOriginalUnobserved() {
        CommonPublicChatMessage sameId = messageInChannel(SUPPORT_ID, "m", AUTHOR, 2);
        supportMessages.add(sameId);

        supportMessages.remove(sameId);

        reactions.add(mockReaction("r", REACTOR, "m", 0));
        assertThat(sentJson(subscriber)).contains(reactionId("r"));
    }

    /**
     * The id is not unique within a channel either: {@code ChatMessage.verify} bounds its length and
     * nothing else, and the channel set stores by full-object equality, so a peer can publish a
     * <em>different</em> message under an id that is live in the same channel and both stay side by
     * side. A single binding per key would let the imposter displace and unbind the original's
     * observer — the same silencing as across channels, through the front door.
     */
    @Test
    void aSameChannelMessageReusingALiveIdDoesNotUnbindTheOriginalsObserver() {
        messages.add(mockMessage("m", AUTHOR, 2, new ObservedSet<>()));

        reactions.add(mockReaction("r", REACTOR, "m", 0));

        assertThat(sentJson(subscriber)).contains(event("ADDED")).contains(reactionId("r"));
    }

    /** The teardown half: the imposter leaving must take its own binding and nobody else's. */
    @Test
    void removingTheImposterLeavesTheOriginalObservedAndItselfNot() {
        ObservedSet<ChatMessageReaction> imposterReactions = new ObservedSet<>();
        CommonPublicChatMessage imposter = mockMessage("m", AUTHOR, 2, imposterReactions);
        messages.add(imposter);

        messages.remove(imposter);

        assertThat(imposterReactions.hasObservers()).isFalse();
        reactions.add(mockReaction("r", REACTOR, "m", 0));
        assertThat(sentJson(subscriber)).contains(reactionId("r"));
    }

    /**
     * The reaction counterpart of the messages topic's replay: a reaction whose sender's profile has
     * not arrived yet is parked and pushed when it does — see
     * {@code aMessageWaitingForItsAuthorIsPushedWhenTheProfileArrives}.
     */
    @Test
    void aReactionWaitingForItsSenderIsPushedWhenTheProfileArrives() {
        reactions.add(mockReaction("r", "late-reactor", "m", 0));

        profileArrives(userProfileService, profiles, "late-reactor");

        assertThat(sentJson(subscriber)).contains(event("ADDED")).contains(reactionId("r"));
    }

    /**
     * A reaction skipped because the <em>message author</em> is missing is not parked here: when that
     * author arrives, the messages topic replays the message and the dto embeds the reactions visible
     * by then, so a second delivery from this topic would only duplicate it.
     */
    @Test
    void aReactionOnAMessageAwaitingItsAuthorIsNotReplayedByThisTopic() {
        ObservedSet<ChatMessageReaction> orphanReactions = new ObservedSet<>();
        messages.add(mockMessage("orphan", "late-author", 2, orphanReactions));
        orphanReactions.add(mockReaction("r", REACTOR, "orphan", 0));

        profileArrives(userProfileService, profiles, "late-author");

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aWaitingReactionRemovedBeforeTheProfileArrivesIsNotReplayed() {
        CommonPublicChatMessageReaction reaction = mockReaction("r", "late-reactor", "m", 0);
        reactions.add(reaction);
        reactions.remove(reaction);

        profileArrives(userProfileService, profiles, "late-reactor");

        verify(subscriber, never()).send(anyString());
    }

    /** See {@code PublicChatMessagesWebSocketServiceTest#aDrainedAuthorLeavesNoKeyBehind}. */
    @Test
    void aDrainedSenderLeavesNoKeyBehind() {
        CommonPublicChatMessageReaction reaction = mockReaction("r", "late-reactor", "m", 0);
        reactions.add(reaction);

        reactions.remove(reaction);

        assertThat(service.parkedSenderKeys()).isZero();
    }

    @Test
    void aRemovedMessageDropsItsWaitingReactionsKey() {
        reactions.add(mockReaction("r", "late-reactor", "m", 0));

        messages.remove(message);

        assertThat(service.parkedSenderKeys()).isZero();
    }

    @Test
    void aReplayedSenderLeavesNoKeyBehind() {
        reactions.add(mockReaction("r", "late-reactor", "m", 0));

        profileArrives(userProfileService, profiles, "late-reactor");

        assertThat(service.parkedSenderKeys()).isZero();
    }

    @Test
    void aWaitingReactionWhoseMessageLeftTheChannelIsNotReplayed() {
        reactions.add(mockReaction("r", "late-reactor", "m", 0));
        messages.remove(message);

        profileArrives(userProfileService, profiles, "late-reactor");

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void removingAMessageUnbindsItsReactionObserver() {
        messages.remove(message);

        assertThat(reactions.hasObservers()).isFalse();
        reactions.add(mockReaction("late", REACTOR, "m", 0));
        verify(subscriber, never()).send(anyString());
    }

    /**
     * The window the replay of a long history leaves open: the message leaves its channel after the
     * reaction observer is registered but before its pin is in the map, so the removal that should have
     * unbound the pin finds nothing to unbind.
     */
    @Test
    void aMessageThatLeavesItsChannelWhileBeingBoundLeavesNoPinBehind() {
        ObservedSet<ChatMessageReaction> racingReactions = new ObservedSet<>();
        CommonPublicChatMessage racing = mockMessage("racing", AUTHOR, 2, racingReactions);
        when(racing.getChatMessageReactions()).thenAnswer(invocation -> {
            messages.remove(racing);
            return racingReactions;
        });

        messages.add(racing);

        assertThat(racingReactions.hasObservers()).isFalse();
    }

    /** The same window against a shutdown, which tears the map down while the binding is in flight. */
    @Test
    void aMessageBoundWhileTheServiceShutsDownLeavesNoPinBehind() {
        ObservedSet<ChatMessageReaction> racingReactions = new ObservedSet<>();
        CommonPublicChatMessage racing = mockMessage("racing", AUTHOR, 2, racingReactions);
        when(racing.getChatMessageReactions()).thenAnswer(invocation -> {
            service.shutdown().join();
            return racingReactions;
        });

        messages.add(racing);

        assertThat(racingReactions.hasObservers()).isFalse();
    }

    /**
     * The shutdown window one step later: the binding is already published when the sweep takes its key
     * snapshot, so the sweep misses it and the flag re-read in the bind is what has to take it out. The
     * message is still live in its channel here — it is the service that is going, not the message — so
     * the cleanup cannot be the one that keys on liveness.
     * <p>
     * The shutdown fires from inside the liveness check of the publish, which runs after the flag was
     * read as clear — and on another thread, as in production: from the binding thread it would mutate
     * the pin map inside its own {@code compute}, which that map's contract forbids. The service is its
     * own, with nothing else bound, so the sweep has no entry to contend on.
     */
    @Test
    void aMessageBoundAsShutdownStartsLeavesNoPinBehind() {
        ObservedSet<CommonPublicChatMessage> channelMessages = new ObservedSet<>();
        ObservedSet<ChatMessageReaction> racingReactions = new ObservedSet<>();
        CommonPublicChatMessage racing = mockMessage("racing", AUTHOR, 2, racingReactions);
        CommonPublicChatChannel racingChannel = mockChannel(SubDomain.DISCUSSION_BISQ, channelMessages);
        PublicChatChannels racingChannels = publicChatChannels(racingChannel,
                mockChannel(SubDomain.SUPPORT_SUPPORT, new ObservableSet<>()));
        PublicChatReactionsWebSocketService racingService = new PublicChatReactionsWebSocketService(
                subscriberRepository, racingChannels, userProfileService, bannedUserService);
        racingService.initialize().join();
        AtomicBoolean fired = new AtomicBoolean();
        when(racingChannel.getChatMessages()).thenAnswer(invocation -> {
            if (fired.compareAndSet(false, true)) {
                CompletableFuture.runAsync(() -> racingService.shutdown().join()).join();
            }
            return channelMessages;
        });

        channelMessages.add(racing);

        assertThat(racingReactions.hasObservers()).isFalse();
    }

    @Test
    void shutdownUnbindsEverything() {
        service.shutdown().join();

        assertThat(messages.hasObservers()).isFalse();
        assertThat(reactions.hasObservers()).isFalse();
    }

    /**
     * The emission guard {@code shutdownStarted} exists for, on the added path. The two tests above
     * that shut down from inside the bind assert only that no pin is left behind, and they hand the
     * bind an empty reaction set — so {@code addObserver}'s replay has nothing to deliver and never
     * reaches the guard at all. This one gives it something to replay.
     */
    @Test
    void aReplayRunningWhenTheShutdownStartsPushesNothing() {
        ObservedSet<ChatMessageReaction> racingReactions = new ObservedSet<>();
        racingReactions.add(mockReaction("replayed", REACTOR, "racing", 0));
        CommonPublicChatMessage racing = mockMessage("racing", AUTHOR, 2, racingReactions);
        when(racing.getChatMessageReactions()).thenAnswer(invocation -> {
            service.shutdown().join();
            return racingReactions;
        });

        messages.add(racing);

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The same guard on the removal path, which has its own early return. Driven by a hook observer
     * registered on the reaction set before the message is bound, so it runs first in the
     * copy-on-write snapshot and the service's callback is the one that outlives the shutdown.
     */
    @Test
    void aRemovedReactionCallbackThatOutlivesTheShutdownPushesNothing() {
        ObservedSet<ChatMessageReaction> lateReactions = new ObservedSet<>();
        CommonPublicChatMessage late = mockMessage("late", AUTHOR, 3, lateReactions);
        hookOnRemovedReaction(lateReactions, removed -> service.shutdown().join());
        messages.add(late);
        CommonPublicChatMessageReaction reaction = mockReaction("r", REACTOR, "late", 0);
        lateReactions.add(reaction);
        clearInvocations(subscriber);

        lateReactions.remove(reaction);

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The P2P store re-delivers a message as a fresh instance that is equal to the one it replaced, so
     * {@code equals} cannot tell the two apart and the channel set holds only one of them at a time.
     * Both tests drive that with a hook observer registered before the service, so it runs first in the
     * copy-on-write list and its callback nests inside the notification the service is about to get.
     * Mocks cannot express this: they are equal only by identity.
     */
    @Test
    void aDepartingMessageDoesNotUnbindItsSuccessorsObserver() {
        ObservableSet<CommonPublicChatMessage> channelMessages = new ObservableSet<>();
        CommonPublicChatMessage departing = redeliveredMessage();
        CommonPublicChatMessage successor = redeliveredMessage();
        hookOnRemoved(channelMessages, removed -> {
            if (removed == departing) {
                channelMessages.add(successor);
            }
        });
        serviceOver(channelMessages);
        channelMessages.add(departing);

        channelMessages.remove(departing);

        successor.getChatMessageReactions().add(mockReaction("r", REACTOR, REDELIVERED_ID, 0));
        assertThat(allSentJson(subscriber)).anyMatch(json -> json.contains(reactionId("r")));
    }

    @Test
    void aStaleBindDoesNotDisplaceTheMessageThatReplacedIt() {
        ObservableSet<CommonPublicChatMessage> channelMessages = new ObservableSet<>();
        CommonPublicChatMessage stale = redeliveredMessage();
        CommonPublicChatMessage successor = redeliveredMessage();
        hookOnAdded(channelMessages, added -> {
            if (added == stale) {
                channelMessages.remove(stale);
                channelMessages.add(successor);
            }
        });
        serviceOver(channelMessages);

        channelMessages.add(stale);

        successor.getChatMessageReactions().add(mockReaction("r", REACTOR, REDELIVERED_ID, 0));
        assertThat(allSentJson(subscriber)).anyMatch(json -> json.contains(reactionId("r")));
    }

    /**
     * The production removal path never delivers the bound instance on a restarted node: the channel
     * store deserializes its own copy of every message at startup, the network store deserializes
     * another, and {@code ObservableCollection#remove} drops the stored one by {@code equals} but
     * notifies with the argument. Deciding the teardown on the notified instance's identity would skip
     * the unbind on every such removal and leak one reaction observer per pruned or deleted message.
     */
    @Test
    void aMessageRemovedAsAnEqualCopyStillUnbindsItsReactionObserver() {
        ObservableSet<CommonPublicChatMessage> channelMessages = new ObservableSet<>();
        CommonPublicChatMessage bound = redeliveredMessage();
        serviceOver(channelMessages);
        channelMessages.add(bound);

        channelMessages.remove(redeliveredMessage());

        bound.getChatMessageReactions().add(mockReaction("r", REACTOR, REDELIVERED_ID, 0));
        verify(subscriber, never()).send(anyString());
    }

    private void serviceOver(ObservableSet<CommonPublicChatMessage> channelMessages) {
        PublicChatChannels channels = publicChatChannels(mockChannel(SubDomain.DISCUSSION_BISQ, channelMessages),
                mockChannel(SubDomain.SUPPORT_SUPPORT, new ObservableSet<>()));
        new PublicChatReactionsWebSocketService(subscriberRepository, channels,
                userProfileService, bannedUserService).initialize().join();
    }

    private static void hookOnAdded(ObservableSet<CommonPublicChatMessage> messages,
                                    Consumer<CommonPublicChatMessage> handler) {
        messages.addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(CommonPublicChatMessage element) {
                handler.accept(element);
            }

            @Override
            public void onRemoved(Object element) {
            }

            @Override
            public void onCleared() {
            }
        });
    }

    private static void hookOnRemovedReaction(ObservableSet<ChatMessageReaction> reactions, Consumer<Object> handler) {
        reactions.addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(ChatMessageReaction element) {
            }

            @Override
            public void onRemoved(Object element) {
                handler.accept(element);
            }

            @Override
            public void onCleared() {
            }
        });
    }

    private static void hookOnRemoved(ObservableSet<CommonPublicChatMessage> messages, Consumer<Object> handler) {
        messages.addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(CommonPublicChatMessage element) {
            }

            @Override
            public void onRemoved(Object element) {
                handler.accept(element);
            }

            @Override
            public void onCleared() {
            }
        });
    }

    /** Two calls give equal but distinct instances, which is what a re-delivery looks like. */
    private static CommonPublicChatMessage redeliveredMessage() {
        return new CommonPublicChatMessage(REDELIVERED_ID,
                ChatChannelDomain.DISCUSSION,
                DISCUSSION_ID,
                REDELIVERED_AUTHOR,
                Optional.of("hi"),
                Optional.empty(),
                REDELIVERED_DATE,
                false,
                ChatMessageType.TEXT);
    }

    /** The event carries the payload as an escaped JSON string, hence the escaped quotes. */
    private static String reactionId(String id) {
        return "\\\"id\\\":\\\"" + id + "\\\"";
    }
}
