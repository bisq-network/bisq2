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
import bisq.api.chat.common.PublicChatDtoFactory;
import bisq.api.dto.chat.common.CommonPublicChatMessageDto;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.api.web_socket.subscription.Topic.PUBLIC_CHAT_MESSAGES;

/**
 * Pushes the messages of the public chat channels. The subscribe snapshot is the whole visible history
 * of the subscribed channels (the mobile client searches messages locally, see
 * {@link PublicChatDtoFactory#visibleMessagesNewestFirst}; REST serves the same list for reloads).
 * After that, {@code ADDED} for every new message and {@code REMOVED} for every message that
 * leaves a channel — a deletion, the removal half of an edit, or the P2P store pruning expired data,
 * which comes in bursts every few minutes.
 * <p>
 * Structurally {@code TradeChatMessagesWebSocketService}, not the private chat sibling: none of the
 * channel ownership machinery private chat needs applies here.
 */
public class PublicChatMessagesWebSocketService extends ChannelScopedWebSocketService {
    private final UserProfileService userProfileService;
    private final List<Pin> messagesPins = new ArrayList<>();
    private Pin profilesPin;
    /**
     * The additions that could not be pushed because the author's profile had not arrived yet, keyed by
     * the profile id they wait for. On a fresh node the P2P store routinely delivers a channel's
     * messages before their authors' profiles, so without this every such message would be invisible to
     * a live subscriber until it reconnected for a new snapshot. No extra retention on either axis:
     * every parked message is still held by its channel and {@link #handleRemovedMessage} drops the
     * parked entry when the channel lets go of it, and an emptied set's key is unlinked right where it
     * is drained ({@link #pruneIfEmpty}), so fabricated author ids cannot accumulate as garbage keys.
     * Sets claimed exclusively — see {@link #park}.
     */
    private final Map<String, Set<CommonPublicChatMessage>> awaitingAuthor = new ConcurrentHashMap<>();
    /**
     * Observers are notified from a copy-on-write snapshot, so a callback that began before the shutdown
     * unbound its pin still reaches this service afterwards; this keeps it from pushing.
     */
    private volatile boolean shutdownStarted;

    public PublicChatMessagesWebSocketService(SubscriberRepository subscriberRepository,
                                             PublicChatChannels channels,
                                             UserProfileService userProfileService,
                                             BannedUserService bannedUserService) {
        super(subscriberRepository, PUBLIC_CHAT_MESSAGES, channels, userProfileService, bannedUserService);
        this.userProfileService = userProfileService;
    }

    /**
     * The channel set is fixed by {@code CommonPublicChatChannelService.initialize}, which completes
     * before this service starts, and no public channel is added or removed afterwards, so the messages
     * of each channel are observed once and for all.
     */
    @Override
    public CompletableFuture<Boolean> initialize() {
        // Registered before the channel observers, whose registration replays the persisted history:
        // this way the profile observer's own registration replay of the profile store meets an empty
        // pending map and costs nothing, and every message parked from then on has its put to wake it.
        profilesPin = userProfileService.getUserProfileById().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String profileId, UserProfile profile) {
                replayFor(profileId);
            }
        });
        channels.getChannels().forEach(channel ->
                messagesPins.add(channel.getChatMessages().addObserver(new CollectionObserver<>() {
                    @Override
                    public void onAdded(CommonPublicChatMessage message) {
                        handleAddedMessage(message);
                    }

                    @Override
                    public void onRemoved(Object element) {
                        if (element instanceof CommonPublicChatMessage message) {
                            handleRemovedMessage(message);
                        }
                    }

                    @Override
                    public void onCleared() {
                        // Never happens: nothing calls clear or setAll on a public channel's messages,
                        // they are only removed one by one.
                    }
                })));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        messagesPins.forEach(Pin::unbind);
        messagesPins.clear();
        if (profilesPin != null) {
            profilesPin.unbind();
            profilesPin = null;
        }
        awaitingAuthor.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(Optional.empty());
    }

    @Override
    public Optional<String> getJsonPayload(Optional<String> parameter) {
        List<CommonPublicChatMessage> messages = channelsOf(parameter)
                .flatMap(channel -> channel.getChatMessages().stream())
                .toList();
        return toJson(dtoFactory.visibleMessageDtosNewestFirst(messages));
    }

    private void handleAddedMessage(CommonPublicChatMessage message) {
        if (shutdownStarted) {
            return;
        }
        // One profile-store read classifies push against park, see PublicChatDtoFactory#isVisible:
        // with each predicate reading the store again, a profile landing between the two reads is a
        // lost wakeup — not visible yet, nothing parked for the replay to find, not awaited anymore.
        Optional<UserProfile> author = dtoFactory.findProfile(message.getAuthorUserProfileId());
        if (dtoFactory.isVisible(message, author)) {
            push(message, ModificationType.ADDED);
        } else if (dtoFactory.awaitsAuthorProfile(message, author)) {
            park(message);
        }
    }

    /**
     * Parks the message until its author's profile arrives, then double-checks: the profile can land
     * between the visibility check that sent us here and the entry being parked, and the put that
     * carried it may have found the pending map empty. Whoever takes the entry out of its set pushes —
     * set removal is atomic, so however this races {@link #replayFor} the message is pushed once.
     * <p>
     * The add runs inside the map update on purpose: {@link #pruneIfEmpty} decides under the same
     * per-key lock whether a set is empty, so an add can never land on a set that a prune is about to
     * unlink — outside the lock, a message parked into such an orphan whose profile never arrives
     * would be lost. The claim stays on the set instance rather than going back through the map: a
     * concurrent {@link #replayFor} detaches the whole set, and a map lookup would then miss an entry
     * its weakly consistent iteration can still be about to claim.
     */
    private void park(CommonPublicChatMessage message) {
        String authorId = message.getAuthorUserProfileId();
        AtomicReference<Set<CommonPublicChatMessage>> ref = new AtomicReference<>();
        awaitingAuthor.compute(authorId, (id, present) -> {
            Set<CommonPublicChatMessage> set = present != null ? present : ConcurrentHashMap.newKeySet();
            set.add(message);
            ref.set(set);
            return set;
        });
        Set<CommonPublicChatMessage> parked = ref.get();
        if (dtoFactory.isVisible(message) && parked.remove(message)) {
            pruneIfEmpty(authorId, parked);
            push(message, ModificationType.ADDED);
        }
    }

    /**
     * Takes an emptied set's key out of the map, so that the map's growth is bounded by the authors
     * currently waited for rather than by every unresolvable author id ever seen — those are free for a
     * peer to fabricate ({@code ChatMessage.verify} checks the id's length and nothing else), so a key
     * left behind per fabricated author would be an unbounded leak. Guarded by identity as well as
     * emptiness, both read under the key's lock: only the set the caller drained may be unlinked, and
     * only while no parker has refilled it — adds run under the same lock, see {@link #park}.
     */
    private void pruneIfEmpty(String profileId, Set<CommonPublicChatMessage> parked) {
        if (parked.isEmpty()) {
            awaitingAuthor.computeIfPresent(profileId, (id, present) ->
                    present == parked && present.isEmpty() ? null : present);
        }
    }

    /**
     * Replays the additions that waited for this profile. Claiming the whole set under the profile id
     * races a concurrent {@link #park}, which may then be adding to a set no longer reachable from the
     * map — that is fine, because a parker only reaches that state after this profile was already
     * stored, so its own double-check sees the profile and claims its entry out of the orphaned set.
     * Each claimed message is re-checked in full: it may have left its channel or lost its visibility
     * again while it waited.
     */
    private void replayFor(String profileId) {
        Set<CommonPublicChatMessage> parked = awaitingAuthor.remove(profileId);
        if (parked == null) {
            return;
        }
        parked.forEach(message -> {
            if (!shutdownStarted
                    && parked.remove(message)
                    && isStillInChannel(message)
                    && dtoFactory.isVisible(message)) {
                push(message, ModificationType.ADDED);
            }
        });
    }

    /**
     * By {@code equals}, deliberately: the parked instance and the copy a removal notifies with are
     * equal but distinct (see {@code PublicChatReactionsWebSocketService#unbindMessage}), and here the
     * content being live is what matters, not which instance carries it.
     */
    private boolean isStillInChannel(CommonPublicChatMessage message) {
        return channels.findChannel(message.getChannelId())
                .map(channel -> channel.getChatMessages().contains(message))
                .orElse(false);
    }

    /**
     * Not filtered by visibility, and it does not need to be: the domain gates both ends of the channel
     * on the same ban check, so a message from a banned author never entered it
     * ({@code ChatChannelService#addMessage}) and cannot arrive here. What does arrive is a message that
     * expired or was deleted while its author was still valid, and that has to be taken back.
     * <p>
     * The gate on the removal side is also a known limitation, and it is wider than the ban.
     * {@code PublicChatChannelService#processRemovedMessage} gates on {@code ChatChannelService#isValid},
     * which returns false for a banned author <em>and</em> for one who is currently rate limited. So a
     * message whose author was banned after delivery, or one pruned by the store while its author happens
     * to be over the limit, is not removed: the removal is offered once and dropped, the message never
     * leaves the channel and no {@code REMOVED} is pushed for it. A subscriber keeps it until it
     * reconnects and gets a snapshot without it. Desktop has the same shape — the message stays in its
     * list and is only dropped when the filter predicate next runs.
     * <p>
     * {@code push} leaves the same window for a different reason: it maps through {@code findDto},
     * which needs the author's profile, so an author pruned from the profile store after the message
     * was pushed produces no {@code REMOVED} either. See
     * {@code PublicChatReactionsWebSocketService#handleRemovedReaction}, which carries the same note.
     */
    private void handleRemovedMessage(CommonPublicChatMessage message) {
        if (shutdownStarted) {
            return;
        }
        dropParked(message);
        // No visible survivor falls through to REMOVED on purpose: the snapshot would not show the id
        // either, and a survivor merely waiting for its author gets its ADDED from the replay.
        Optional<CommonPublicChatMessage> survivor = survivorSharingId(message);
        if (survivor.isPresent()) {
            push(survivor.get(), ModificationType.ADDED);
            return;
        }
        push(message, ModificationType.REMOVED);
    }

    /**
     * A message still live in the channel under the removed one's id. The wire keys messages by their
     * id, and a client can only interpret {@code REMOVED} as delete-by-id — so when the id is still
     * live, a {@code REMOVED} would take down what the client shows under it with nothing to bring it
     * back until a reconnect. That happens two ways: the P2P store re-delivering a message as a fresh
     * equal instance whose predecessor's removal is still in flight, and a peer deliberately publishing
     * a different message under a live id (nothing prevents that, see
     * {@code PublicChatReactionsWebSocketService.BindingKey}). Re-pushing the survivor as {@code ADDED}
     * instead converges the client on what a fresh snapshot would show; for the equal re-delivery it is
     * an idempotent upsert. The newest visible survivor is chosen so several colliders converge the
     * same way the snapshot's ordering would resolve them — and visibility filters the candidates
     * before newest wins, as {@link PublicChatDtoFactory#visibleMessagesNewestFirst} filters before it
     * sorts. The other order hands the id to whoever plants the newest collider: an invisible one
     * (unresolvable author, far-future date) would win the max, fail the visibility check and turn
     * every removal under that id into a {@code REMOVED} that takes down the visible message a fresh
     * snapshot still shows.
     */
    private Optional<CommonPublicChatMessage> survivorSharingId(CommonPublicChatMessage removed) {
        return channels.findChannel(removed.getChannelId())
                .stream()
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(live -> live.getId().equals(removed.getId()))
                .filter(dtoFactory::isVisible)
                .max(Comparator.comparingLong(CommonPublicChatMessage::getDate));
    }

    /**
     * A message that leaves its channel while parked is not coming back; equality drops the parked
     * copy. Equality is enough because {@code ChatChannelService} serializes every add and remove —
     * observer callbacks included — under the store monitor: a re-delivered equal successor's park
     * cannot interleave with this drop, so the dropped entry is never a live successor's.
     */
    private void dropParked(CommonPublicChatMessage message) {
        awaitingAuthor.computeIfPresent(message.getAuthorUserProfileId(), (id, present) -> {
            present.remove(message);
            return present.isEmpty() ? null : present;
        });
    }

    /** Test seam: the no-leak claim on {@link #awaitingAuthor} is about its keys, so the tests count them. */
    int parkedAuthorKeys() {
        return awaitingAuthor.size();
    }

    private void push(CommonPublicChatMessage message, ModificationType modificationType) {
        List<Subscriber> subscribers = findSubscribers(message.getChannelId());
        if (subscribers.isEmpty()) {
            return;
        }
        Optional<CommonPublicChatMessageDto> dto = dtoFactory.findDto(message);
        if (dto.isEmpty()) {
            return;
        }
        // The payload is defined as a list to support batch data delivery at subscribe.
        toJson(Collections.singletonList(dto.get())).ifPresent(json ->
                subscribers.forEach(subscriber -> send(json, subscriber, modificationType)));
    }
}
