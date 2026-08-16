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
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatChannelDomain;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
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
import java.util.concurrent.atomic.AtomicInteger;

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
 * ({@code ChatReactionsWebSocketService}) cannot be relied on to cover: the per-channel nesting of the
 * reaction pins, which is the whole reason this class is not a copy, and the two-event shape an
 * un-reaction takes on the wire.
 */
class PrivateChatReactionsWebSocketServiceTest {
    private static final String CHANNEL_ID = "discussion.a-b";
    private static final String MESSAGE_ID = "message-1";

    private ObservableSet<TwoPartyPrivateChatChannel> channels;
    private ObservableSet<TwoPartyPrivateChatMessageReaction> reactions;
    private TwoPartyPrivateChatChannel channel;
    private Subscriber subscriber;
    private BannedUserService bannedUserService;
    private PrivateChatReactionsWebSocketService service;

    @BeforeEach
    void setUp() {
        reactions = new ObservableSet<>();
        TwoPartyPrivateChatMessage message = mock(TwoPartyPrivateChatMessage.class, RETURNS_DEEP_STUBS);
        when(message.getId()).thenReturn(MESSAGE_ID);
        when(message.getChatMessageReactions()).thenReturn(reactions);

        ObservableSet<TwoPartyPrivateChatMessage> messages = new ObservableSet<>();
        messages.add(message);

        channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        when(channel.getChatMessages()).thenReturn(messages);

        // The channel and its message exist before initialize(), because addObserver replays what is
        // already there — which is how the reaction observer of an existing message gets bound at all.
        channels = new ObservableSet<>();
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
     * What the nested {@code chatMessageReactionsPinsByChannelId} buys over the flat message-id map of
     * the trade-chat sibling. Leaving a DM is routine, so a channel's reaction observers have to go with
     * it — otherwise they outlive the channel and keep pushing for a conversation the node dropped.
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
     * case because {@code unbindAllChannelPins} iterates the message-pin keys rather than the reaction-pin
     * ones, on the argument that a channel never holds the second without the first.
     */
    @Test
    void clearingTheChannelCollectionUnbindsTheReactionObserversToo() {
        channels.clear();

        reactions.add(reaction("reaction-1", false));

        verify(subscriber, never()).send(anyString());
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
