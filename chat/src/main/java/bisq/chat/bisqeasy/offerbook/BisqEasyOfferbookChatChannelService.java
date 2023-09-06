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

package bisq.chat.bisqeasy.offerbook;

import bisq.chat.ChatChannelDomain;
import bisq.chat.Citation;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatChannelService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BisqEasyOfferbookChatChannelService extends PublicChatChannelService<BisqEasyPublicChatMessage, BisqEasyOfferbookChatChannel, BisqEasyOfferbookChatChannelStore> {
    @Getter
    private final BisqEasyOfferbookChatChannelStore persistableStore = new BisqEasyOfferbookChatChannelStore();
    @Getter
    private final Persistence<BisqEasyOfferbookChatChannelStore> persistence;

    public BisqEasyOfferbookChatChannelService(PersistenceService persistenceService,
                                               NetworkService networkService,
                                               UserService userService) {
        super(networkService, userService, ChatChannelDomain.BISQ_EASY_OFFERBOOK);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    @Override
    public void onPersistedApplied(BisqEasyOfferbookChatChannelStore persisted) {
    }

    //todo not useful anymore. consider to remove it
    public void joinChannel(BisqEasyOfferbookChatChannel channel) {
        getVisibleChannelIds().add(channel.getId());
        persist();
    }

    @Override
    public void leaveChannel(BisqEasyOfferbookChatChannel channel) {
        getVisibleChannelIds().remove(channel.getId());
        persist();
    }

    //todo not useful anymore. consider to remove it
    public boolean isVisible(BisqEasyOfferbookChatChannel channel) {
        return getVisibleChannelIds().contains(channel.getId());
    }

    //todo not useful anymore. consider to remove it
    public ObservableSet<String> getVisibleChannelIds() {
        return persistableStore.getVisibleChannelIds();
    }

    //todo not useful anymore. consider to remove it
    public Set<BisqEasyOfferbookChatChannel> getVisibleChannels() {
        return getChannels().stream().filter(channel -> getVisibleChannelIds().contains(channel.getId())).collect(Collectors.toSet());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof BisqEasyPublicChatMessage) {
            processAddedMessage((BisqEasyPublicChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof BisqEasyPublicChatMessage) {
            processRemovedMessage((BisqEasyPublicChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<BisqEasyOfferbookChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public Optional<BisqEasyOfferbookChatChannel> findChannel(Market market) {
        return findChannel(BisqEasyOfferbookChatChannel.createId(market));
    }

    @Override
    public Optional<BisqEasyOfferbookChatChannel> getDefaultChannel() {
        Market defaultMarket = MarketRepository.getDefault();
        return getChannels().stream()
                .filter(this::isVisible)
                .filter(channel -> defaultMarket.equals(channel.getMarket()))
                .findAny()
                .or(super::getDefaultChannel);
    }

    public Optional<BisqEasyPublicChatMessage> findMessageByOffer(BisqEasyOffer offer) {
        return findChannel(offer.getMarket())
                .map(PublicChatChannel::getChatMessages).stream()
                .flatMap(Collection::stream)
                .filter(chatMessage -> offer.equals(chatMessage.getBisqEasyOffer().orElse(null)))
                .findAny();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Protected 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected BisqEasyPublicChatMessage createChatMessage(String text,
                                                          Optional<Citation> citation,
                                                          BisqEasyOfferbookChatChannel publicChannel,
                                                          UserProfile userProfile) {
        return new BisqEasyPublicChatMessage(publicChannel.getId(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(text),
                citation,
                new Date().getTime(),
                false);
    }

    @Override
    protected BisqEasyPublicChatMessage createEditedChatMessage(BisqEasyPublicChatMessage originalChatMessage,
                                                                String editedText,
                                                                UserProfile userProfile) {
        return new BisqEasyPublicChatMessage(originalChatMessage.getChannelId(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(editedText),
                originalChatMessage.getCitation(),
                originalChatMessage.getDate(),
                true);
    }

    @Override
    protected void maybeAddDefaultChannels() {
        if (getChannels().isEmpty()) {
            BisqEasyOfferbookChatChannel defaultChannel = new BisqEasyOfferbookChatChannel(MarketRepository.getDefault());
            joinChannel(defaultChannel);
            maybeAddPublicTradeChannel(defaultChannel);

            List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
            allMarkets.remove(MarketRepository.getDefault());
            allMarkets.forEach(market -> maybeAddPublicTradeChannel(new BisqEasyOfferbookChatChannel(market)));
        }
    }

    private void maybeAddPublicTradeChannel(BisqEasyOfferbookChatChannel channel) {
        if (!getChannels().contains(channel)) {
            getChannels().add(channel);
        }
    }
}