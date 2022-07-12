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

package bisq.chat.channels;

import bisq.chat.ChannelNotificationType;
import bisq.chat.messages.ChatMessage;
import bisq.chat.messages.PublicTradeChatMessage;
import bisq.chat.messages.Quotation;
import bisq.common.application.Service;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.ObservableSet;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PublicTradeChannelService implements PersistenceClient<PublicTradeChannelStore>,
        DataService.Listener, Service {
    @Getter
    private final PublicTradeChannelStore persistableStore = new PublicTradeChannelStore();
    @Getter
    private final Persistence<PublicTradeChannelStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;

    public PublicTradeChannelService(PersistenceService persistenceService,
                                     NetworkService networkService,
                                     UserIdentityService userIdentityService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
    }

    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAllAuthenticatedPayload().forEach(this::onAuthenticatedDataAdded));
        maybeAddDefaultChannels();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicTradeChatMessage) {
            PublicTradeChatMessage message = (PublicTradeChatMessage) distributedData;
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> addPublicTradeChatMessage(message, channel));
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicTradeChatMessage) {
            PublicTradeChatMessage message = (PublicTradeChatMessage) distributedData;
            findPublicTradeChannel(message.getChannelId())
                    .ifPresent(channel -> removePublicTradeChatMessage(message, channel));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<PublicTradeChannel> showPublicTradeChannel(Market market) {
        return findPublicTradeChannel(PublicTradeChannel.getId(market))
                .map(channel -> {
                    channel.setVisible(true);
                    persist();
                    return channel;
                });
    }

    public void showPublicTradeChannel(PublicTradeChannel channel) {
        channel.setVisible(true);
        persist();
    }

    public void hidePublicTradeChannel(PublicTradeChannel channel) {
        channel.setVisible(false);
        persist();
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatTextMessage(String text,
                                                                                          Optional<Quotation> quotedMessage,
                                                                                          PublicTradeChannel publicTradeChannel,
                                                                                          UserIdentity userIdentity) {
        UserProfile userProfile = userIdentity.getUserProfile();
        PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(publicTradeChannel.getId(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
        return publish(userIdentity, userProfile, chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishPublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                            UserIdentity userIdentity) {
        return publish(userIdentity, userIdentity.getUserProfile(), chatMessage);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishEditedTradeChatMessage(PublicTradeChatMessage originalChatMessage,
                                                                                            String editedText,
                                                                                            UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(originalChatMessage, nodeIdAndKeyPair)
                .thenCompose(result -> {
                    // We do not support editing the MarketChatOffer directly but remove it and replace it with 
                    // the edited text.
                    UserProfile userProfile = userIdentity.getUserProfile();
                    PublicTradeChatMessage chatMessage = new PublicTradeChatMessage(originalChatMessage.getChannelId(),
                            userProfile.getId(),
                            Optional.empty(),
                            Optional.of(editedText),
                            originalChatMessage.getQuotation(),
                            originalChatMessage.getDate(),
                            true);
                    return publish(userIdentity, userProfile, chatMessage);
                });
    }

    public CompletableFuture<DataService.BroadCastDataResult> deletePublicTradeChatMessage(PublicTradeChatMessage chatMessage,
                                                                                           UserIdentity userIdentity) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return networkService.removeAuthenticatedData(chatMessage, nodeIdAndKeyPair);
    }

    public Optional<PublicTradeChannel> findPublicTradeChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    private void addPublicTradeChatMessage(PublicTradeChatMessage message, PublicTradeChannel channel) {
        channel.addChatMessage(message);
        persist();
    }

    private void removePublicTradeChatMessage(PublicTradeChatMessage message, PublicTradeChannel channel) {
        channel.removeChatMessage(message);
        persist();
    }

    public ObservableSet<PublicTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Misc
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void setNotificationSetting(Channel<? extends ChatMessage> channel, ChannelNotificationType channelNotificationType) {
        channel.getChannelNotificationType().set(channelNotificationType);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<DataService.BroadCastDataResult> publish(UserIdentity userIdentity,
                                                                       UserProfile userProfile,
                                                                       DistributedData distributedData) {
        NetworkIdWithKeyPair nodeIdAndKeyPair = userIdentity.getNodeIdAndKeyPair();
        return userIdentityService.maybePublicUserProfile(userProfile, nodeIdAndKeyPair)
                .thenCompose(result -> networkService.publishAuthenticatedData(distributedData, nodeIdAndKeyPair));
    }


   /* private boolean isValidProofOfWorkOrChatUserNotFound(ChatMessage message) {
        // In case we don't find the chat user we still return true as it might be that the chat user gets added later.
        // In that case we check again if the chat user has a valid proof of work.
        return findChatUser(message.getAuthorId())
                .map(chatUser -> hasAuthorValidProofOfWork(chatUser.getProofOfWork()))
                .orElse(true);
    }*/

    private void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }

        PublicTradeChannel defaultChannel = new PublicTradeChannel(MarketRepository.getDefault(), true);
        maybeAddPublicTradeChannel(defaultChannel);
        List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
        allMarkets.remove(MarketRepository.getDefault());
        allMarkets.forEach(market ->
                maybeAddPublicTradeChannel(new PublicTradeChannel(market, false)));
    }

    private void maybeAddPublicTradeChannel(PublicTradeChannel channel) {
        if (!getChannels().contains(channel)) {
            getChannels().add(channel);
        }
    }
}