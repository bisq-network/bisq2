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

import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatMessageType;
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.api.web_socket.domain.chat.private_chat.PrivateChatTestMocks.mockUserProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the one thing this service does that its trade-chat sibling does not: dropping messages whose
 * sender has been banned. Bisq 2 already rejects banned senders inbound, so the filter only covers a
 * peer banned <i>after</i> their messages arrived — a state no other test in this package produces.
 */
class PrivateChatMessagesWebSocketServiceTest {
    private static final String CHANNEL_ID = "discussion.a-b";

    private ObservableSet<TwoPartyPrivateChatChannel> channels;
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
        channels = new ObservableSet<>();
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

    private UserProfile banned(boolean isBanned) {
        UserProfile sender = mockUserProfile();
        when(bannedUserService.isUserProfileBanned(sender)).thenReturn(isBanned);
        return sender;
    }

    private static TwoPartyPrivateChatMessage messageFrom(UserProfile sender) {
        TwoPartyPrivateChatMessage message = mock(TwoPartyPrivateChatMessage.class, RETURNS_DEEP_STUBS);
        when(message.getSenderUserProfile()).thenReturn(sender);
        when(message.getCitation()).thenReturn(Optional.empty());
        when(message.getText()).thenReturn(Optional.of("hi"));
        when(message.getChatMessageReactions()).thenReturn(new ObservableSet<>());
        // Everything a deep stub cannot supply: a null enum and a null key both throw inside the
        // mapping, which the service catches and logs — so a missing stub here would look exactly like
        // the filter dropping the message. See mockUserProfile for the same problem on the profile side.
        when(message.getChatMessageType()).thenReturn(ChatMessageType.TEXT);
        when(message.getReceiverNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return message;
    }
}
