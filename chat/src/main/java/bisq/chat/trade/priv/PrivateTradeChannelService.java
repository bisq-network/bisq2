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

import bisq.chat.channel.BasePrivateChannelService;
import bisq.chat.channel.ChannelDomain;
import bisq.chat.message.MessageType;
import bisq.chat.message.Quotation;
import bisq.common.data.Pair;
import bisq.common.observable.ObservableArray;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PrivateTradeChannelService extends BasePrivateChannelService<PrivateTradeChatMessage, PrivateTradeChannel, PrivateTradeChannelStore> {
    @Getter
    private final PrivateTradeChannelStore persistableStore = new PrivateTradeChannelStore();
    @Getter
    private final Persistence<PrivateTradeChannelStore> persistence;

    public PrivateTradeChannelService(PersistenceService persistenceService,
                                      NetworkService networkService,
                                      UserIdentityService userIdentityService,
                                      UserProfileService userProfileService,
                                      ProofOfWorkService proofOfWorkService) {
        super(networkService, userIdentityService, userProfileService, proofOfWorkService, ChannelDomain.TRADE);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public void setMediationActivated(PrivateTradeChannel channel, boolean mediationActivated) {
        channel.getInMediation().set(mediationActivated);
        persist();
    }

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof PrivateTradeChatMessage) {
            processMessage((PrivateTradeChatMessage) networkMessage);
        }
    }

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
        // We send the mediator only in the first message to the peer.
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.getMediator() : Optional.empty();
        return new PrivateTradeChatMessage(
                messageId,
                channel.getChannelName(),
                sender,
                receiversId,
                text,
                quotedMessage,
                new Date().getTime(),
                wasEdited,
                mediator,
                messageType);
    }

    @Override
    protected PrivateTradeChannel createNewChannel(UserProfile peer, UserIdentity myUserIdentity) {
        throw new RuntimeException("createNewChannel not supported at PrivateTradeChannelService. " +
                "Use mediatorCreatesNewChannel or traderCreatesNewChannel instead.");
    }

    public PrivateTradeChannel mediatorCreatesNewChannel(UserIdentity myUserIdentity, UserProfile trader1, UserProfile trader2) {
        String channelName = PrivateTradeChannel.createChannelName(new Pair<>(trader1.getId(), trader2.getId()));
        Optional<PrivateTradeChannel> existingChannel = getChannels().stream()
                .filter(channel -> channel.getChannelName().equals(channelName))
                .findAny();
        if (existingChannel.isPresent()) {
            return existingChannel.get();
        }

        PrivateTradeChannel channel = PrivateTradeChannel.createByMediator(myUserIdentity, trader1, trader2);
        channel.getChannelNotificationType().addObserver(value -> persist());
        getChannels().add(channel);
        persist();
        return channel;
    }

    public PrivateTradeChannel traderCreatesNewChannel(UserIdentity myUserIdentity, UserProfile peersUserProfile, Optional<UserProfile> mediator) {
        String channelName = PrivateTradeChannel.createChannelName(new Pair<>(myUserIdentity.getUserProfile().getId(), peersUserProfile.getId()));
        Optional<PrivateTradeChannel> existingChannel = getChannels().stream()
                .filter(channel -> channel.getChannelName().equals(channelName))
                .findAny();
        if (existingChannel.isPresent()) {
            return existingChannel.get();
        }

        PrivateTradeChannel channel = PrivateTradeChannel.createByTrader(myUserIdentity, peersUserProfile, mediator);
        channel.getChannelNotificationType().addObserver(value -> persist());
        getChannels().add(channel);
        persist();
        return channel;
    }

    public void leaveChannel(PrivateTradeChannel channel) {
        sendLeaveMessage(channel)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Sending leave channel message failed.");
                    }
                    getChannels().remove(channel);
                    persist();
                });
    }

    @Override
    public ObservableArray<PrivateTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected void processMessage(PrivateTradeChatMessage message) {
        if (!userIdentityService.isUserIdentityPresent(message.getAuthorId())) {
            userIdentityService.findUserIdentity(message.getReceiversId())
                    .flatMap(myUserIdentity -> findChannelForMessage(message)
                            .or(() -> {
                                if (message.getMessageType() == MessageType.LEAVE) {
                                    return Optional.empty();
                                } else if (userProfileService.isChatUserIgnored(message.getSender())) {
                                    return Optional.empty();
                                } else {
                                    return Optional.of(traderCreatesNewChannel(myUserIdentity,
                                            message.getSender(),
                                            message.getMediator()));
                                }
                            }))
                    .ifPresent(channel -> {
                        addMessage(message, channel);
                    });
        }
    }

    public CompletableFuture<NetworkService.SendMessageResult> sendTakeOfferMessage(String text,
                                                                                    Optional<Quotation> quotedMessage,
                                                                                    PrivateTradeChannel channel) {
        return sendPrivateChatMessage(StringUtils.createShortUid(), text, quotedMessage, channel, channel.getMyUserIdentity(), channel.getPeer(), MessageType.TAKE_OFFER);
    }

    @Override
    public CompletableFuture<NetworkService.SendMessageResult> sendPrivateChatMessage(String text,
                                                                                      Optional<Quotation> quotedMessage,
                                                                                      PrivateTradeChannel channel,
                                                                                      MessageType messageType) {
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        String messageId = StringUtils.createShortUid();
        if (!channel.getInMediation().get() || channel.getMediator().isEmpty()) {
            return super.sendPrivateChatMessage(messageId, text, quotedMessage, channel, myUserIdentity, channel.getPeer(), messageType);
        }

        // If mediation has been activated we send all messages to the 2 other peers
        UserProfile receiver1, receiver2;
        if (channel.isMediator()) {
            receiver1 = channel.getPeerOrTrader1();
            receiver2 = channel.getMyUserProfileOrTrader2();
        } else {
            receiver1 = channel.getPeer();
            receiver2 = channel.getMediator().get();
        }

        UserProfile senderUserProfile = myUserIdentity.getUserProfile();
        NetworkIdWithKeyPair senderNodeIdAndKeyPair = myUserIdentity.getNodeIdAndKeyPair();
        long date = new Date().getTime();
        Optional<UserProfile> mediator = channel.getChatMessages().isEmpty() ? channel.getMediator() : Optional.empty();
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
                messageType);

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
                messageType);
        CompletableFuture<NetworkService.SendMessageResult> sendFuture2 = networkService.confidentialSend(message2,
                receiver2.getNetworkId(),
                senderNodeIdAndKeyPair);

        // We only add one message to avoid duplicates (receiverId is different)
        addMessage(message1, channel);

        // We do not use the SendMessageResult yet, so we simply return the first. 
        // If it becomes relevant we would need to change the API of the method.
        return CompletableFutureUtils.allOf(sendFuture1, sendFuture2)
                .thenApply(list -> list.get(0));
    }
}