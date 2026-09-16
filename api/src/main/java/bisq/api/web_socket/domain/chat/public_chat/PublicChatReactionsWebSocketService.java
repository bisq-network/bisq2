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
import bisq.api.dto.chat.common.CommonPublicChatMessageReactionDto;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.api.web_socket.subscription.Topic.PUBLIC_CHAT_REACTIONS;

/**
 * Pushes the reactions on the public chat channels' messages. A public reaction is removed from the
 * network for real, so it is {@code ADDED} and {@code REMOVED} directly; there is no removal marker as
 * in private chat.
 * <p>
 * Structurally {@code ChatReactionsWebSocketService}, plus the one thing public messages add: when a
 * message leaves its channel, its reaction observer is unbound. Neither trade nor private chat needs
 * that because their messages never go; here, with a 10-day TTL on channels of thousands of messages,
 * keeping the pins would be a leak. No {@code REMOVED} is pushed for the reactions of a removed message:
 * the client drops them together with the message.
 * <p>
 * The unbind rides on the removal actually being processed, so it inherits the limitation
 * {@code PublicChatMessagesWebSocketService#handleRemovedMessage} describes: a message the domain
 * refuses to remove — its author banned or rate limited at that moment — keeps both its place in the
 * channel and its binding here, for the life of the process. Bounded by how many authors are in that
 * state, not by the message count, so it is a stale entry rather than the leak the unbind exists for.
 */
@Slf4j
public class PublicChatReactionsWebSocketService extends ChannelScopedWebSocketService {
    /**
     * Upper bound on the parked reactions, see {@link #awaitingProfile}. Far above anything the honest
     * case produces — parking needs a reaction whose message author or sender has no profile yet,
     * which is a startup-window state — so hitting it means something is flooding reactions with
     * unresolvable profiles, and degrading those to the snapshot-only behavior is the right answer.
     */
    private static final int MAX_PARKED_REACTIONS = 10_000;

    private final UserProfileService userProfileService;
    private final List<Pin> messagesPins = new ArrayList<>();
    private Pin profilesPin;
    private final Map<BindingKey, List<MessageBinding>> bindingsByMessage = new ConcurrentHashMap<>();
    /**
     * The reaction additions that could not be pushed because a profile had not arrived yet — the
     * message author's or the reaction sender's, keyed by whichever id they wait for (see
     * {@link PublicChatDtoFactory#awaitedProfileId}) — the counterpart of
     * {@code PublicChatMessagesWebSocketService#awaitingAuthor}, with one difference in retention: a
     * message that leaves its channel takes no per-reaction removals with it, so parked entries are
     * also dropped when {@link #unbindMessage} sees their message go, {@link #MAX_PARKED_REACTIONS}
     * bounds what a flood of unresolvable profiles can pile up, and a drained key is unlinked where it
     * empties ({@link #pruneIfEmpty}). Sets claimed exclusively, see
     * {@code PublicChatMessagesWebSocketService#park}.
     */
    private final Map<String, Set<PendingReaction>> awaitingProfile = new ConcurrentHashMap<>();
    private final AtomicInteger parkedCount = new AtomicInteger();
    private final AtomicBoolean parkedCapWarned = new AtomicBoolean();
    private volatile boolean shutdownStarted;

    public PublicChatReactionsWebSocketService(SubscriberRepository subscriberRepository,
                                              PublicChatChannels channels,
                                              UserProfileService userProfileService,
                                              BannedUserService bannedUserService) {
        super(subscriberRepository, PUBLIC_CHAT_REACTIONS, channels, userProfileService, bannedUserService);
        this.userProfileService = userProfileService;
    }

    /** The channel set is fixed before this service starts, see PublicChatMessagesWebSocketService. */
    @Override
    public CompletableFuture<Boolean> initialize() {
        // Before the channel observers, whose registration replays the history — see
        // PublicChatMessagesWebSocketService#initialize.
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
                        bindMessage(message);
                    }

                    @Override
                    public void onRemoved(Object element) {
                        if (element instanceof CommonPublicChatMessage message) {
                            unbindMessage(message);
                        }
                    }

                    @Override
                    public void onCleared() {
                        // Never happens, see PublicChatMessagesWebSocketService.
                    }
                })));
        return CompletableFuture.completedFuture(true);
    }

    private void bindMessage(CommonPublicChatMessage message) {
        // Registered before the map is touched: addObserver replays the reactions already on the message
        // and each replayed callback reaches send, which must not run under a ConcurrentHashMap bin lock.
        // The set is declared over the base reaction type, see PublicChatDtoFactory#reactionsOf.
        Pin reactionsPin = message.getChatMessageReactions().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(ChatMessageReaction reaction) {
                if (reaction instanceof CommonPublicChatMessageReaction publicReaction) {
                    handleAddedReaction(message, publicReaction);
                }
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof CommonPublicChatMessageReaction reaction) {
                    handleRemovedReaction(reaction);
                }
            }

            @Override
            public void onCleared() {
                // Never happens: reactions are only removed one by one.
            }
        });
        MessageBinding binding = new MessageBinding(message, reactionsPin);
        if (!install(binding)) {
            reactionsPin.unbind();
        } else if (shutdownStarted) {
            // Read again after publishing, so a shutdown that swept before this entry existed does not
            // leave it behind: the sweep runs after shutdownStarted is set, so either this read sees the
            // flag and takes the entry out here, or the entry was already there for the sweep to find.
            // Not through unbindMessage: the message is still live in its channel — it is the service
            // that is going — so this takes out the exact binding, and whichever of sweep and this wins
            // the claim is the one that unbinds the pin.
            if (claim(binding)) {
                reactionsPin.unbind();
            }
        }
    }

    /**
     * Publishes the binding, but only while its message is still the live one, and the check runs
     * inside the map update so that it cannot go stale between the two. The entry holds a list, not a
     * single binding: {@code ChatMessage.verify} bounds the id's length and nothing else, so a peer can
     * publish a <em>different</em> message under an id that is live in the same channel, and both stay
     * in the channel side by side — displacing one binding with the other would unbind the observer of
     * a message a peer never touched, for the life of the process. What can be swept here is any
     * co-keyed binding whose owner is no longer live, which is how the entry of a re-delivered message
     * — a fresh instance equal to the one it replaced — gets taken out.
     * <p>
     * One binding per owner instance: {@code ObservableCollection#addObserver} registers the observer
     * before it replays the collection, so an addition racing the registration in
     * {@link #initialize} can arrive through both the live callback and the replay, and keeping both
     * bindings would push every later reaction of that message twice for the life of the process.
     *
     * @return false if the message is gone, already bound or the shutdown has started, leaving the pin
     * for the caller to unbind
     */
    private boolean install(MessageBinding binding) {
        List<MessageBinding> dead = new ArrayList<>();
        AtomicBoolean installed = new AtomicBoolean();
        bindingsByMessage.compute(keyOf(binding.owner()), (key, present) -> {
            List<MessageBinding> live = new ArrayList<>();
            if (present != null) {
                present.forEach(each -> (isLive(each.owner()) ? live : dead).add(each));
            }
            boolean alreadyBound = live.stream().anyMatch(each -> each.owner() == binding.owner());
            if (!shutdownStarted && !alreadyBound && isLive(binding.owner())) {
                live.add(binding);
                installed.set(true);
            }
            return live.isEmpty() ? null : List.copyOf(live);
        });
        dead.forEach(each -> each.reactionsPin().unbind());
        return installed.get();
    }

    /** Takes this exact binding out of its entry, by identity: its pin exists once. */
    private boolean claim(MessageBinding binding) {
        AtomicBoolean claimed = new AtomicBoolean();
        bindingsByMessage.computeIfPresent(keyOf(binding.owner()), (key, present) -> {
            List<MessageBinding> rest = new ArrayList<>(present);
            if (rest.removeIf(each -> each == binding)) {
                claimed.set(true);
            }
            return rest.isEmpty() ? null : List.copyOf(rest);
        });
        return claimed.get();
    }

    /**
     * Unbinds the reaction observers of the bindings under this message's key whose owner is no longer
     * live. The teardown cannot key on the notified instance: {@code ObservableCollection#remove} drops
     * the stored element by {@code equals} but notifies with the argument, and on a restarted node the
     * removal path always delivers the network store's copy rather than the channel store's instance
     * that was bound. Nor can it take the whole entry: the P2P store re-delivers a message as a fresh
     * instance that is equal to the one it replaced, so a successor can be bound under the same channel
     * and id while this callback is still pending, and a colliding message that shares the id keeps its
     * own live binding in the same entry — only the bindings whose owner is gone go.
     */
    private void unbindMessage(CommonPublicChatMessage message) {
        List<MessageBinding> dead = new ArrayList<>();
        bindingsByMessage.computeIfPresent(keyOf(message), (key, present) -> {
            List<MessageBinding> live = new ArrayList<>();
            present.forEach(each -> (isLive(each.owner()) ? live : dead).add(each));
            return live.isEmpty() ? null : List.copyOf(live);
        });
        dead.forEach(each -> {
            each.reactionsPin().unbind();
            dropParkedFor(each.owner());
        });
    }

    /**
     * Reference identity rather than {@code contains}, which compares by {@code equals}: a chat message
     * is equal on all its value fields, so a re-delivered copy is equal to the instance it replaced.
     * Asking by equality would answer for the successor instead of for this instance.
     */
    private boolean isLive(CommonPublicChatMessage message) {
        return channels.findChannel(message.getChannelId())
                .map(channel -> channel.getChatMessages().stream().anyMatch(live -> live == message))
                .orElse(false);
    }

    /**
     * A message's reaction observer together with the instance it was registered for. The owner is what
     * lets {@link #install} and {@link #unbindMessage} ask {@link #isLive} whether this binding still
     * belongs to a message in its channel, or to one a newer copy displaced under the same id.
     */
    private record MessageBinding(CommonPublicChatMessage owner, Pin reactionsPin) {
    }

    /**
     * What a binding is filed under. The channel belongs in the key because a message id is not unique
     * across channels: {@code ChatMessage.verify} bounds its length and nothing else, so a peer
     * publishing to one channel can carry an id that is already in use in another. Within a channel the
     * id is not unique either — that is what the entry being a list is for, see {@link #install}. The
     * private chat sibling keeps the same separation by nesting its reaction pins inside a per-channel
     * entry.
     */
    private record BindingKey(String channelId, String messageId) {
    }

    private static BindingKey keyOf(CommonPublicChatMessage message) {
        return new BindingKey(message.getChannelId(), message.getId());
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
        awaitingProfile.clear();
        // Swept key by key rather than snapshot-then-clear, so an entry published between the two is
        // never dropped with its observer still registered.
        new ArrayList<>(bindingsByMessage.keySet()).forEach(key -> {
            List<MessageBinding> bindings = bindingsByMessage.remove(key);
            if (bindings != null) {
                bindings.forEach(binding -> binding.reactionsPin().unbind());
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(Optional.empty());
    }

    @Override
    public Optional<String> getJsonPayload(Optional<String> parameter) {
        List<CommonPublicChatMessageReactionDto> payload = channelsOf(parameter)
                .flatMap(channel -> channel.getChatMessages().stream())
                .flatMap(message -> PublicChatDtoFactory.reactionsOf(message)
                        .filter(reaction -> dtoFactory.isVisible(message, reaction))
                        .map(dtoFactory::findDto)
                        .flatMap(Optional::stream))
                .toList();
        return toJson(payload);
    }

    private void handleAddedReaction(CommonPublicChatMessage message, CommonPublicChatMessageReaction reaction) {
        if (shutdownStarted) {
            return;
        }
        // One read of each profile classifies push against park — the same lost wakeup as in
        // PublicChatMessagesWebSocketService#handleAddedMessage otherwise.
        Optional<UserProfile> author = dtoFactory.findProfile(message.getAuthorUserProfileId());
        Optional<UserProfile> sender = dtoFactory.findProfile(reaction.getUserProfileId());
        if (dtoFactory.isVisible(message, reaction, author, sender)) {
            push(reaction, ModificationType.ADDED);
        } else {
            dtoFactory.awaitedProfileId(message, reaction, author, sender)
                    .ifPresent(profileId -> park(profileId, message, reaction));
        }
    }

    /**
     * The claim-after-parking double check, as in {@code PublicChatMessagesWebSocketService#park} —
     * including the add running inside the map update so it serializes with {@link #pruneIfEmpty}, and
     * the claim staying on the set instance; the reasons live on that method.
     */
    private void park(String awaitedProfileId, CommonPublicChatMessage message, CommonPublicChatMessageReaction reaction) {
        if (parkedCount.get() >= MAX_PARKED_REACTIONS) {
            if (parkedCapWarned.compareAndSet(false, true)) {
                log.warn("Parked reaction cap of {} reached; further reactions with unresolvable profiles " +
                        "will only be delivered in snapshots", MAX_PARKED_REACTIONS);
            }
            return;
        }
        PendingReaction pending = new PendingReaction(message, reaction);
        AtomicReference<Set<PendingReaction>> ref = new AtomicReference<>();
        awaitingProfile.compute(awaitedProfileId, (id, present) -> {
            Set<PendingReaction> set = present != null ? present : ConcurrentHashMap.newKeySet();
            if (set.add(pending)) {
                parkedCount.incrementAndGet();
            }
            ref.set(set);
            return set;
        });
        Set<PendingReaction> parked = ref.get();
        if (dtoFactory.isVisible(message, reaction) && claim(parked, pending)) {
            pruneIfEmpty(awaitedProfileId, parked);
            push(reaction, ModificationType.ADDED);
        }
    }

    /**
     * Takes an emptied set's key out of the map — author and sender ids are equally free to fabricate,
     * so without this every drained key would be a leak the {@link #MAX_PARKED_REACTIONS} cap does not
     * see (it counts entries, not keys). Same locking argument as
     * {@code PublicChatMessagesWebSocketService#pruneIfEmpty}.
     */
    private void pruneIfEmpty(String profileId, Set<PendingReaction> parked) {
        if (parked.isEmpty()) {
            awaitingProfile.computeIfPresent(profileId, (id, present) ->
                    present == parked && present.isEmpty() ? null : present);
        }
    }

    /**
     * See {@code PublicChatMessagesWebSocketService#replayFor} for why racing an orphaned set is fine.
     * A claimed reaction still short of the <em>other</em> profile — parked for the message author with
     * the sender also missing, or the author pruned while it waited for the sender — is parked again
     * under the id it now waits for; the arrival of that one replays it from there.
     */
    private void replayFor(String profileId) {
        Set<PendingReaction> parked = awaitingProfile.remove(profileId);
        if (parked == null) {
            return;
        }
        parked.forEach(pending -> {
            if (shutdownStarted || !claim(parked, pending) || !isStillInChannel(pending.message())) {
                return;
            }
            Optional<UserProfile> author = dtoFactory.findProfile(pending.message().getAuthorUserProfileId());
            Optional<UserProfile> sender = dtoFactory.findProfile(pending.reaction().getUserProfileId());
            if (dtoFactory.isVisible(pending.message(), pending.reaction(), author, sender)) {
                push(pending.reaction(), ModificationType.ADDED);
            } else {
                dtoFactory.awaitedProfileId(pending.message(), pending.reaction(), author, sender)
                        .ifPresent(id -> park(id, pending.message(), pending.reaction()));
            }
        });
    }

    private boolean claim(Set<PendingReaction> parked, PendingReaction pending) {
        if (parked.remove(pending)) {
            parkedCount.decrementAndGet();
            return true;
        }
        return false;
    }

    /** By {@code equals} on purpose, see {@code PublicChatMessagesWebSocketService#isStillInChannel}. */
    private boolean isStillInChannel(CommonPublicChatMessage message) {
        return channels.findChannel(message.getChannelId())
                .map(channel -> channel.getChatMessages().contains(message))
                .orElse(false);
    }

    /** A parked reaction whose message left its channel is not coming back; the sweep keeps the map honest. */
    private void dropParkedFor(CommonPublicChatMessage message) {
        awaitingProfile.forEach((profileId, parked) -> {
            parked.forEach(pending -> {
                if (pending.message().equals(message)) {
                    claim(parked, pending);
                }
            });
            pruneIfEmpty(profileId, parked);
        });
    }

    /** An addition waiting for a profile, with the message it sits on for the re-checks at replay. */
    private record PendingReaction(CommonPublicChatMessage message, CommonPublicChatMessageReaction reaction) {
    }

    /**
     * Not filtered by visibility, unlike the added path. {@code ChatChannelService#addMessageReaction}
     * gates on the ban but {@code #removeMessageReaction} does not, so a removal does reach here for a
     * sender banned after the reaction was pushed — which is exactly the one that has to be taken back.
     * A removal for a reaction that was never pushed is a no-op on the client, which deletes by id.
     * The same goes for a subscriber that connected after the ban and never saw the reaction in its
     * snapshot: the sender's profile rides along in the payload, but {@code GET /user-profiles} already
     * serves it to that client without filtering banned users.
     * <p>
     * {@code push} narrows this again, and that is a known limitation: it maps through
     * {@code findDto}, which needs the sender's profile, so a sender pruned from the profile store
     * after the reaction was pushed produces no {@code REMOVED} at all and the client keeps an orphan
     * until it reconnects. Carrying the removal without the profile would mean a dto that admits an
     * absent author, which is a wider contract change than the window is worth.
     */
    private void handleRemovedReaction(CommonPublicChatMessageReaction reaction) {
        if (shutdownStarted) {
            return;
        }
        dropParked(reaction);
        // Reaction ids are as much the peer's to choose as message ids, and the wire keys a reaction by
        // its id within its message — so a REMOVED while another reaction is live under the same id
        // would make the client delete what a fresh snapshot still shows. The same convergence as
        // PublicChatMessagesWebSocketService#survivorSharingId: re-push the newest visible survivor.
        Optional<CommonPublicChatMessageReaction> survivor = survivorSharingId(reaction);
        if (survivor.isPresent()) {
            push(survivor.get(), ModificationType.ADDED);
            return;
        }
        push(reaction, ModificationType.REMOVED);
    }

    /**
     * A reaction still live under the removed one's id, on a message live under its message id in the
     * removed one's channel — the coordinates the client files a reaction under. Visibility filters the
     * candidates before newest wins, for the reason on the messages twin.
     */
    private Optional<CommonPublicChatMessageReaction> survivorSharingId(CommonPublicChatMessageReaction removed) {
        return channels.findChannel(removed.getChatChannelId())
                .stream()
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(message -> message.getId().equals(removed.getChatMessageId()))
                .flatMap(message -> PublicChatDtoFactory.reactionsOf(message)
                        .filter(live -> live.getId().equals(removed.getId()))
                        .filter(live -> dtoFactory.isVisible(message, live)))
                .max(Comparator.comparingLong(CommonPublicChatMessageReaction::getDate));
    }

    /**
     * A reaction removed while parked was never pushed, so there is nothing left to wait for. Parked
     * under whichever profile was missing, so every key is swept, as in {@link #dropParkedFor}.
     */
    private void dropParked(CommonPublicChatMessageReaction reaction) {
        awaitingProfile.forEach((profileId, parked) -> {
            parked.forEach(pending -> {
                if (pending.reaction().equals(reaction)) {
                    claim(parked, pending);
                }
            });
            pruneIfEmpty(profileId, parked);
        });
    }

    /** Test seam: the no-leak claim on {@link #awaitingProfile} is about its keys, so the tests count them. */
    int parkedProfileKeys() {
        return awaitingProfile.size();
    }

    private void push(CommonPublicChatMessageReaction reaction, ModificationType modificationType) {
        List<Subscriber> subscribers = findSubscribers(reaction.getChatChannelId());
        if (subscribers.isEmpty()) {
            return;
        }
        Optional<CommonPublicChatMessageReactionDto> dto = dtoFactory.findDto(reaction);
        if (dto.isEmpty()) {
            return;
        }
        // The payload is defined as a list to support batch data delivery at subscribe.
        toJson(Collections.singletonList(dto.get())).ifPresent(json ->
                subscribers.forEach(subscriber -> send(json, subscriber, modificationType)));
    }
}
