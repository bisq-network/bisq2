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

package bisq.chat.mu_sig.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.priv.PrivateGroupChatChannelService;
import bisq.chat.reactions.MuSigOpenTradeMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigOpenTradeChannelService extends PrivateGroupChatChannelService<MuSigOpenTradeMessageReaction,
        MuSigOpenTradeMessage, MuSigOpenTradeChannel, MuSigOpenTradeChannelStore> {

    @Getter
    private final MuSigOpenTradeChannelStore persistableStore = new MuSigOpenTradeChannelStore();
    @Getter
    private final Persistence<MuSigOpenTradeChannelStore> persistence;
    private final Set<MuSigOpenTradeMessage> pendingMessages = new CopyOnWriteArraySet<>();

    public MuSigOpenTradeChannelService(PersistenceService persistenceService,
                                        NetworkService networkService,
                                        UserService userService) {
        super(networkService, userService, ChatChannelDomain.MU_SIG_OPEN_TRADES);

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    /* --------------------------------------------------------------------- */
    // MessageListener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigOpenTradeMessage) {
            processMessage((MuSigOpenTradeMessage) envelopePayloadMessage);
            if (!pendingMessages.isEmpty()) {
                log.info("Processing pendingMessages messages");
                pendingMessages.forEach(this::processMessage);
            }
        } else if (envelopePayloadMessage instanceof MuSigOpenTradeMessageReaction) {
            processMessageReaction((MuSigOpenTradeMessageReaction) envelopePayloadMessage);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public MuSigOpenTradeChannel traderFindOrCreatesChannel(String tradeId,
                                                            UserIdentity myUserIdentity,
                                                            UserProfile peer,
                                                            Optional<UserProfile> mediator) {
        return findChannelByTradeId(tradeId)
                .orElseGet(() -> traderCreatesChannel(tradeId, myUserIdentity, peer, mediator));
    }

    public MuSigOpenTradeChannel traderCreatesChannel(String tradeId,
                                                      UserIdentity myUserIdentity,
                                                      UserProfile peer,
                                                      Optional<UserProfile> mediator) {
        MuSigOpenTradeChannel channel = MuSigOpenTradeChannel.createByTrader(tradeId, myUserIdentity, peer, mediator);
        getChannels().add(channel);
        persist();
        return channel;
    }

    public MuSigOpenTradeChannel mediatorFindOrCreatesChannel(String tradeId,
                                                              UserIdentity myUserIdentity,
                                                              UserProfile requestingTrader,
                                                              UserProfile nonRequestingTrader) {
        return findChannelByTradeId(tradeId)
                .orElseGet(() -> {
                    MuSigOpenTradeChannel channel = MuSigOpenTradeChannel.createByMediator(tradeId,
                            myUserIdentity,
                            requestingTrader,
                            nonRequestingTrader);
                    getChannels().add(channel);
                    persist();
                    return channel;
                });
    }

    public CompletableFuture<SendMessageResult> sendTradeLogMessage(String text,
                                                                    MuSigOpenTradeChannel channel) {
        return sendMessage(text, Optional.empty(), ChatMessageType.PROTOCOL_LOG_MESSAGE, channel);
    }

    public CompletableFuture<SendMessageResult> sendTextMessage(String text,
                                                                Optional<Citation> citation,
                                                                MuSigOpenTradeChannel channel) {
        return sendMessage(text, citation, ChatMessageType.TEXT, channel);
    }

    private CompletableFuture<SendMessageResult> sendMessage(@Nullable String text,
                                                             Optional<Citation> citation,
                                                             ChatMessageType chatMessageType,
                                                             MuSigOpenTradeChannel channel) {
        String messageId = StringUtils.createUid();
        long date = new Date().getTime();
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
                    .thenApply(list -> list.get(0)); //TODO return list?
        } else {
            return sendMessage(messageId, text, citation, channel, channel.getPeer(), chatMessageType, date);
        }
    }

    public CompletableFuture<SendMessageResult> sendTextMessageReaction(MuSigOpenTradeMessage message,
                                                                        MuSigOpenTradeChannel channel,
                                                                        Reaction reaction,
                                                                        boolean isRemoved) {
        return sendMessageReaction(message, channel, channel.getPeer(), reaction, StringUtils.createUid(), isRemoved);
    }

    @Override
    public void leaveChannel(MuSigOpenTradeChannel channel) {
        super.leaveChannel(channel);

        // We want to send a leave message even the peer has not sent any message so far (is not participant yet).
        long date = new Date().getTime();
        Stream.concat(channel.getTraders().stream(), channel.getMediator().stream())
                .filter(userProfile -> allowSendLeaveMessage(channel, userProfile))
                .forEach(userProfile -> sendLeaveMessage(channel, userProfile, date));
    }

    @Override
    public ObservableSet<MuSigOpenTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public void setIsInMediation(MuSigOpenTradeChannel channel, boolean isInMediation) {
        channel.setIsInMediation(isInMediation);
        persist();
    }

    public void addMediatorsResponseMessage(MuSigOpenTradeChannel channel, String text) {
        setIsInMediation(channel, true);
        checkArgument(channel.getMediator().isPresent());
        UserProfile receiverUserProfile = channel.getMyUserIdentity().getUserProfile();
        UserProfile senderUserProfile = channel.getMediator().get();
        MuSigOpenTradeMessage tradeLogMessage = new MuSigOpenTradeMessage(channel.getTradeId(),
                StringUtils.createUid(),
                channel.getId(),
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                text,
                Optional.empty(),
                new Date().getTime(),
                false,
                channel.getMediator(),
                ChatMessageType.PROTOCOL_LOG_MESSAGE,
                Optional.empty(),
                new HashSet<>());
        channel.addChatMessage(tradeLogMessage);
    }

    public Optional<MuSigOpenTradeChannel> findChannel(String tradeId, String peersUserProfileId) {
        return getChannels().stream()
                .filter(channel -> channel.getTradeId().equals(tradeId))
                .filter(channel -> channel.getPeer().getId().equals(peersUserProfileId))
                .findAny();
    }

    public Optional<MuSigOpenTradeChannel> findChannelByTradeId(String tradeId) {
        return getChannels().stream()
                .filter(channel -> channel.getTradeId().equals(tradeId))
                .findAny();
    }


    /* --------------------------------------------------------------------- */
    // Protected
    /* --------------------------------------------------------------------- */

    @Override
    protected MuSigOpenTradeMessage createAndGetNewPrivateChatMessage(String messageId,
                                                                      MuSigOpenTradeChannel channel,
                                                                      UserProfile senderUserProfile,
                                                                      UserProfile receiverUserProfile,
                                                                      @Nullable String text,
                                                                      Optional<Citation> citation,
                                                                      long time,
                                                                      boolean wasEdited,
                                                                      ChatMessageType chatMessageType) {
        // We send mediator only at first message
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.getMediator() : Optional.empty();
        return new MuSigOpenTradeMessage(
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
    protected MuSigOpenTradeChannel createAndGetNewPrivateChatChannel(UserProfile peer,
                                                                      UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }

    @Override
    protected MuSigOpenTradeMessageReaction createAndGetNewPrivateChatMessageReaction(MuSigOpenTradeMessage message,
                                                                                      UserProfile senderUserProfile,
                                                                                      UserProfile receiverUserProfile,
                                                                                      Reaction reaction,
                                                                                      String messageReactionId,
                                                                                      boolean isRemoved) {
        return new MuSigOpenTradeMessageReaction(
                messageReactionId,
                senderUserProfile,
                receiverUserProfile.getId(),
                receiverUserProfile.getNetworkId(),
                message.getChannelId(),
                message.getChatChannelDomain(),
                message.getId(),
                reaction.ordinal(),
                new Date().getTime(),
                isRemoved
        );
    }

    @Override
    protected void addMessageAndProcessQueuedReactions(MuSigOpenTradeMessage message,
                                                       MuSigOpenTradeChannel channel) {
        addMessage(message, channel);
        // Check if there are any reactions that should be added to existing messages
        processQueuedReactions();

        if (pendingMessages.contains(message)) {
            log.info("Removing message from pendingMessages. message={}", message);
            pendingMessages.remove(message);
        }
    }

    @Override
    protected Optional<MuSigOpenTradeChannel> createNewChannelFromReceivedMessage(MuSigOpenTradeMessage message) {
        if (message.getMuSigOffer().isPresent()) {
            return userIdentityService.findUserIdentity(message.getReceiverUserProfileId())
                    .map(myUserIdentity -> traderCreatesChannel(message.getTradeId(),
                            myUserIdentity,
                            message.getSenderUserProfile(),
                            message.getMediator()));
        } else {
            // It could be that taker sends quickly a message after take offer, and we receive them
            // out of order. In that case the seconds message (which arrived first) would get dropped.
            // This is a very unlikely case, so we ignore it.
            // It also happens if we left a trade channel and receive a message again.
            // We ignore that and do not re-open the channel.
            log.warn("We received the first message for a new channel without an offer. " +
                    "We add that message to pendingMessages for re-processing when we receive the next message. " +
                    "Message={}", message);
            pendingMessages.add(message);
            return Optional.empty();
        }
    }

    private boolean allowSendLeaveMessage(MuSigOpenTradeChannel channel, UserProfile userProfile) {
        return channel.getUserProfileIdsOfSendingLeaveMessage().contains(userProfile.getId());
    }
}
