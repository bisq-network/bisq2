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

package bisq.chat.priv;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelService;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.reactions.PrivateChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.StartupReplay;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.PersistableStore;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class PrivateChatChannelService<
        R extends PrivateChatMessageReaction,
        M extends PrivateChatMessage<R>,
        C extends PrivateChatChannel<M>,
        S extends PersistableStore<S>
        > extends ChatChannelService<M, C, S> implements ConfidentialMessageService.Listener {
    // In arrival order, bounded (see docs/specifications/chat/pending-reactions.md). Access is synchronized on the set.
    @VisibleForTesting
    public static final int MAX_PENDING_REACTIONS = 100;
    private final Set<R> unprocessedReactions = new LinkedHashSet<>();

    public PrivateChatChannelService(NetworkService networkService,
                                     UserService userService,
                                     ChatChannelDomain chatChannelDomain) {
        super(networkService, userService, chatChannelDomain);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        StartupReplay.forEach(networkService.getConfidentialMessageServices().stream()
                        .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream()),
                this::onMessage);
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        synchronized (unprocessedReactions) {
            unprocessedReactions.clear();
        }
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    protected CompletableFuture<SendMessageResult> sendMessage(String messageId,
                                                               @Nullable String text,
                                                               Optional<Citation> citation,
                                                               C channel,
                                                               UserProfile receiver,
                                                               ChatMessageType chatMessageType,
                                                               long date) {
        return trySendMessage(messageId, text, citation, channel, receiver, chatMessageType, date).getDelivery();
    }

    /**
     * Sends, and reports what it decided locally rather than only what it started. Callers that must
     * answer for the send synchronously use this; {@link #sendMessage} is the same thing for those that
     * only care about delivery.
     * <p>
     * A rejection means the caller's own message is neither stored nor sent, but it is not free of side
     * effects: {@link SendRejection#PEER_BANNED} adds a system message to the channel telling the user why,
     * de-duplicated by {@link #alreadyNotifiedAboutBannedPeer}. That message is local, never reaches the
     * peer, and raises no notification, but it does reach anything observing the channel. A caller that
     * reports the rejection onwards has to say so too.
     */
    protected SendOutcome trySendMessage(String messageId,
                                         @Nullable String text,
                                         Optional<Citation> citation,
                                         C channel,
                                         UserProfile receiver,
                                         ChatMessageType chatMessageType,
                                         long date) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        Optional<SendRejection> rejection = findSendRejection(channel, receiver);
        if (rejection.isPresent()) {
            if (rejection.get() == SendRejection.PEER_BANNED) {
                addPeerBannedSystemMessage(channel, myUserIdentity, receiver, date);
            }
            return SendOutcome.rejected(rejection.get());
        }

        M chatMessage = createAndGetNewPrivateChatMessage(messageId,
                channel,
                myUserIdentity.getUserProfile(),
                receiver,
                text,
                citation,
                date,
                false,
                chatMessageType);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        return SendOutcome.accepted(
                networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair));
    }

    /**
     * Why a send to this receiver would be refused before anything is stored, if it would. Private on
     * purpose: asking it from outside and then sending is two decisions where there should be one, and
     * only the send's own answer is authoritative. Callers that need that answer read it off the
     * {@link SendOutcome} the send returns.
     */
    private Optional<SendRejection> findSendRejection(C channel, UserProfile receiver) {
        if (bannedUserService.isUserProfileBanned(channel.getMyUserIdentity().getUserProfile())) {
            return Optional.of(SendRejection.MY_PROFILE_BANNED);
        }
        if (isPeerBanned(receiver)) {
            return Optional.of(SendRejection.PEER_BANNED);
        }
        return Optional.empty();
    }

    protected boolean isPeerBanned(UserProfile userProfile) {
        return bannedUserService.isUserProfileBanned(userProfile);
    }

    @VisibleForTesting
    static final String PEER_BANNED_NOTICE_KEY = "chat.privateChannel.peerBanned";

    private void addPeerBannedSystemMessage(C channel, UserIdentity myUserIdentity, UserProfile receiver, long date) {
        if (alreadyNotifiedAboutBannedPeer(channel.getChatMessages(), receiver.getUserName())) {
            return;
        }
        UserProfile nonBannedAuthor = myUserIdentity.getUserProfile();
        M systemMessage = createAndGetNewPrivateChatMessage(StringUtils.createUid(),
                channel,
                nonBannedAuthor,
                receiver,
                Res.encode(PEER_BANNED_NOTICE_KEY, receiver.getUserName()),
                Optional.empty(),
                date,
                false,
                ChatMessageType.PROTOCOL_LOG_MESSAGE);
        addMessage(systemMessage, channel);
    }

    @VisibleForTesting
    static boolean alreadyNotifiedAboutBannedPeer(Collection<? extends ChatMessage> messages, String peerName) {
        String peerNotice = Res.encode(PEER_BANNED_NOTICE_KEY, peerName);
        return messages.stream()
                .sorted(Comparator.comparingLong(ChatMessage::getDate).reversed())
                .takeWhile(PrivateChatChannelService::isPeerBannedNotice)
                .anyMatch(message -> message.getText().map(peerNotice::equals).orElse(false));
    }

    private static boolean isPeerBannedNotice(ChatMessage message) {
        return message.getChatMessageType() == ChatMessageType.PROTOCOL_LOG_MESSAGE
                && message.getText().map(text -> text.startsWith(PEER_BANNED_NOTICE_KEY)).orElse(false);
    }

    public void leaveChannel(String id) {
        findChannel(id).ifPresent(this::leaveChannel);
    }

    public void leaveChannel(C channel) {
        synchronized (this) {
            getChannels().remove(channel);
        }
        persist();
    }

    @Override
    public Optional<C> getDefaultChannel() {
        return Optional.empty();
    }

    protected CompletableFuture<SendMessageResult> sendLeaveMessage(C channel, UserProfile receiver, long date) {
        String encoded = Res.encode("chat.privateChannel.message.leave", channel.getMyUserIdentity().getUserProfile().getUserName());
        return sendMessage(StringUtils.createUid(),
                encoded,
                Optional.empty(),
                channel,
                receiver,
                ChatMessageType.LEAVE,
                date);
    }

    @Override
    public String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel) {
        checkArgument(chatChannel instanceof PrivateChatChannel,
                "chatChannel at PrivateChatChannelService.getChannelTitlePostFix must be of type PrivateChatChannel");
        return userIdentityService.hasMultipleUserIdentities() ? "" :
                " [" + ((PrivateChatChannel<?>) chatChannel).getMyUserIdentity().getUserName() + "]";
    }

    protected CompletableFuture<SendMessageResult> sendMessageReaction(M message,
                                                                       C chatChannel,
                                                                       UserProfile receiver,
                                                                       Reaction reaction,
                                                                       String messageReactionId,
                                                                       boolean isRemoved) {
        return trySendMessageReaction(message, chatChannel, receiver, reaction, messageReactionId, isRemoved)
                .getDelivery();
    }

    /**
     * The reaction counterpart of {@link #trySendMessage}, refused on the same grounds. It adds no notice on
     * a banned peer: a reaction is not a statement the user is waiting on an answer for, and a notice per
     * refused emoji would bury the conversation. A rejection here leaves the channel untouched.
     */
    protected SendOutcome trySendMessageReaction(M message,
                                                 C chatChannel,
                                                 UserProfile receiver,
                                                 Reaction reaction,
                                                 String messageReactionId,
                                                 boolean isRemoved) {
        UserIdentity myUserIdentity = chatChannel.getMyUserIdentity();
        Optional<SendRejection> rejection = findSendRejection(chatChannel, receiver);
        if (rejection.isPresent()) {
            return SendOutcome.rejected(rejection.get());
        }

        R chatMessageReaction = createAndGetNewPrivateChatMessageReaction(message, myUserIdentity.getUserProfile(),
                receiver, reaction, messageReactionId, isRemoved);

        addMessageReaction(chatMessageReaction, message);

        NetworkId receiverNetworkId = receiver.getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        return SendOutcome.accepted(
                networkService.confidentialSend(chatMessageReaction, receiverNetworkId, senderNetworkIdWithKeyPair));
    }


    /* --------------------------------------------------------------------- */
    // Protected
    /* --------------------------------------------------------------------- */

    protected void processMessage(M message) {
        if (!canProcessMessage(message)) {
            return;
        }
        Optional<C> existingChannel = findChannel(message);
        if (existingChannel.isPresent()) {
            // A message for an existing channel never creates another channel, also when it is rejected
            C channel = existingChannel.get();
            if (isReceivedMessageOfChannel(channel, message)) {
                addMessageAndProcessQueuedReactions(message, channel);
            }
            return;
        }
        // We prevent to send leave messages after a peer has left, but there might be still
        // race conditions where that might happen, so we check at receiving the message as well, so that
        // in cases we would get a leave message as first message (e.g. after having closed the channel)
        // we do not create a channel.
        if (message.getChatMessageType() == ChatMessageType.LEAVE) {
            log.warn("We received a leave message for a non existing channel. This can happen if the peer " +
                    "sent a leave message around the same time as we have closed the channel.");
            return;
        }
        // The open trade services may return an existing channel of the trade ID in the message, so the check applies
        // here as well. A new channel has the sender as peer and the receiver as our identity.
        createNewChannelFromReceivedMessage(message)
                .filter(channel -> isReceivedMessageOfChannel(channel, message))
                .ifPresent(channel -> addMessageAndProcessQueuedReactions(message, channel));
    }

    // A received message is added to a channel only if it was sent to the channel by a peer and belongs to the channel
    // (docs/specifications/chat/private-channel-membership.md)
    protected boolean isReceivedMessageOfChannel(C channel, M message) {
        return isSentToChannel(channel, message.getSenderUserProfile().getId(), message.getReceiverUserProfileId()) &&
                isMessageOfChannel(channel, message);
    }

    // Whether a received message belongs to the channel apart from its peers, for example to the trade of a trade
    // channel. The sender chooses the IDs in the message.
    protected boolean isMessageOfChannel(C channel, M message) {
        return true;
    }

    // The sender chooses the channel ID. A received message or reaction belongs to an existing channel only if its
    // sender is a peer of the channel and its receiver is our identity in the channel. Otherwise anybody who learned the
    // ID of a channel, for example the trade ID, could add messages to a conversation of other users.
    private boolean isSentToChannel(C channel, String senderUserProfileId, String receiverUserProfileId) {
        boolean isSentToChannel = channel.isPeer(senderUserProfileId) &&
                channel.getMyUserIdentity().getId().equals(receiverUserProfileId);
        if (!isSentToChannel) {
            log.warn("Ignoring a message or reaction for channel {} from a sender which is not a peer of the channel, " +
                    "or to another receiver", channel.getId());
        }
        return isSentToChannel;
    }

    protected boolean canProcessMessage(M message) {
        return canHandleChannelDomain(message) && isValid(message);
    }

    protected abstract Optional<C> createNewChannelFromReceivedMessage(M message);

    protected void addMessageAndProcessQueuedReactions(M message, C channel) {
        addMessage(message, channel);
        // Check if there are any reactions that should be added to existing messages
        processQueuedReactions();
    }

    protected void processMessageReaction(R messageReaction) {
        if (applyMessageReaction(messageReaction)) {
            return;
        }
        addPendingReaction(messageReaction);
        // Another thread can have added the message after the first attempt, without seeing the pending reaction
        if (applyMessageReaction(messageReaction)) {
            removePendingReaction(messageReaction);
        }
    }

    // Returns false only if the channel or the message is missing; a reaction of a non-peer is ignored (true)
    private boolean applyMessageReaction(R messageReaction) {
        Optional<C> channel = findChannel(messageReaction.getChatChannelId());
        if (channel.isPresent() && !isSentToChannel(channel.get(), messageReaction.getSenderUserProfile().getId(),
                messageReaction.getReceiverUserProfileId())) {
            return true;
        }
        Optional<M> message = channel.flatMap(foundChannel -> foundChannel.getChatMessages().stream()
                .filter(chatMessage -> chatMessage.getId().equals(messageReaction.getChatMessageId()))
                .findFirst());
        message.ifPresent(chatMessage -> addMessageReaction(messageReaction, chatMessage));
        return message.isPresent();
    }

    private void addPendingReaction(R messageReaction) {
        synchronized (unprocessedReactions) {
            if (unprocessedReactions.contains(messageReaction)) {
                return;
            }
            if (unprocessedReactions.size() >= MAX_PENDING_REACTIONS) {
                Iterator<R> iterator = unprocessedReactions.iterator();
                iterator.next();
                iterator.remove();
                log.warn("We drop the oldest pending reaction because the limit of {} pending reactions is reached",
                        MAX_PENDING_REACTIONS);
            }
            unprocessedReactions.add(messageReaction);
        }
    }

    private void removePendingReaction(R messageReaction) {
        synchronized (unprocessedReactions) {
            unprocessedReactions.remove(messageReaction);
        }
    }

    protected void processQueuedReactions() {
        List<R> pendingReactions;
        synchronized (unprocessedReactions) {
            pendingReactions = new ArrayList<>(unprocessedReactions);
        }
        long now = System.currentTimeMillis();
        pendingReactions.forEach(reaction -> {
            boolean isExpired = now - reaction.getDate() >= reaction.getMetaData().getTtl();
            if (isExpired || applyMessageReaction(reaction)) {
                removePendingReaction(reaction);
            }
        });
    }

    @Override
    protected boolean isValid(M message) {
        if (message.isMyMessage(userIdentityService)) {
            log.warn("Sent a message to myself. This should never happen for private chat messages.");
            return false;
        }
        if (bannedUserService.isUserProfileBanned(message.getSenderUserProfile())) {
            log.warn("Message invalid as sender is banned");
            return false;
        }
        return super.isValid(message);
    }

    protected abstract C createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity);

    protected abstract M createAndGetNewPrivateChatMessage(String messageId,
                                                           C channel,
                                                           UserProfile senderUserProfile,
                                                           UserProfile receiverUserProfile,
                                                           String text,
                                                           Optional<Citation> citation,
                                                           long time,
                                                           boolean wasEdited,
                                                           ChatMessageType chatMessageType);

    protected abstract R createAndGetNewPrivateChatMessageReaction(M message,
                                                                   UserProfile senderUserProfile,
                                                                   UserProfile receiverUserProfile,
                                                                   Reaction reaction,
                                                                   String messageReactionId,
                                                                   boolean isRemoved);

    @Override
    protected void checkRateLimit(String authorUserProfileId, long messageDate) {
        // For private messages we don't check, as user can ignore anyway the peer.
    }
}
