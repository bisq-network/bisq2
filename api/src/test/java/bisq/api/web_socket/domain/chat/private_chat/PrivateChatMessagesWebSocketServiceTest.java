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
    private ObservableSet<TwoPartyPrivateChatChannel> channels;
    private ObservableSet<TwoPartyPrivateChatMessage> messages;
    private BannedUserService bannedUserService;
    private Subscriber subscriber;
    private PrivateChatMessagesWebSocketService service;

    @BeforeEach
    void setUp() {
        messages = new ObservableSet<>();
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn("discussion.a-b");
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
