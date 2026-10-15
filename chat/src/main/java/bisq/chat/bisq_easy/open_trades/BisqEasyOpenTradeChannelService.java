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

package bisq.chat.bisq_easy.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.priv.PendingTradeChatMessages;
import bisq.chat.priv.PrivateGroupChatChannelService;
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyOpenTradeChannelService extends PrivateGroupChatChannelService<BisqEasyOpenTradeMessageReaction,
        BisqEasyOpenTradeMessage, BisqEasyOpenTradeChannel, BisqEasyOpenTradeChannelStore> {

    @Getter
    private final BisqEasyOpenTradeChannelStore persistableStore = new BisqEasyOpenTradeChannelStore();
    @Getter
    private final Persistence<BisqEasyOpenTradeChannelStore> persistence;
    private final PendingTradeChatMessages<BisqEasyOpenTradeMessage> pendingMessages =
            new PendingTradeChatMessages<>(BisqEasyOpenTradeMessage::getTradeId);
    // Set by the trade service: whether we have a trade with the trade ID whose peer is the sender and whose own user
    // profile is the receiver of a message. A received message creates a channel only for such a trade (see rule 6 of
    // pending-trade-messages.md). Until it is set, received messages create no channel.
    private volatile TradeWithPeerPredicate isTradeWithPeer = (tradeId, senderUserProfileId, receiverUserProfileId) -> false;

    public BisqEasyOpenTradeChannelService(PersistenceService persistenceService,
                                           NetworkService networkService,
                                           UserService userService) {
        super(networkService, userService, ChatChannelDomain.BISQ_EASY_OPEN_TRADES);

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof BisqEasyOpenTradeMessage) {
            processMessage((BisqEasyOpenTradeMessage) envelopePayloadMessage);
            if (!pendingMessages.isEmpty()) {
                log.info("Processing pendingMessages messages");
                pendingMessages.getMessagesInArrivalOrder().forEach(this::processMessage);
            }
        } else if (envelopePayloadMessage instanceof BisqEasyOpenTradeMessageReaction) {
            processMessageReaction((BisqEasyOpenTradeMessageReaction) envelopePayloadMessage);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    // The verified mediation request is the authority for the mediator's channel. Anybody who knows the trade ID can
    // send the mediator a chat message with an offer before the request, and that creates a channel with the sender as
    // the only trader. Such a channel would hide the case and drop the messages of the other trader, so it is replaced.
    // Its messages are dropped: honest traders send nothing to the mediator before the request, and the request
    // carries the chat of the trade. Other channels of the trade are kept (see chat-channel-binding.md).
    public BisqEasyOpenTradeChannel mediatorFindOrCreatesChannel(String tradeId,
                                                                 BisqEasyOffer bisqEasyOffer,
                                                                 UserIdentity myUserIdentity,
                                                                 UserProfile requestingTrader,
                                                                 UserProfile nonRequestingTrader) {
        BisqEasyOpenTradeChannel channel;
        synchronized (this) {
            Optional<BisqEasyOpenTradeChannel> existingChannel = findChannelByTradeId(tradeId);
            if (existingChannel.isPresent()) {
                if (!isCreatedByChatMessageToMediator(existingChannel.get(), myUserIdentity)) {
                    return existingChannel.get();
                }
                log.warn("We replace the channel of trade {} which a chat message created before the mediation request",
                        StringUtils.sanitizeForLog(tradeId));
                getChannels().remove(existingChannel.get());
            }
            channel = BisqEasyOpenTradeChannel.create(tradeId,
                    bisqEasyOffer,
                    myUserIdentity,
                    Set.of(requestingTrader, nonRequestingTrader),
                    Optional.of(myUserIdentity.getUserProfile()),
                    true);
            getChannels().add(channel);
        }
        persist();
        processPendingMessages(tradeId);
        return channel;
    }

    public CompletableFuture<SendMessageResult> sendTakeOfferMessage(String tradeId,
                                                                     BisqEasyOffer bisqEasyOffer,
                                                                     Optional<UserProfile> mediator) {
        return userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId())
                .map(makerUserProfile -> {
                    if (bannedUserService.isUserProfileBanned(makerUserProfile)) {
                        return CompletableFuture.<SendMessageResult>failedFuture(new RuntimeException("Maker is banned"));
                    }
                    UserIdentity myUserIdentity = userIdentityService.getSelectedUserIdentity();
                    if (bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile())) {
                        return CompletableFuture.<SendMessageResult>failedFuture(new RuntimeException());
                    }
                    BisqEasyOpenTradeChannel channel = traderFindOrCreatesChannel(tradeId,
                            bisqEasyOffer,
                            myUserIdentity,
                            makerUserProfile,
                            mediator);
                    UserProfile maker = channel.getPeer();
                    BisqEasyOpenTradeMessage takeOfferMessage = BisqEasyOpenTradeMessage.createTakeOfferMessage(
                            tradeId,
                            channel.getId(),
                            myUserIdentity.getUserProfile(),
                            maker,
                            channel.getMediator(),
                            bisqEasyOffer);
                    addMessage(takeOfferMessage, channel);
                    return networkService.confidentialSend(takeOfferMessage, maker.getNetworkId(), myUserIdentity.getNetworkIdWithKeyPair());
                })
                .orElseGet(() -> CompletableFuture.failedFuture(new RuntimeException("makerUserProfile not found from message.authorUserProfileId")));
    }

    public CompletableFuture<SendMessageResult> sendTradeLogMessage(String text,
                                                                    BisqEasyOpenTradeChannel channel) {
        return sendMessage(text, Optional.empty(), ChatMessageType.PROTOCOL_LOG_MESSAGE, channel);
    }

    public CompletableFuture<SendMessageResult> sendTextMessage(String text,
                                                                Optional<Citation> citation,
                                                                BisqEasyOpenTradeChannel channel) {
        return sendMessage(text, citation, ChatMessageType.TEXT, channel);
    }

    private CompletableFuture<SendMessageResult> sendMessage(@Nullable String text,
                                                             Optional<Citation> citation,
                                                             ChatMessageType chatMessageType,
                                                             BisqEasyOpenTradeChannel channel) {
        String messageId = StringUtils.createUid();
        long date = System.currentTimeMillis();
        if (channel.isInMediation() && channel.getMediator().isPresent()) {
            String senderId = channel.getMyUserIdentity().getId();
            List<CompletableFuture<SendMessageResult>> futures = channel.getTraders().stream()
                    .filter(peer -> !peer.getId().equals(senderId))
                    .map(peer -> sendMessage(messageId, text, citation, channel, peer, chatMessageType, date))
                    .collect(Collectors.toList());
            channel.getMediator()
                    .filter(mediator -> !mediator.getId().equals(senderId))
                    .map(mediator -> sendMessage(messageId, text, citation, channel, mediator, chatMessageType, date))
                    .ifPresent(futures::add);
            return CompletableFutureUtils.allOf(futures)
                    .thenApply(list -> list.get(0));
        } else {
            return sendMessage(messageId, text, citation, channel, channel.getPeer(), chatMessageType, date);
        }
    }

    public CompletableFuture<SendMessageResult> sendTextMessageReaction(BisqEasyOpenTradeMessage message,
                                                                        BisqEasyOpenTradeChannel channel,
                                                                        Reaction reaction,
                                                                        boolean isRemoved) {
        return sendMessageReaction(message, channel, channel.getPeer(), reaction, StringUtils.createUid(), isRemoved);
    }

    @Override
    public void leaveChannel(BisqEasyOpenTradeChannel channel) {
        super.leaveChannel(channel);

        // We want to send a leave message even the peer has not sent any message so far (is not participant yet).
        long date = System.currentTimeMillis();
        Stream.concat(channel.getTraders().stream(), channel.getMediator().stream())
                .filter(userProfile -> allowSendLeaveMessage(channel, userProfile))
                .forEach(userProfile -> sendLeaveMessage(channel, userProfile, date));
    }

    @Override
    public ObservableSet<BisqEasyOpenTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public void setIsInMediation(BisqEasyOpenTradeChannel channel, boolean isInMediation) {
        channel.setIsInMediation(isInMediation);
        persist();
    }

    public void addMediatorsResponseMessage(BisqEasyOpenTradeChannel channel, String text) {
        setIsInMediation(channel, true);
        checkArgument(channel.getMediator().isPresent());
        UserProfile receiverUserProfile = channel.getMyUserIdentity().getUserProfile();
        UserProfile senderUserProfile = channel.getMediator().get();
        BisqEasyOpenTradeMessage tradeLogMessage = new BisqEasyOpenTradeMessage(channel.getTradeId(),
                StringUtils.createUid(),
                channel.getId(),
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                channel.getMediator(),
                ChatMessageType.PROTOCOL_LOG_MESSAGE,
                Optional.empty(),
                new HashSet<>());
        channel.addChatMessage(tradeLogMessage);
    }

    public Optional<BisqEasyOpenTradeChannel> findChannel(String offerId, String peersUserProfileId) {
        return getChannels().stream()
                .filter(channel -> channel.getBisqEasyOffer().getId().equals(offerId))
                .filter(channel -> channel.getPeer().getId().equals(peersUserProfileId))
                .findAny();
    }

    public Optional<BisqEasyOpenTradeChannel> findChannelByTradeId(String tradeId) {
        return getChannels().stream()
                .filter(channel -> channel.getTradeId().equals(tradeId))
                .findAny();
    }


    /* --------------------------------------------------------------------- */
    // Protected
    /* --------------------------------------------------------------------- */

    @Override
    protected BisqEasyOpenTradeMessage createAndGetNewPrivateChatMessage(String messageId,
                                                                         BisqEasyOpenTradeChannel channel,
                                                                         UserProfile senderUserProfile,
                                                                         UserProfile receiverUserProfile,
                                                                         @Nullable String text,
                                                                         Optional<Citation> citation,
                                                                         long time,
                                                                         boolean wasEdited,
                                                                         ChatMessageType chatMessageType) {
        // We send mediator only at first message
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.getMediator() : Optional.empty();
        return new BisqEasyOpenTradeMessage(
                channel.getTradeId(),
                messageId,
                channel.getId(),
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                citation,
                time,
                wasEdited,
                mediator,
                chatMessageType,
                Optional.empty(),
                new HashSet<>());
    }

    //todo (refactor, low prio)
    @Override
    protected BisqEasyOpenTradeChannel createAndGetNewPrivateChatChannel(UserProfile peer,
                                                                         UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }

    @Override
    protected BisqEasyOpenTradeMessageReaction createAndGetNewPrivateChatMessageReaction(BisqEasyOpenTradeMessage message,
                                                                                         UserProfile senderUserProfile,
                                                                                         UserProfile receiverUserProfile,
                                                                                         Reaction reaction,
                                                                                         String messageReactionId,
                                                                                         boolean isRemoved) {
        return new BisqEasyOpenTradeMessageReaction(
                messageReactionId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                message.getChannelId(),
                message.getChatChannelDomain(),
                message.getId(),
                reaction.ordinal(),
                System.currentTimeMillis(),
                isRemoved
        );
    }

    @Override
    protected void addMessageAndProcessQueuedReactions(BisqEasyOpenTradeMessage message,
                                                       BisqEasyOpenTradeChannel channel) {
        addMessage(message, channel);
        // Check if there are any reactions that should be added to existing messages
        processQueuedReactions();

        if (pendingMessages.remove(message)) {
            log.info("Removed message from pendingMessages. tradeId={}", StringUtils.sanitizeForLog(message.getTradeId()));
        }
    }

    // The sender chooses the trade ID of its message. A message with the trade ID of another trade does not belong to
    // this trade, and the mediator ignores a mediation request which carries such a message (rule 5 of
    // docs/specifications/chat/private-channel-membership.md).
    @Override
    protected boolean isMessageOfChannel(BisqEasyOpenTradeChannel channel, BisqEasyOpenTradeMessage message) {
        boolean isMessageOfTrade = channel.getTradeId().equals(message.getTradeId());
        if (!isMessageOfTrade) {
            log.warn("Ignoring a trade chat message whose trade ID is not the trade ID of channel {}", channel.getId());
        }
        return isMessageOfTrade;
    }

    @Override
    protected Optional<BisqEasyOpenTradeChannel> createNewChannelFromReceivedMessage(BisqEasyOpenTradeMessage message) {
        Optional<UserIdentity> myUserIdentity = userIdentityService.findUserIdentity(message.getReceiverUserProfileId());
        if (myUserIdentity.isEmpty() || message.isExpired()) {
            log.warn("We received a message for a new channel which is not addressed to one of our identities or is " +
                    "expired. We ignore it. tradeId={}, messageId={}", StringUtils.sanitizeForLog(message.getTradeId()),
                    StringUtils.sanitizeForLog(message.getId()));
            return Optional.empty();
        }
        if (message.getBisqEasyOffer().isPresent() &&
                isTradeWithPeer.test(message.getTradeId(),
                        message.getSenderUserProfile().getId(),
                        message.getReceiverUserProfileId())) {
            return Optional.of(traderFindOrCreatesChannel(message.getTradeId(),
                    message.getBisqEasyOffer().get(),
                    myUserIdentity.get(),
                    message.getSenderUserProfile(),
                    message.getMediator()));
        }
        // A message without offer, or for a trade which we do not have. The taker's first chat message can arrive
        // before the take offer request which creates our trade; the trade service then processes the pending messages.
        // If the trade is created after the check above, onMessage processes the pending messages again.
        pendingMessages.add(message);
        return Optional.empty();
    }

    public void setIsTradeWithPeer(TradeWithPeerPredicate isTradeWithPeer) {
        this.isTradeWithPeer = isTradeWithPeer;
    }

    @FunctionalInterface
    public interface TradeWithPeerPredicate {
        boolean test(String tradeId, String senderUserProfileId, String receiverUserProfileId);
    }

    // The taker's UI and the received take offer message can both create the channel, on different threads.
    @VisibleForTesting
    BisqEasyOpenTradeChannel traderFindOrCreatesChannel(String tradeId,
                                                        BisqEasyOffer bisqEasyOffer,
                                                        UserIdentity myUserIdentity,
                                                        UserProfile peer,
                                                        Optional<UserProfile> mediator) {
        BisqEasyOpenTradeChannel channel;
        synchronized (this) {
            Optional<BisqEasyOpenTradeChannel> existingChannel = findChannelByTradeId(tradeId);
            if (existingChannel.isPresent()) {
                return existingChannel.get();
            }
            channel = BisqEasyOpenTradeChannel.create(tradeId, bisqEasyOffer, myUserIdentity, Set.of(peer), mediator, false);
            getChannels().add(channel);
        }
        persist();
        processPendingMessages(tradeId);
        return channel;
    }

    // Only a mediation request creates a channel with two traders, and the mediator profile never trades in a trade
    // which it mediates, as the mediator selection excludes both traders. So a channel of the trade which has the
    // mediator's identity as own identity, but not two traders, was created by a chat message of a user who knew the
    // trade ID. A channel of another of our identities can be the channel of our own trade, and a channel with two
    // traders holds a mediation case. Both are kept.
    private static boolean isCreatedByChatMessageToMediator(BisqEasyOpenTradeChannel channel,
                                                            UserIdentity mediatorUserIdentity) {
        return channel.getMyUserIdentity().getId().equals(mediatorUserIdentity.getId()) &&
                channel.getTraders().size() != 2;
    }

    private boolean allowSendLeaveMessage(BisqEasyOpenTradeChannel channel, UserProfile userProfile) {
        return channel.getUserProfileIdsOfSendingLeaveMessage().contains(userProfile.getId());
    }

    @VisibleForTesting
    int getNumPendingMessages() {
        return pendingMessages.size();
    }

    public void processPendingMessages(String tradeId) {
        pendingMessages.getMessagesOfTrade(tradeId).forEach(this::processMessage);
    }
}
