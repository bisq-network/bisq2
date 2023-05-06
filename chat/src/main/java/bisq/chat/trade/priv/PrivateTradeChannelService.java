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

package bisq.chat.trade.priv;

import bisq.chat.channel.ChannelDomain;
import bisq.chat.channel.PrivateGroupChannelService;
import bisq.chat.message.MessageType;
import bisq.chat.message.Quotation;
import bisq.chat.trade.TradeChatOffer;
import bisq.chat.trade.pub.PublicTradeChatMessage;
import bisq.common.monetary.Fiat;
import bisq.common.observable.Pin;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class PrivateTradeChannelService extends PrivateGroupChannelService<PrivateTradeChatMessage, PrivateTradeChannel, PrivateTradeChannelStore> {

    @Getter
    private final PrivateTradeChannelStore persistableStore = new PrivateTradeChannelStore();
    @Getter
    private final Persistence<PrivateTradeChannelStore> persistence;
    private final Map<String, Pin> notificationTypeChangePins = new HashMap<>();

    public PrivateTradeChannelService(PersistenceService persistenceService,
                                      NetworkService networkService,
                                      UserIdentityService userIdentityService,
                                      UserProfileService userProfileService,
                                      ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService, userProfileService, proofOfWorkService, ChannelDomain.TRADE);

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateTradeChatMessage) {
            processMessage((PrivateTradeChatMessage) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public PrivateTradeChannel traderFindOrCreatesChannel(TradeChatOffer tradeChatOffer,
                                                          UserIdentity myUserIdentity,
                                                          UserProfile peer,
                                                          Optional<UserProfile> mediator) {
        return findChannel(tradeChatOffer.getId())
                .orElseGet(() -> {
                    PrivateTradeChannel channel = PrivateTradeChannel.createByTrader(tradeChatOffer, myUserIdentity, peer, mediator);
                    Pin pin = channel.getChannelNotificationType().addObserver(value -> persist());
                    notificationTypeChangePins.put(channel.getId(), pin);
                    getChannels().add(channel);
                    persist();
                    return channel;
                });
    }

    public PrivateTradeChannel mediatorFindOrCreatesChannel(TradeChatOffer tradeChatOffer,
                                                            UserIdentity myUserIdentity,
                                                            UserProfile requestingTrader,
                                                            UserProfile nonRequestingTrader) {
        return findChannel(tradeChatOffer.getId())
                .orElseGet(() -> {
                    PrivateTradeChannel channel = PrivateTradeChannel.createByMediator(tradeChatOffer, myUserIdentity, requestingTrader, nonRequestingTrader);
                    Pin pin = channel.getChannelNotificationType().addObserver(value -> persist());
                    notificationTypeChangePins.put(channel.getId(), pin);
                    getChannels().add(channel);
                    persist();
                    return channel;
                });
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTakeOfferMessage(PublicTradeChatMessage offerMessage,
                                                                                    PrivateTradeChannel channel) {
        checkArgument(offerMessage.getTradeChatOffer().isPresent());
        UserProfile maker = channel.getPeer();
        TradeChatOffer tradeChatOffer = offerMessage.getTradeChatOffer().get();
        String direction = Res.get(tradeChatOffer.getDirection().mirror().name().toLowerCase()).toUpperCase();
        String amount = AmountFormatter.formatAmountWithCode(Fiat.of(tradeChatOffer.getQuoteSideAmount(),
                tradeChatOffer.getMarket().getQuoteCurrencyCode()), true);
        String methods = Joiner.on(", ").join(tradeChatOffer.getPaymentMethods());
        String text = Res.get("bisqEasy.takeOffer.takerRequest",
                direction, amount, methods);
        Optional<Quotation> quotation = Optional.of(new Quotation(maker.getNym(),
                maker.getNickName(),
                maker.getPubKeyHash(),
                offerMessage.getText()));
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        PrivateTradeChatMessage takeOfferMessage = new PrivateTradeChatMessage(StringUtils.createShortUid(),
                channel.getChannelName(),
                myUserIdentity.getUserProfile(),
                maker.getId(),
                text,
                quotation,
                new Date().getTime(),
                false,
                channel.findMediator(),
                MessageType.TAKE_OFFER,
                Optional.of(tradeChatOffer));
        addMessage(takeOfferMessage, channel);
        return networkService.confidentialSend(takeOfferMessage, maker.getNetworkId(), myUserIdentity.getNodeIdAndKeyPair());
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTextMessage(String text,
                                                                               Optional<Quotation> quotation,
                                                                               PrivateTradeChannel channel) {
        String shortUid = StringUtils.createShortUid();
        if (channel.getIsInMediation().get() && channel.findMediator().isPresent()) {
            List<CompletableFuture<NetworkService.SendMessageResult>> futures = channel.getPeers().stream()
                    .map(peer -> sendMessage(shortUid, text, quotation, channel, peer, MessageType.TEXT))
                    .collect(Collectors.toList());
            return CompletableFutureUtils.allOf(futures)
                    .thenApply(list -> list.get(0));
        } else {
            return sendMessage(shortUid, text, quotation, channel, channel.getPeer(), MessageType.TEXT);
        }
    }

    public void setMediationActivated(PrivateTradeChannel channel, boolean mediationActivated) {
        channel.getIsInMediation().set(mediationActivated);
        persist();
    }

    @Override
    public ObservableArray<PrivateTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected PrivateTradeChatMessage createNewPrivateChatMessage(String messageId,
                                                                  PrivateTradeChannel channel,
                                                                  UserProfile sender,
                                                                  String receiversId,
                                                                  String text,
                                                                  Optional<Quotation> quotedMessage,
                                                                  long time,
                                                                  boolean wasEdited,
                                                                  MessageType messageType) {
        // We send mediator only at first message
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.findMediator() : Optional.empty();
        return new PrivateTradeChatMessage(
                messageId,
                channel.getChannelName(),
                sender,
                receiversId,
                text,
                quotedMessage,
                time,
                wasEdited,
                mediator,
                messageType,
                Optional.empty());
    }

    //todo
    @Override
    protected PrivateTradeChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }


    private void processMessage(PrivateTradeChatMessage message) {
        if (!userIdentityService.isUserIdentityPresent(message.getAuthorId())) {
            userIdentityService.findUserIdentity(message.getReceiversId())
                    .flatMap(myUserIdentity -> findChannelForMessage(message)
                            .or(() -> {
                                if (message.getMessageType() == MessageType.LEAVE) {
                                    return Optional.empty();
                                } else if (userProfileService.isChatUserIgnored(message.getSender())) {
                                    return Optional.empty();
                                } else if (message.getTradeChatOffer().isPresent()) {
                                    return Optional.of(traderFindOrCreatesChannel(message.getTradeChatOffer().get(),
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


   

   /* public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage1(String text,
                                                                                       Optional<Quotation> quotedMessage,
                                                                                       PrivateTradeChannel channel,
                                                                                       MessageType messageType) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        String messageId = StringUtils.createShortUid();
        if (!channel.getInMediation().get() || channel.findMediator().isEmpty()) {
            return super.sendPrivateChatMessage(messageId, text, quotedMessage, channel, myUserIdentity, channel.getPeer(), messageType);
        }

        // If mediation has been activated we send all messages to the 2 other peers
        UserProfile receiver1, receiver2;
        if (channel.isMediator()) {
           *//* receiver1 = channel.getPeerOrTrader1();
            receiver2 = channel.getMyUserProfileOrTrader2();*//*
//todo
            receiver1 = channel.getPeer();
            receiver2 = channel.findMediator().get();
        } else {
            receiver1 = channel.getPeer();
            receiver2 = channel.findMediator().get();
        }

        UserProfile senderUserProfile = myUserIdentity.getUserProfile();
        NetworkIdWithKeyPair senderNodeIdAndKeyPair = myUserIdentity.getNodeIdAndKeyPair();
        long date = new Date().getTime();
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.findMediator() : Optional.empty();
        PrivateTradeChatMessage message1 = new PrivateTradeChatMessage(
                messageId,
                channel.getChannelName(),
                senderUserProfile,
                receiver1.getId(),
                text,
                quotedMessage,
                date,
                false,
                mediator,
                messageType,
                Optional.empty());

        CompletableFuture<NetworkService.SendMessageResult> sendFuture1 = networkService.confidentialSend(message1,
                receiver1.getNetworkId(),
                senderNodeIdAndKeyPair);

        PrivateTradeChatMessage message2 = new PrivateTradeChatMessage(
                messageId,
                channel.getChannelName(),
                senderUserProfile,
                receiver2.getId(),
                text,
                quotedMessage,
                date,
                false,
                mediator,
                messageType,
                Optional.empty());
        CompletableFuture<NetworkService.SendMessageResult> sendFuture2 = networkService.confidentialSend(message2,
                receiver2.getNetworkId(),
                senderNodeIdAndKeyPair);

        // We only add one message to avoid duplicates (receiverId is different)
        addMessage(message1, channel);

        // We do not use the SendMessageResult yet, so we simply return the first. 
        // If it becomes relevant we would need to change the API of the method.
        return CompletableFutureUtils.allOf(sendFuture1, sendFuture2)
                .thenApply(list -> list.get(0));
    }*/

  /*  public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String text,
                                                                                      Optional<Quotation> quotedMessage,
                                                                                      PrivateTradeChannel channel,
                                                                                      Optional<TradeChatOffer> tradeChatOffer) {
        PrivateTradeChatMessage chatMessage = createNewPrivateTradeChatMessage(
                StringUtils.createShortUid(),
                channel,
                channel.getMyUserIdentity().getUserProfile(),
                channel.getPeer().getId(),
                text,
                quotedMessage,
                new Date().getTime(),
                false,
                MessageType.TEXT,
                tradeChatOffer);
        addMessage(chatMessage, channel);
        NetworkId receiverNetworkId = channel.getPeer().getNetworkId();
        NetworkIdWithKeyPair senderNetworkIdWithKeyPair = channel.getMyUserIdentity().getNodeIdAndKeyPair();
        return networkService.confidentialSend(chatMessage, receiverNetworkId, senderNetworkIdWithKeyPair);
    }
*/

}