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

package bisq.chat.bisqeasy.channel.priv;

import bisq.chat.bisqeasy.message.BisqEasyOffer;
import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.PrivateGroupChatChannelService;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.common.monetary.Fiat;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.presentation.formatters.AmountFormatter;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class BisqEasyPrivateTradeChatChannelService extends PrivateGroupChatChannelService<BisqEasyPrivateTradeChatMessage, BisqEasyPrivateTradeChatChannel, BisqEasyPrivateTradeChatChannelStore> {

    @Getter
    private final BisqEasyPrivateTradeChatChannelStore persistableStore = new BisqEasyPrivateTradeChatChannelStore();
    @Getter
    private final Persistence<BisqEasyPrivateTradeChatChannelStore> persistence;

    public BisqEasyPrivateTradeChatChannelService(PersistenceService persistenceService,
                                                  NetworkService networkService,
                                                  UserIdentityService userIdentityService,
                                                  UserProfileService userProfileService,
                                                  ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService, userProfileService, proofOfWorkService, ChatChannelDomain.BISQ_EASY);

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof BisqEasyPrivateTradeChatMessage) {
            processMessage((BisqEasyPrivateTradeChatMessage) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BisqEasyPrivateTradeChatChannel traderFindOrCreatesChannel(BisqEasyOffer bisqEasyOffer,
                                                                      UserIdentity myUserIdentity,
                                                                      UserProfile peer,
                                                                      Optional<UserProfile> mediator) {
        return findChannel(bisqEasyOffer)
                .orElseGet(() -> {
                    BisqEasyPrivateTradeChatChannel channel = BisqEasyPrivateTradeChatChannel.createByTrader(bisqEasyOffer, myUserIdentity, peer, mediator);
                    getChannels().add(channel);
                    persist();
                    return channel;
                });
    }

    public BisqEasyPrivateTradeChatChannel mediatorFindOrCreatesChannel(BisqEasyOffer bisqEasyOffer,
                                                                        UserIdentity myUserIdentity,
                                                                        UserProfile requestingTrader,
                                                                        UserProfile nonRequestingTrader) {
        return findChannel(bisqEasyOffer)
                .orElseGet(() -> {
                    BisqEasyPrivateTradeChatChannel channel = BisqEasyPrivateTradeChatChannel.createByMediator(bisqEasyOffer, myUserIdentity, requestingTrader, nonRequestingTrader);
                    getChannels().add(channel);
                    persist();
                    return channel;
                });
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTakeOfferMessage(BisqEasyPublicChatMessage message,
                                                                                    Optional<UserProfile> mediator) {
        checkArgument(message.getBisqEasyOffer().isPresent(), "message must contain offer");
        return userProfileService.findUserProfile(message.getAuthorUserProfileId())
                .map(makerUserProfile -> {
                    UserIdentity myUserIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
                    BisqEasyOffer bisqEasyOffer = message.getBisqEasyOffer().get();
                    BisqEasyPrivateTradeChatChannel channel = traderFindOrCreatesChannel(bisqEasyOffer,
                            myUserIdentity,
                            makerUserProfile,
                            mediator);
                    UserProfile maker = channel.getPeer();
                    String direction = Res.get(bisqEasyOffer.getDirection().mirror().name().toLowerCase()).toUpperCase();
                    String amount = AmountFormatter.formatAmountWithCode(Fiat.of(bisqEasyOffer.getQuoteSideAmount(),
                            bisqEasyOffer.getMarket().getQuoteCurrencyCode()), true);
                    String methods = Joiner.on(", ").join(bisqEasyOffer.getPaymentMethods());
                    String text = Res.get("bisqEasy.takeOffer.takerRequest",
                            direction, amount, methods);
                    Optional<Citation> citation = Optional.of(new Citation(maker.getNym(),
                            maker.getNickName(),
                            maker.getPubKeyHash(),
                            message.getText()));
                    BisqEasyPrivateTradeChatMessage takeOfferMessage = new BisqEasyPrivateTradeChatMessage(StringUtils.createShortUid(),
                            channel.getId(),
                            myUserIdentity.getUserProfile(),
                            maker.getId(),
                            text,
                            citation,
                            new Date().getTime(),
                            false,
                            channel.getMediator(),
                            ChatMessageType.TAKE_BISQ_EASY_OFFER,
                            Optional.of(bisqEasyOffer));
                    addMessage(takeOfferMessage, channel);
                    return networkService.confidentialSend(takeOfferMessage, maker.getNetworkId(), myUserIdentity.getNodeIdAndKeyPair());

                })
                .orElse(CompletableFuture.failedFuture(new RuntimeException("makerUserProfile not found from message.authorUserProfileId")));
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTextMessage(String text,
                                                                               Optional<Citation> citation,
                                                                               BisqEasyPrivateTradeChatChannel channel) {
        String shortUid = StringUtils.createShortUid();
        long date = new Date().getTime();
        if (channel.isInMediation() && channel.getMediator().isPresent()) {
            List<CompletableFuture<NetworkService.SendMessageResult>> futures = channel.getTraders().stream()
                    .map(peer -> sendMessage(shortUid, text, citation, channel, peer, ChatMessageType.TEXT, date))
                    .collect(Collectors.toList());
            channel.getMediator()
                    .map(mediator -> sendMessage(shortUid, text, citation, channel, mediator, ChatMessageType.TEXT, date))
                    .ifPresent(futures::add);
            return CompletableFutureUtils.allOf(futures)
                    .thenApply(list -> list.get(0));
        } else {
            return sendMessage(shortUid, text, citation, channel, channel.getPeer(), ChatMessageType.TEXT, date);
        }
    }

    @Override
    public void leaveChannel(BisqEasyPrivateTradeChatChannel channel) {
        super.leaveChannel(channel);

        // We want to send a leave message even the peer has not sent any message so far (is not participant yet).
        long date = new Date().getTime();
        Stream.concat(channel.getTraders().stream(), channel.getMediator().stream())
                .filter(userProfile -> allowSendLeaveMessage(channel, userProfile))
                .forEach(userProfile -> sendLeaveMessage(channel, userProfile, date));
    }

    @Override
    public ObservableArray<BisqEasyPrivateTradeChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public void setIsInMediation(BisqEasyPrivateTradeChatChannel channel, boolean isInMediation) {
        channel.setIsInMediation(isInMediation);
        persist();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public Optional<BisqEasyPrivateTradeChatChannel> findChannel(BisqEasyOffer bisqEasyOffer) {
        return findChannel(BisqEasyPrivateTradeChatChannel.createId(bisqEasyOffer));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected BisqEasyPrivateTradeChatMessage createAndGetNewPrivateChatMessage(String messageId,
                                                                                BisqEasyPrivateTradeChatChannel channel,
                                                                                UserProfile sender,
                                                                                String receiverUserProfileId,
                                                                                String text,
                                                                                Optional<Citation> citation,
                                                                                long time,
                                                                                boolean wasEdited,
                                                                                ChatMessageType chatMessageType) {
        // We send mediator only at first message
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.getMediator() : Optional.empty();
        return new BisqEasyPrivateTradeChatMessage(
                messageId,
                channel.getId(),
                sender,
                receiverUserProfileId,
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
    protected BisqEasyPrivateTradeChatChannel createAndGetNewPrivateChatChannel(UserProfile peer, UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }

    private void processMessage(BisqEasyPrivateTradeChatMessage message) {
        if (!userIdentityService.isUserIdentityPresent(message.getAuthorUserProfileId())) {
            userIdentityService.findUserIdentity(message.getReceiverUserProfileId())
                    .flatMap(myUserIdentity -> findChannel(message)
                            .or(() -> {
                                if (message.getChatMessageType() == ChatMessageType.LEAVE) {
                                    return Optional.empty();
                                } else if (userProfileService.isChatUserIgnored(message.getSender())) {
                                    return Optional.empty();
                                } else if (message.getBisqEasyOffer().isPresent()) {
                                    return Optional.of(traderFindOrCreatesChannel(message.getBisqEasyOffer().get(),
                                            myUserIdentity,
                                            message.getSender(),
                                            message.getMediator()));
                                } else {
                                    log.error("Unexpected case");
                                    return Optional.empty();
                                }
                            }))
                    .ifPresent(channel -> addMessage(message, channel));
        }
    }

    private boolean allowSendLeaveMessage(BisqEasyPrivateTradeChatChannel channel, UserProfile userProfile) {
        return channel.getUserProfileIdsOfSendingLeaveMessage().contains(userProfile.getId());
    }
}