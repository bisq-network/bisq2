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


package bisq.api.chat.common;

import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Topic;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test doubles shared by the public chat tests. Every stub here exists because a deep stub alone
 * would hand the dto mapping a null enum, a null key or a null {@code byte[]}, and the services catch and
 * log the resulting exception — so a missing stub would look exactly like the service deciding not to
 * push.
 */
public final class PublicChatTestMocks {
    public static final String DISCUSSION_ID = SubDomain.DISCUSSION_BISQ.getChannelId();
    public static final String SUPPORT_ID = SubDomain.SUPPORT_SUPPORT.getChannelId();

    private PublicChatTestMocks() {
    }

    /** Exposes whether anything still observes the set, which is what a leaked pin is. */
    public static class ObservedSet<T> extends ObservableSet<T> {
        public boolean hasObservers() {
            return !observers.isEmpty();
        }
    }

    public static UserProfile mockUserProfile(String id) {
        UserProfile profile = mock(UserProfile.class, RETURNS_DEEP_STUBS);
        when(profile.getId()).thenReturn(id);
        when(profile.getNickName()).thenReturn("nick-" + id);
        when(profile.getProofOfWork().getPayload()).thenReturn(new byte[0]);
        when(profile.getProofOfWork().getSolution()).thenReturn(new byte[0]);
        when(profile.getNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return profile;
    }

    /**
     * Built into a local first and stubbed after: Mockito rejects a stubbing nested inside another
     * {@code thenReturn(...)} argument, which {@code mockUserProfile} inside {@code thenReturn} would be.
     */
    public static UserProfile knownProfile(UserProfileService userProfileService, String id) {
        UserProfile profile = mockUserProfile(id);
        when(userProfileService.findUserProfile(id)).thenReturn(Optional.of(profile));
        return profile;
    }

    /**
     * The observable half of the profile store, which the messages and reactions services observe to
     * replay additions that waited for a profile. A real map rather than a stub: the services register
     * a real observer on it, and a test drives a "profile arrives" event by putting into it — see
     * {@link #profileArrives}.
     */
    public static ObservableHashMap<String, UserProfile> observedProfiles(UserProfileService userProfileService) {
        ObservableHashMap<String, UserProfile> profiles = new ObservableHashMap<>();
        when(userProfileService.getUserProfileById()).thenReturn(profiles);
        return profiles;
    }

    /** A profile landing from the network: resolvable from now on, and its arrival event fires. */
    public static UserProfile profileArrives(UserProfileService userProfileService,
                                             ObservableHashMap<String, UserProfile> profiles,
                                             String id) {
        UserProfile profile = knownProfile(userProfileService, id);
        profiles.put(id, profile);
        return profile;
    }

    /** The short forms put the message on Discussions; the Support channel is named explicitly. */
    public static CommonPublicChatMessage mockMessage(String id, String authorId, long date) {
        return mockMessage(id, authorId, date, new ObservableSet<>());
    }

    public static CommonPublicChatMessage mockMessage(String id,
                                                      String authorId,
                                                      long date,
                                                      ObservableSet<ChatMessageReaction> reactions) {
        return messageInChannel(DISCUSSION_ID, id, authorId, date, reactions);
    }

    public static CommonPublicChatMessage messageInChannel(String channelId, String id, String authorId, long date) {
        return messageInChannel(channelId, id, authorId, date, new ObservableSet<>());
    }

    public static CommonPublicChatMessage messageInChannel(String channelId,
                                                           String id,
                                                           String authorId,
                                                           long date,
                                                           ObservableSet<ChatMessageReaction> reactions) {
        CommonPublicChatMessage message = mock(CommonPublicChatMessage.class, RETURNS_DEEP_STUBS);
        when(message.getId()).thenReturn(id);
        when(message.getAuthorUserProfileId()).thenReturn(authorId);
        when(message.getDate()).thenReturn(date);
        when(message.getChannelId()).thenReturn(channelId);
        when(message.getChatChannelDomain()).thenReturn(SubDomain.from(channelId).getChatChannelDomain());
        when(message.getText()).thenReturn(Optional.of("hi"));
        when(message.getCitation()).thenReturn(Optional.empty());
        when(message.getChatMessageType()).thenReturn(ChatMessageType.TEXT);
        when(message.isWasEdited()).thenReturn(false);
        when(message.isExpired()).thenReturn(false);
        when(message.getChatMessageReactions()).thenReturn(reactions);
        return message;
    }

    public static CommonPublicChatMessageReaction mockReaction(String id,
                                                               String senderId,
                                                               String messageId,
                                                               int reactionId) {
        return reactionInChannel(DISCUSSION_ID, id, senderId, messageId, reactionId);
    }

    public static CommonPublicChatMessageReaction reactionInChannel(String channelId,
                                                                    String id,
                                                                    String senderId,
                                                                    String messageId,
                                                                    int reactionId) {
        CommonPublicChatMessageReaction reaction = mock(CommonPublicChatMessageReaction.class, RETURNS_DEEP_STUBS);
        when(reaction.getId()).thenReturn(id);
        when(reaction.getUserProfileId()).thenReturn(senderId);
        when(reaction.getChatChannelId()).thenReturn(channelId);
        when(reaction.getChatChannelDomain()).thenReturn(SubDomain.from(channelId).getChatChannelDomain());
        when(reaction.getChatMessageId()).thenReturn(messageId);
        when(reaction.getReactionId()).thenReturn(reactionId);
        when(reaction.getDate()).thenReturn(1L);
        return reaction;
    }

    /**
     * {@code getId()} answers the migrated id, so a channel deserialized from a sub-domain that was
     * consolidated away still reports the live id — which is what the API addresses it by.
     */
    public static CommonPublicChatChannel mockChannel(SubDomain subDomain,
                                                      ObservableSet<CommonPublicChatMessage> messages) {
        CommonPublicChatChannel channel = mock(CommonPublicChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(subDomain.migrate().getChannelId());
        when(channel.getSubDomain()).thenReturn(subDomain);
        when(channel.getChatChannelDomain()).thenReturn(subDomain.getChatChannelDomain());
        when(channel.getChatMessages()).thenReturn(messages);
        // Res is not loaded in unit tests, so the localized strings are stubbed.
        when(channel.getDisplayString()).thenReturn(subDomain.getTitle());
        when(channel.getDescription()).thenReturn("Channel " + subDomain.getChannelId());
        return channel;
    }

    /** The two public channels the API serves, each behind the service its domain owns. */
    public static PublicChatChannels publicChatChannels(CommonPublicChatChannel discussionChannel,
                                                        CommonPublicChatChannel supportChannel) {
        return new PublicChatChannels(Map.of(
                ChatChannelDomain.DISCUSSION, mockChannelService(discussionChannel),
                ChatChannelDomain.SUPPORT, mockChannelService(supportChannel)));
    }

    /** One service per domain, as Bisq 2 wires them; the API only ever reads getChannels() off them. */
    public static CommonPublicChatChannelService mockChannelService(CommonPublicChatChannel channel) {
        CommonPublicChatChannelService service = mock(CommonPublicChatChannelService.class);
        ObservableSet<CommonPublicChatChannel> channels = new ObservableSet<>();
        channels.add(channel);
        when(service.getChannels()).thenReturn(channels);
        return service;
    }

    public static Subscriber mockSubscriber(Topic topic, String subscriberId) {
        Subscriber subscriber = mock(Subscriber.class);
        when(subscriber.getTopic()).thenReturn(topic);
        when(subscriber.getSubscriberId()).thenReturn(subscriberId);
        AtomicInteger sequenceNumber = new AtomicInteger();
        when(subscriber.incrementAndGetSequenceNumber()).thenAnswer(i -> sequenceNumber.incrementAndGet());
        return subscriber;
    }

    /** Subscribers that named no channel, so they are served and pushed every public channel. */
    public static void subscribed(SubscriberRepository subscriberRepository, Topic topic, Set<Subscriber> subscribers) {
        Map<SubscriptionSpecifier, Set<Subscriber>> result = subscribers.isEmpty()
                ? Collections.emptyMap()
                : Map.of(new SubscriptionSpecifier(topic, Optional.empty()), subscribers);
        when(subscriberRepository.findSubscribers(topic)).thenReturn(result);
        when(subscriberRepository.findSubscribers(topic, Optional.empty())).thenReturn(subscribers);
    }

    /** A subscribe request naming a channel, or none when the id is null. */
    public static SubscriptionRequest subscriptionRequest(@Nullable String channelId) {
        SubscriptionRequest request = mock(SubscriptionRequest.class);
        when(request.getParameter()).thenReturn(channelId);
        return request;
    }

    /** Subscribers scoped to one channel id, which hear about that channel only. */
    public static void subscribedToChannel(SubscriberRepository subscriberRepository,
                                           Topic topic,
                                           String channelId,
                                           Set<Subscriber> subscribers) {
        when(subscriberRepository.findSubscribers(topic, Optional.of(channelId))).thenReturn(subscribers);
    }

    public static String event(String modificationType) {
        return "\"modificationType\":\"" + modificationType + "\"";
    }

    /** The single json the subscriber was sent; fails the verification if it was sent none or several. */
    public static String sentJson(Subscriber subscriber) {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(subscriber).send(json.capture());
        return json.getValue();
    }

    public static List<String> allSentJson(Subscriber subscriber) {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(subscriber, atLeast(0)).send(json.capture());
        return json.getAllValues();
    }

    /**
     * Runs the service's push executor on the calling thread, so pushes are observable without draining.
     * Deliberately keeps running after {@code shutdown()}: a rejection here would be a second reason for
     * a push not to happen, and {@code shutdownStopsListeningToNotifications} would then pass on a
     * service that leaked its observer. The rejection path is covered by
     * {@link QueueingExecutorService#rejectNext()} instead.
     */
    public static class SameThreadExecutorService extends AbstractExecutorService {
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

    /**
     * Queues without running, which is the only way coalescing is observable: with
     * {@link SameThreadExecutorService} a task runs before the next one is submitted, so a burst of
     * changes looks the same whether it was coalesced or not.
     */
    public static class QueueingExecutorService extends AbstractExecutorService {
        private final List<Runnable> queued = new ArrayList<>();
        private volatile boolean shutdown;
        private boolean rejectNext;

        @Override
        public void execute(Runnable command) {
            if (shutdown) {
                throw new RejectedExecutionException();
            }
            if (rejectNext) {
                rejectNext = false;
                throw new RejectedExecutionException();
            }
            queued.add(command);
        }

        public int queued() {
            return queued.size();
        }

        /** Runs what is queued now. Tasks queued by those tasks stay for the next drain. */
        public void drain() {
            List<Runnable> tasks = List.copyOf(queued);
            queued.clear();
            tasks.forEach(Runnable::run);
        }

        /** Makes the next submit fail, as one racing a shutdown does. */
        public void rejectNext() {
            rejectNext = true;
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
