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

package bisq.chat.bisqeasy.channel.pub;

import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
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
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
                                            UserIdentityService userIdentityService,
                                            UserProfileService userProfileService) {
        super(networkService, userIdentityService, userProfileService, ChatChannelDomain.TRADE);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    public void showChannel(BisqEasyPublicChatChannel channel) {
        getVisibleChannelNames().add(channel.getChannelName());
        numVisibleChannels.set(getVisibleChannelNames().size());
        persist();
    }

    public void hidePublicTradeChannel(BisqEasyPublicChatChannel channel) {
        getVisibleChannelNames().remove(channel.getChannelName());
        numVisibleChannels.set(getVisibleChannelNames().size());
        persist();
    }

    public boolean isVisible(BisqEasyPublicChatChannel channel) {
        return getVisibleChannelNames().contains(channel.getChannelName());
    }

    public ObservableSet<String> getVisibleChannelNames() {
        return persistableStore.getVisibleChannelNames();
    }

    public Set<BisqEasyPublicChatChannel> getVisibleChannels() {
        return getChannels().stream().filter(channel -> getVisibleChannelNames().contains(channel.getChannelName())).collect(Collectors.toSet());
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
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<BisqEasyPublicChatChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected BisqEasyPublicChatMessage createChatMessage(String text,
                                                          Optional<Citation> citation,
                                                          BisqEasyPublicChatChannel publicChannel,
                                                          UserProfile userProfile) {
        return new BisqEasyPublicChatMessage(publicChannel.getChannelName(),
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
        return new BisqEasyPublicChatMessage(originalChatMessage.getChannelName(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(editedText),
                originalChatMessage.getCitation(),
                originalChatMessage.getDate(),
                true);
    }

    @Override
    protected void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }

        BisqEasyPublicChatChannel defaultChannel = new BisqEasyPublicChatChannel(MarketRepository.getDefault());
        showChannel(defaultChannel);
        maybeAddPublicTradeChannel(defaultChannel);
        List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
        allMarkets.remove(MarketRepository.getDefault());
        allMarkets.forEach(market -> maybeAddPublicTradeChannel(new BisqEasyPublicChatChannel(market)));
    }

    private void maybeAddPublicTradeChannel(BisqEasyPublicChatChannel channel) {
        if (!getChannels().contains(channel)) {
            channel.getChatChannelNotificationType().addObserver(value -> persist());
            getChannels().add(channel);
        }
    }
}