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

package bisq.chat.bisqeasy.channel.offerbook;

import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.channel.pub.PublicChatChannelService;
import bisq.chat.message.Citation;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Observable;
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
public class BisqEasyPublicChatChannelService extends PublicChatChannelService<BisqEasyPublicChatMessage, BisqEasyPublicChatChannel, BisqEasyPublicChatChannelStore> {
    @Getter
    private final BisqEasyPublicChatChannelStore persistableStore = new BisqEasyPublicChatChannelStore();
    @Getter
    private final Persistence<BisqEasyPublicChatChannelStore> persistence;
    @Getter
    private final Observable<Integer> numVisibleChannels = new Observable<>(0);

    public BisqEasyPublicChatChannelService(PersistenceService persistenceService,
                                            NetworkService networkService,
                                            UserService userService) {
        super(networkService, userService, ChatChannelDomain.BISQ_EASY);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);

        getVisibleChannelIds().addListener(() -> {
            numVisibleChannels.set(getVisibleChannelIds().size());
        });
    }

    @Override
    public void onPersistedApplied(BisqEasyPublicChatChannelStore persisted) {
    }

    public void joinChannel(BisqEasyPublicChatChannel channel) {
        getVisibleChannelIds().add(channel.getId());
        persist();
    }

    @Override
    public void leaveChannel(BisqEasyPublicChatChannel channel) {
        getVisibleChannelIds().remove(channel.getId());
        persist();
    }

    public boolean isVisible(BisqEasyPublicChatChannel channel) {
        return getVisibleChannelIds().contains(channel.getId());
    }

    public ObservableSet<String> getVisibleChannelIds() {
        return persistableStore.getVisibleChannelIds();
    }

    public Set<BisqEasyPublicChatChannel> getVisibleChannels() {
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
    public ObservableArray<BisqEasyPublicChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public Optional<BisqEasyPublicChatChannel> findChannel(Market market) {
        return findChannel(BisqEasyPublicChatChannel.createId(market));
    }

    @Override
    public Optional<BisqEasyPublicChatChannel> getDefaultChannel() {
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
                                                          BisqEasyPublicChatChannel publicChannel,
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
            BisqEasyPublicChatChannel defaultChannel = new BisqEasyPublicChatChannel(MarketRepository.getDefault());
            joinChannel(defaultChannel);
            maybeAddPublicTradeChannel(defaultChannel);

            List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
            allMarkets.remove(MarketRepository.getDefault());
            allMarkets.forEach(market -> maybeAddPublicTradeChannel(new BisqEasyPublicChatChannel(market)));
        } else if (getNumVisibleChannels().get() == 0) {
            findChannel(MarketRepository.getDefault()).ifPresent(this::joinChannel);
        }
    }

    private void maybeAddPublicTradeChannel(BisqEasyPublicChatChannel channel) {
        if (!getChannels().contains(channel)) {
            getChannels().add(channel);
        }
    }
}