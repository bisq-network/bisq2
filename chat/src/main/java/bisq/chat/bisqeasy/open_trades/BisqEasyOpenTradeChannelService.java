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

package bisq.chat.bisqeasy.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.priv.PrivateGroupChatChannelService;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class BisqEasyOpenTradeChannelService extends PrivateGroupChatChannelService<BisqEasyOpenTradeMessage, BisqEasyOpenTradeChannel, BisqEasyOpenTradeChannelStore> {

    @Getter
    private final BisqEasyOpenTradeChannelStore persistableStore = new BisqEasyOpenTradeChannelStore();
    @Getter
    private final Persistence<BisqEasyOpenTradeChannelStore> persistence;

    public BisqEasyOpenTradeChannelService(PersistenceService persistenceService,
                                           NetworkService networkService,
                                           UserService userService,
                                           ProofOfWorkService proofOfWorkService) {
        super(networkService, userService, proofOfWorkService, ChatChannelDomain.BISQ_EASY_OPEN_TRADES);

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof BisqEasyOpenTradeMessage) {
            processMessage((BisqEasyOpenTradeMessage) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyOpenTradeChannel traderFindOrCreatesChannel(String tradeId,
                                                               BisqEasyOffer bisqEasyOffer,
                                                               UserIdentity myUserIdentity,
                                                               UserProfile peer,
                                                               Optional<UserProfile> mediator) {
        return findChannelByTradeId(tradeId)
                .orElseGet(() -> traderCreatesChannel(tradeId, bisqEasyOffer, myUserIdentity, peer, mediator));
    }

    public BisqEasyOpenTradeChannel traderCreatesChannel(String tradeId,
                                                         BisqEasyOffer bisqEasyOffer,
                                                         UserIdentity myUserIdentity,
                                                         UserProfile peer,
                                                         Optional<UserProfile> mediator) {
        BisqEasyOpenTradeChannel channel = BisqEasyOpenTradeChannel.createByTrader(tradeId, bisqEasyOffer, myUserIdentity, peer, mediator);
        getChannels().add(channel);
        persist();
        return channel;
    }

    public BisqEasyOpenTradeChannel mediatorFindOrCreatesChannel(String tradeId,
                                                                 BisqEasyOffer bisqEasyOffer,
                                                                 UserIdentity myUserIdentity,
                                                                 UserProfile requestingTrader,
                                                                 UserProfile nonRequestingTrader) {
        return findChannelByTradeId(tradeId)
                .orElseGet(() -> {
                    BisqEasyOpenTradeChannel channel = BisqEasyOpenTradeChannel.createByMediator(tradeId,
                            bisqEasyOffer,
                            myUserIdentity,
                            requestingTrader,
                            nonRequestingTrader);
                    getChannels().add(channel);
                    persist();
                    return channel;
                });
    }

    public CompletableFuture<SendMessageResult> sendTakeOfferMessage(String tradeId,
                                                                     BisqEasyOffer bisqEasyOffer,
                                                                     Optional<UserProfile> mediator) {
        return userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId())
                .map(makerUserProfile -> {
                    if (bannedUserService.isUserProfileBanned(makerUserProfile)) {
                        return CompletableFuture.<SendMessageResult>failedFuture(new RuntimeException("Maker is banned"));
                    }
                    UserIdentity myUserIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
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
                .orElse(CompletableFuture.failedFuture(new RuntimeException("makerUserProfile not found from message.authorUserProfileId")));
    }

    public CompletableFuture<SendMessageResult> sendSystemMessage(String text,
                                                                  BisqEasyOpenTradeChannel channel) {
        return sendMessage(text, Optional.empty(), ChatMessageType.SYSTEM_MESSAGE, channel);
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
        String shortUid = StringUtils.createUid();
        long date = new Date().getTime();
        if (channel.isInMediation() && channel.getMediator().isPresent()) {
            List<CompletableFuture<SendMessageResult>> futures = channel.getTraders().stream()
                    .map(peer -> sendMessage(shortUid, text, citation, channel, peer, chatMessageType, date))
                    .collect(Collectors.toList());
            channel.getMediator()
                    .map(mediator -> sendMessage(shortUid, text, citation, channel, mediator, chatMessageType, date))
                    .ifPresent(futures::add);
            return CompletableFutureUtils.allOf(futures)
                    .thenApply(list -> list.get(0));
        } else {
            return sendMessage(shortUid, text, citation, channel, channel.getPeer(), chatMessageType, date);
        }
    }

    @Override
    public void leaveChannel(BisqEasyOpenTradeChannel channel) {
        super.leaveChannel(channel);

        // We want to send a leave message even the peer has not sent any message so far (is not participant yet).
        long date = new Date().getTime();
        Stream.concat(channel.getTraders().stream(), channel.getMediator().stream())
                .filter(userProfile -> allowSendLeaveMessage(channel, userProfile))
                .forEach(userProfile -> sendLeaveMessage(channel, userProfile, date));
    }

    @Override
    public ObservableArray<BisqEasyOpenTradeChannel> getChannels() {
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
        BisqEasyOpenTradeMessage systemMessage = new BisqEasyOpenTradeMessage(channel.getTradeId(),
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
                ChatMessageType.SYSTEM_MESSAGE,
                Optional.empty());
        channel.addChatMessage(systemMessage);
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


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

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
                Optional.empty());
    }


    //todo
    @Override
    protected BisqEasyOpenTradeChannel createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }

    @Override
    protected void processMessage(BisqEasyOpenTradeMessage message) {
        if (canHandleChannelDomain(message) && isValid(message)) {
            findChannel(message)
                    .or(() -> {
                        // We prevent to send leave messages after a peer has left, but there might be still 
                        // race conditions where that might happen, so we check at receiving the message as well, so that
                        // in cases we would get a leave message as first message (e.g. after having closed the channel) 
                        //  we do not create a channel.
                        if (message.getChatMessageType() == ChatMessageType.LEAVE) {
                            log.warn("We received a leave message as first message. This is not expected but might " +
                                    "happen in some rare cases.");
                            return Optional.empty();
                        } else if (message.getBisqEasyOffer().isPresent()) {
                            return userIdentityService.findUserIdentity(message.getReceiverUserProfileId())
                                    .map(myUserIdentity -> traderCreatesChannel(message.getTradeId(),
                                            message.getBisqEasyOffer().get(),
                                            myUserIdentity,
                                            message.getSenderUserProfile(),
                                            message.getMediator()));
                        } else {
                            // It could be that taker sends quickly a message after take offer, and we receive them 
                            // out of order. In that case the seconds message (which arrived first) would get dropped.
                            // This is a very unlikely case, so we ignore it.
                            // It also happens if we left a trade channel and receive a message again. 
                            // We ignore that and do not re-open the channel.
                            log.debug("We received the first message for a new channel without an offer. " +
                                    "We drop that message. Message={}", message);
                            return Optional.empty();
                        }
                    })
                    .ifPresent(channel -> addMessage(message, channel));
        }
    }

    private boolean allowSendLeaveMessage(BisqEasyOpenTradeChannel channel, UserProfile userProfile) {
        return channel.getUserProfileIdsOfSendingLeaveMessage().contains(userProfile.getId());
    }
}