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
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
public class PublicChatReactionsWebSocketService extends ChannelScopedWebSocketService {
    private final List<Pin> messagesPins = new ArrayList<>();
    private final Map<BindingKey, MessageBinding> bindingsByMessage = new ConcurrentHashMap<>();
    private volatile boolean shutdownStarted;

    public PublicChatReactionsWebSocketService(SubscriberRepository subscriberRepository,
                                              PublicChatChannels channels,
                                              UserProfileService userProfileService,
                                              BannedUserService bannedUserService) {
        super(subscriberRepository, PUBLIC_CHAT_REACTIONS, channels, userProfileService, bannedUserService);
    }

    /** The channel set is fixed before this service starts, see PublicChatMessagesWebSocketService. */
    @Override
    public CompletableFuture<Boolean> initialize() {
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
            // that is going — so this keys on the exact binding, and whichever of sweep and this wins
            // the entry is the one that unbinds the pin.
            if (bindingsByMessage.remove(keyOf(message), binding)) {
                reactionsPin.unbind();
            }
        }
    }

    /**
     * Publishes the binding, but only while its message is still the live one under that key, and the
     * check runs inside the map update so that it cannot go stale between the two. Whatever it displaces
     * is a message of the same channel with the same id, so it is an instance that is gone and its
     * observer is unbound — see {@link BindingKey} for why the channel has to be in the key for that to
     * hold.
     *
     * @return false if the message is gone or the shutdown has started, leaving the pin for the caller
     * to unbind
     */
    private boolean install(MessageBinding binding) {
        AtomicReference<MessageBinding> displaced = new AtomicReference<>();
        MessageBinding current = bindingsByMessage.compute(keyOf(binding.owner()), (key, present) -> {
            if (shutdownStarted || !isLive(binding.owner())) {
                return present;
            }
            displaced.set(present);
            return binding;
        });
        MessageBinding stale = displaced.get();
        if (stale != null) {
            stale.reactionsPin().unbind();
        }
        return current == binding;
    }

    /**
     * Unbinds this message's reaction observer, unless the binding's owner is still live. The teardown
     * cannot key on the notified instance: {@code ObservableCollection#remove} drops the stored element
     * by {@code equals} but notifies with the argument, and on a restarted node the removal path always
     * delivers the network store's copy rather than the channel store's instance that was bound. Nor is
     * the entry's key enough on its own: the P2P store re-delivers a message as a fresh instance that is
     * equal to the one it replaced, so a successor can be bound under the same channel and id while this
     * callback is still pending — a live owner is that successor's, and its observer must stay.
     */
    private void unbindMessage(CommonPublicChatMessage message) {
        AtomicReference<MessageBinding> removed = new AtomicReference<>();
        bindingsByMessage.computeIfPresent(keyOf(message), (key, present) -> {
            if (isLive(present.owner())) {
                return present;
            }
            removed.set(present);
            return null;
        });
        MessageBinding binding = removed.get();
        if (binding != null) {
            binding.reactionsPin().unbind();
        }
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
     * publishing to one channel can carry an id that is already in use in another. Under the id alone
     * that message installs over the other channel's binding, and since {@link #install} unbinds
     * whatever it displaces, the reactions on a message a peer never touched stop being observed for
     * the life of the process. The private chat sibling keeps the same separation by nesting its
     * reaction pins inside a per-channel entry.
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
        // Swept key by key rather than snapshot-then-clear, so an entry published between the two is
        // never dropped with its observer still registered.
        new ArrayList<>(bindingsByMessage.keySet()).forEach(key -> {
            MessageBinding binding = bindingsByMessage.remove(key);
            if (binding != null) {
                binding.reactionsPin().unbind();
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
        if (shutdownStarted || !dtoFactory.isVisible(message, reaction)) {
            return;
        }
        push(reaction, ModificationType.ADDED);
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
        push(reaction, ModificationType.REMOVED);
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
