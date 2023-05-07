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

import static com.google.common.base.Preconditions.checkArgument;

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

    public CompletableFuture<NetworkService.SendMessageResult> sendTakeOfferMessage(BisqEasyPublicChatMessage offerMessage,
                                                                                    BisqEasyPrivateTradeChatChannel channel) {
        checkArgument(offerMessage.getBisqEasyOffer().isPresent());
        UserProfile maker = channel.getPeer();
        BisqEasyOffer bisqEasyOffer = offerMessage.getBisqEasyOffer().get();
        String direction = Res.get(bisqEasyOffer.getDirection().mirror().name().toLowerCase()).toUpperCase();
        String amount = AmountFormatter.formatAmountWithCode(Fiat.of(bisqEasyOffer.getQuoteSideAmount(),
                bisqEasyOffer.getMarket().getQuoteCurrencyCode()), true);
        String methods = Joiner.on(", ").join(bisqEasyOffer.getPaymentMethods());
        String text = Res.get("bisqEasy.takeOffer.takerRequest",
                direction, amount, methods);
        Optional<Citation> citation = Optional.of(new Citation(maker.getNym(),
                maker.getNickName(),
                maker.getPubKeyHash(),
                offerMessage.getText()));
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        BisqEasyPrivateTradeChatMessage takeOfferMessage = new BisqEasyPrivateTradeChatMessage(StringUtils.createShortUid(),
                channel.getId(),
                myUserIdentity.getUserProfile(),
                maker.getId(),
                text,
                citation,
                new Date().getTime(),
                false,
                channel.findMediator(),
                ChatMessageType.TAKE_BISQ_EASY_OFFER,
                Optional.of(bisqEasyOffer));
        addMessage(takeOfferMessage, channel);
        return networkService.confidentialSend(takeOfferMessage, maker.getNetworkId(), myUserIdentity.getNodeIdAndKeyPair());
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTextMessage(String text,
                                                                               Optional<Citation> citation,
                                                                               BisqEasyPrivateTradeChatChannel channel) {
        String shortUid = StringUtils.createShortUid();
        if (channel.getIsInMediation().get() && channel.findMediator().isPresent()) {
            List<CompletableFuture<NetworkService.SendMessageResult>> futures = channel.getPeers().stream()
                    .map(peer -> sendMessage(shortUid, text, citation, channel, peer, ChatMessageType.TEXT))
                    .collect(Collectors.toList());
            return CompletableFutureUtils.allOf(futures)
                    .thenApply(list -> list.get(0));
        } else {
            return sendMessage(shortUid, text, citation, channel, channel.getPeer(), ChatMessageType.TEXT);
        }
    }

    public void setIsInMediation(BisqEasyPrivateTradeChatChannel channel, boolean isInMediation) {
        channel.getIsInMediation().set(isInMediation);
        persist();
    }

    @Override
    public ObservableArray<BisqEasyPrivateTradeChatChannel> getChannels() {
        return persistableStore.getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected String provideChannelTitle(BisqEasyPrivateTradeChatChannel chatChannel) {
        String peer = chatChannel.getPeer().getUserName();
        String optionalMyUserProfilePostfix = userIdentityService.hasMultipleUserIdentities() ? "" :
                " [" + chatChannel.getMyUserIdentity().getUserName() + "]";
        if (chatChannel.isMediator()) {
            // We are the mediator:
            // If mediator has 1 identity we show: UserName1 - UserName2
            // If mediator has multiple identities we show: UserName1 - UserName2 [MediatorUserName]
            checkArgument(chatChannel.getPeers().size() >= 2, "getPeers().size() need to be >= 2");
            return peer + " - " + chatChannel.getPeers().get(1).getUserName() + optionalMyUserProfilePostfix;
        } else {
            // We are the trader:
            // If not in mediation or no mediator is available:
            // If trader has 1 identity we show: PeersUserName 
            // If trader has multiple identities we show: PeersUserName [MyUserName]
            // If in mediation and mediator is available:
            // If trader has 1 identity we show: PeersUserName (Mediator: MediatorUserName)
            // If trader has multiple identities we show: PeersUserName (Mediator: MediatorUserName) [MyUserName]
            String optionalMediatorPostfix = chatChannel.findMediator()
                    .filter(mediator -> chatChannel.getIsInMediation().get())
                    .map(mediator -> ", " + mediator.getUserName() + " (" + Res.get("mediator") + ")")
                    .orElse("");
            return peer + optionalMediatorPostfix + optionalMyUserProfilePostfix;
        }
    }

    public Optional<BisqEasyPrivateTradeChatChannel> findChannel(BisqEasyOffer bisqEasyOffer) {
        return findChannel(BisqEasyPrivateTradeChatChannel.createId(bisqEasyOffer));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected BisqEasyPrivateTradeChatMessage createNewPrivateChatMessage(String messageId,
                                                                          BisqEasyPrivateTradeChatChannel channel,
                                                                          UserProfile sender,
                                                                          String receiversId,
                                                                          String text,
                                                                          Optional<Citation> citation,
                                                                          long time,
                                                                          boolean wasEdited,
                                                                          ChatMessageType chatMessageType) {
        // We send mediator only at first message
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.findMediator() : Optional.empty();
        return new BisqEasyPrivateTradeChatMessage(
                messageId,
                channel.getId(),
                sender,
                receiversId,
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
    protected BisqEasyPrivateTradeChatChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }

    private void processMessage(BisqEasyPrivateTradeChatMessage message) {
        if (!userIdentityService.isUserIdentityPresent(message.getAuthorUserProfileId())) {
            userIdentityService.findUserIdentity(message.getReceiversId())
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
}