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
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static bisq.api.chat.common.PublicChatTestMocks.DISCUSSION_ID;
import static bisq.api.chat.common.PublicChatTestMocks.SUPPORT_ID;
import static bisq.api.chat.common.PublicChatTestMocks.allSentJson;
import static bisq.api.chat.common.PublicChatTestMocks.event;
import static bisq.api.chat.common.PublicChatTestMocks.knownProfile;
import static bisq.api.chat.common.PublicChatTestMocks.messageInChannel;
import static bisq.api.chat.common.PublicChatTestMocks.mockChannel;
import static bisq.api.chat.common.PublicChatTestMocks.mockMessage;
import static bisq.api.chat.common.PublicChatTestMocks.mockSubscriber;
import static bisq.api.chat.common.PublicChatTestMocks.observedProfiles;
import static bisq.api.chat.common.PublicChatTestMocks.profileArrives;
import static bisq.api.chat.common.PublicChatTestMocks.publicChatChannels;
import static bisq.api.chat.common.PublicChatTestMocks.sentJson;
import static bisq.api.chat.common.PublicChatTestMocks.subscribed;
import static bisq.api.chat.common.PublicChatTestMocks.subscribedToChannel;
import static bisq.api.chat.common.PublicChatTestMocks.subscriptionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins what sets this topic apart from trade chat: public messages leave the channel, so removals are
 * pushed; the snapshot is the whole visible history filtered like REST; the visibility filter hides
 * banned, unresolved and expired authors on the live path; and a subscription can be scoped to one of
 * the two public channels.
 */
class PublicChatMessagesWebSocketServiceTest {
    private static final String AUTHOR = "author";
    /** Real messages go through {@code verify()}, which wants a 40 character profile id. */
    private static final String REDELIVERED_AUTHOR = "a".repeat(40);
    private static final String REDELIVERED_ID = "redelivered";
    private static final long REDELIVERED_DATE = System.currentTimeMillis();

    private ObservedSet<CommonPublicChatMessage> messages;
    private ObservedSet<CommonPublicChatMessage> supportMessages;
    private ObservableHashMap<String, UserProfile> profiles;
    private UserProfileService userProfileService;
    private BannedUserService bannedUserService;
    private SubscriberRepository subscriberRepository;
    private Subscriber subscriber;
    private PublicChatMessagesWebSocketService service;

    @BeforeEach
    void setUp() {
        messages = new ObservedSet<>();
        supportMessages = new ObservedSet<>();
        PublicChatChannels channels = publicChatChannels(mockChannel(SubDomain.DISCUSSION_BISQ, messages),
                mockChannel(SubDomain.SUPPORT_SUPPORT, supportMessages));

        userProfileService = mock(UserProfileService.class);
        profiles = observedProfiles(userProfileService);
        knownProfile(userProfileService, AUTHOR);
        bannedUserService = mock(BannedUserService.class);

        subscriber = mockSubscriber(Topic.PUBLIC_CHAT_MESSAGES, "subscriber-1");
        subscriberRepository = mock(SubscriberRepository.class);
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_MESSAGES, Set.of(subscriber));

        service = new PublicChatMessagesWebSocketService(subscriberRepository, channels,
                userProfileService, bannedUserService);
        service.initialize().join();
    }

    @Test
    void aVisibleMessageIsPushedAsAdded() {
        messages.add(mockMessage("m", AUTHOR, 1));

        assertThat(sentJson(subscriber)).contains(event("ADDED")).contains(messageId("m"));
    }

    @Test
    void aMessageFromABannedAuthorIsNotPushed() {
        when(bannedUserService.isUserProfileBanned(AUTHOR)).thenReturn(true);

        messages.add(mockMessage("m", AUTHOR, 1));

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aMessageWhoseAuthorCannotBeResolvedDoesNotReachTheClient() {
        messages.add(mockMessage("m", "unknown", 1));

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The normal shape of a fresh node's inventory sync: a channel's messages land before the profiles
     * of their authors. Without the replay every such message would stay invisible to a live subscriber
     * for the life of its subscription, silently disagreeing with REST and the next snapshot.
     */
    @Test
    void aMessageWaitingForItsAuthorIsPushedWhenTheProfileArrives() {
        messages.add(mockMessage("m", "late-author", 1));

        profileArrives(userProfileService, profiles, "late-author");

        assertThat(sentJson(subscriber)).contains(event("ADDED")).contains(messageId("m"));
    }

    @Test
    void anUnrelatedProfileArrivalReplaysNothing() {
        messages.add(mockMessage("m", "late-author", 1));

        profileArrives(userProfileService, profiles, "someone-else");

        verify(subscriber, never()).send(anyString());
    }

    /** The replay re-checks visibility in full: a profile can arrive for an author banned meanwhile. */
    @Test
    void aWaitingMessageWhoseAuthorArrivesBannedStaysBack() {
        messages.add(mockMessage("m", "late-author", 1));
        when(bannedUserService.isUserProfileBanned("late-author")).thenReturn(true);

        profileArrives(userProfileService, profiles, "late-author");

        verify(subscriber, never()).send(anyString());
    }

    /** A banned author is not worth waiting for: an unban event does not exist to wake the entry. */
    @Test
    void aMessageFromABannedUnresolvedAuthorIsNotParked() {
        when(bannedUserService.isUserProfileBanned("late-author")).thenReturn(true);
        messages.add(mockMessage("m", "late-author", 1));
        when(bannedUserService.isUserProfileBanned("late-author")).thenReturn(false);

        profileArrives(userProfileService, profiles, "late-author");

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aWaitingMessageThatLeftItsChannelIsNotReplayed() {
        CommonPublicChatMessage message = mockMessage("m", "late-author", 1);
        messages.add(message);
        messages.remove(message);

        profileArrives(userProfileService, profiles, "late-author");

        verify(subscriber, never()).send(anyString());
    }

    /**
     * The waiting map's keys are attacker-supplied author ids that may never resolve, so a drained
     * entry has to take its key with it — left behind, one garbage key per fabricated author would be
     * an unbounded leak an adversary grows for free.
     */
    @Test
    void aDrainedAuthorLeavesNoKeyBehind() {
        CommonPublicChatMessage message = mockMessage("m", "late-author", 1);
        messages.add(message);

        messages.remove(message);

        assertThat(service.parkedAuthorKeys()).isZero();
        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aReplayedAuthorLeavesNoKeyBehind() {
        messages.add(mockMessage("m", "late-author", 1));

        profileArrives(userProfileService, profiles, "late-author");

        assertThat(service.parkedAuthorKeys()).isZero();
    }

    /**
     * Message ids are not unique within a channel, and the wire keys messages by id: a {@code REMOVED}
     * while another message is live under the same id would make the client delete what a fresh
     * snapshot still shows, with nothing to ever bring it back. The removal of one collider therefore
     * re-pushes the survivor instead of removing the id.
     */
    @Test
    void removingOneOfTwoMessagesSharingAnIdConvergesOnTheSurvivor() {
        CommonPublicChatMessage original = mockMessage("m", AUTHOR, 1);
        CommonPublicChatMessage imposter = mockMessage("m", AUTHOR, 2);
        messages.add(original);
        messages.add(imposter);

        messages.remove(imposter);

        List<String> sent = allSentJson(subscriber);
        assertThat(sent).hasSize(3);
        assertThat(sent.getLast()).contains(event("ADDED")).contains(messageId("m"));
    }

    /**
     * The honest-traffic shape of the same hazard: the P2P store re-delivers a message as a fresh
     * equal instance, and the predecessor's removal can be processed after the successor's add. Real
     * instances rather than mocks, which are only equal by identity — see the reactions test of the
     * same scenario.
     */
    @Test
    void aDepartingMessageWithALiveSuccessorIsNotTakenBackFromClients() {
        ObservableSet<CommonPublicChatMessage> channelMessages = new ObservableSet<>();
        CommonPublicChatMessage departing = redeliveredMessage();
        CommonPublicChatMessage successor = redeliveredMessage();
        knownProfile(userProfileService, REDELIVERED_AUTHOR);
        hookOnRemoved(channelMessages, removed -> {
            if (removed == departing) {
                channelMessages.add(successor);
            }
        });
        serviceOver(channelMessages);
        channelMessages.add(departing);

        channelMessages.remove(departing);

        List<String> sent = allSentJson(subscriber);
        assertThat(sent).isNotEmpty().noneMatch(json -> json.contains(event("REMOVED")));
        assertThat(sent.getLast()).contains(event("ADDED")).contains(messageId(REDELIVERED_ID));
    }

    @Test
    void aProfileArrivingAfterTheShutdownReplaysNothing() {
        messages.add(mockMessage("m", "late-author", 1));
        service.shutdown().join();

        profileArrives(userProfileService, profiles, "late-author");

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void anExpiredMessageIsNotPushed() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        when(message.isExpired()).thenReturn(true);

        messages.add(message);

        verify(subscriber, never()).send(anyString());
    }

    @Test
    void aRemovedMessageIsPushedAsRemoved() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        messages.add(message);

        messages.remove(message);

        List<String> sent = allSentJson(subscriber);
        assertThat(sent).hasSize(2);
        assertThat(sent.get(1)).contains(event("REMOVED")).contains(messageId("m"));
    }

    /** Expiry is what removes most messages; filtering the removal by it would strand them on the client. */
    @Test
    void aMessageThatExpiredAfterItWasPushedIsStillTakenBack() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        messages.add(message);
        when(message.isExpired()).thenReturn(true);

        messages.remove(message);

        assertThat(allSentJson(subscriber).get(1)).contains(event("REMOVED"));
    }

    @Test
    void anEditArrivesAsARemovalFollowedByAnAddition() {
        CommonPublicChatMessage original = mockMessage("old", AUTHOR, 1);
        messages.add(original);

        messages.remove(original);
        CommonPublicChatMessage edited = mockMessage("new", AUTHOR, 1);
        when(edited.isWasEdited()).thenReturn(true);
        messages.add(edited);

        List<String> sent = allSentJson(subscriber);
        assertThat(sent).hasSize(3);
        assertThat(sent.get(1)).contains(event("REMOVED")).contains(messageId("old"));
        assertThat(sent.get(2)).contains(event("ADDED")).contains(messageId("new")).contains("\\\"wasEdited\\\":true");
    }

    @Test
    void theSnapshotIsTheWholeVisibleHistoryWithTheSameFilterAsRest() {
        for (int i = 0; i < 60; i++) {
            messages.add(mockMessage("m" + i, AUTHOR, i));
        }
        messages.add(mockMessage("banned", "banned-author", 1000));
        knownProfile(userProfileService, "banned-author");
        when(bannedUserService.isUserProfileBanned("banned-author")).thenReturn(true);

        String payload = service.getJsonPayload().orElseThrow();

        // All 60 visible messages, none from the banned author, newest first.
        assertThat(payload.split("\"messageId\"", -1)).hasSize(61);
        assertThat(payload).doesNotContain("\"messageId\":\"banned\"");
        assertThat(payload.indexOf("\"messageId\":\"m59\"")).isLessThan(payload.indexOf("\"messageId\":\"m0\""));
    }

    @Test
    void theSnapshotCoversEveryPublicChannel() {
        messages.add(mockMessage("d", AUTHOR, 1));
        supportMessages.add(messageInChannel(SUPPORT_ID, "s", AUTHOR, 2));

        String payload = service.getJsonPayload().orElseThrow();

        assertThat(payload).contains("\"messageId\":\"d\"").contains("\"messageId\":\"s\"");
    }

    @Test
    void aSnapshotScopedToOneChannelLeavesTheOtherOut() {
        messages.add(mockMessage("d", AUTHOR, 1));
        supportMessages.add(messageInChannel(SUPPORT_ID, "s", AUTHOR, 2));

        String payload = service.getJsonPayload(Optional.of(SUPPORT_ID)).orElseThrow();

        assertThat(payload).contains("\"messageId\":\"s\"").doesNotContain("\"messageId\":\"d\"");
    }

    @Test
    void aMessageOnAnotherChannelDoesNotReachAChannelScopedSubscriber() {
        Subscriber scoped = mockSubscriber(Topic.PUBLIC_CHAT_MESSAGES, "subscriber-2");
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_MESSAGES, Set.of());
        subscribedToChannel(subscriberRepository, Topic.PUBLIC_CHAT_MESSAGES, DISCUSSION_ID, Set.of(scoped));

        supportMessages.add(messageInChannel(SUPPORT_ID, "s", AUTHOR, 1));
        messages.add(mockMessage("d", AUTHOR, 2));

        // sentJson verifies a single send, so the support message never reached this subscriber.
        assertThat(sentJson(scoped)).contains(messageId("d"));
    }

    /**
     * The bucket a subscriber is filed under and the bucket a push looks in are both this method's
     * answer, so a padded parameter has to canonicalize to what the channel's own id canonicalizes to.
     */
    @Test
    void aPaddedChannelIdLandsInThatChannelsBucket() {
        Optional<String> padded = service.canonicalizeParameter(Optional.of("  " + DISCUSSION_ID + "  "));

        assertThat(padded).isEqualTo(service.canonicalizeParameter(Optional.of(DISCUSSION_ID)))
                .contains(DISCUSSION_ID);
        assertThatCode(() -> service.validate(subscriptionRequest(" " + DISCUSSION_ID + " ")))
                .doesNotThrowAnyException();
    }

    /**
     * A parameter of nothing but padding is no parameter, so the subscription covers every channel
     * rather than being rejected for naming a channel whose id is the empty string.
     */
    @Test
    void aBlankChannelIdIsNoScopeAtAll() {
        assertThat(service.canonicalizeParameter(Optional.of("   "))).isEmpty();
        assertThatCode(() -> service.validate(subscriptionRequest("   "))).doesNotThrowAnyException();
    }

    /** Answered at subscribe rather than with a stream that would never carry anything. */
    @Test
    void subscribingToAnUnknownChannelIsRejected() {
        assertThatThrownBy(() -> service.validate(subscriptionRequest("discussion.nope")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * The dto is built after the subscriber lookup, not before, so a message arriving with nobody
     * listening costs nothing.
     * <p>
     * Asserted through the dto build rather than through {@code send}: with the subscriber out of the
     * repository, "nothing was sent" holds even if the service does nothing at all, and it would still
     * hold with the early return deleted, because the mapping would run and then iterate an empty list.
     * The visibility gate reads only the author id and the expiry; the text is read by the mapping alone.
     */
    @Test
    void nothingIsSerialisedWithoutSubscribers() {
        subscribed(subscriberRepository, Topic.PUBLIC_CHAT_MESSAGES, Set.of());
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);

        messages.add(message);

        verify(message, never()).getText();
        verify(subscriber, never()).send(anyString());
    }

    @Test
    void shutdownUnbindsTheMessageObservers() {
        service.shutdown().join();

        assertThat(messages.hasObservers()).isFalse();
        messages.add(mockMessage("m", AUTHOR, 1));
        verify(subscriber, never()).send(anyString());
    }

    /**
     * The emission guard {@code shutdownStarted} exists for. Observers are notified from a
     * copy-on-write snapshot, so a callback that began before the shutdown unbound its pin still
     * reaches the service afterwards. Driven by a hook observer registered before the service, which
     * therefore runs first in that snapshot and shuts the service down from inside the very
     * notification the service is about to receive.
     * <p>
     * {@code shutdownUnbindsTheMessageObservers} does not cover this: it shuts down before it adds
     * anything, so the pin is already gone, the service's callback never runs, and that test stays
     * green with the guard deleted.
     */
    @Test
    void anAddedCallbackThatOutlivesTheShutdownPushesNothing() {
        ObservableSet<CommonPublicChatMessage> channelMessages = new ObservableSet<>();
        AtomicReference<PublicChatMessagesWebSocketService> racing = new AtomicReference<>();
        hookOnAdded(channelMessages, added -> racing.get().shutdown().join());
        racing.set(serviceOver(channelMessages));

        channelMessages.add(mockMessage("m", AUTHOR, 1));

        verify(subscriber, never()).send(anyString());
    }

    /** The same guard on the removal path, which has its own early return. */
    @Test
    void aRemovedCallbackThatOutlivesTheShutdownPushesNothing() {
        ObservableSet<CommonPublicChatMessage> channelMessages = new ObservableSet<>();
        AtomicReference<PublicChatMessagesWebSocketService> racing = new AtomicReference<>();
        hookOnRemoved(channelMessages, removed -> racing.get().shutdown().join());
        racing.set(serviceOver(channelMessages));
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        channelMessages.add(message);
        clearInvocations(subscriber);

        channelMessages.remove(message);

        verify(subscriber, never()).send(anyString());
    }

    private PublicChatMessagesWebSocketService serviceOver(ObservableSet<CommonPublicChatMessage> channelMessages) {
        PublicChatChannels channels = publicChatChannels(mockChannel(SubDomain.DISCUSSION_BISQ, channelMessages),
                mockChannel(SubDomain.SUPPORT_SUPPORT, new ObservableSet<>()));
        PublicChatMessagesWebSocketService over = new PublicChatMessagesWebSocketService(subscriberRepository,
                channels, userProfileService, bannedUserService);
        over.initialize().join();
        return over;
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
    private static String messageId(String id) {
        return "\\\"messageId\\\":\\\"" + id + "\\\"";
    }
}
