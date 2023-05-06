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

import bisq.chat.bisqeasy.message.PublicBisqEasyOfferChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.pub.PublicChatChannelService;
import bisq.chat.message.Quotation;
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
public class PublicTradeChannelService extends PublicChatChannelService<PublicBisqEasyOfferChatMessage, PublicTradeChannel, PublicTradeChannelStore> {
    @Getter
    private final PublicTradeChannelStore persistableStore = new PublicTradeChannelStore();
    @Getter
    private final Persistence<PublicTradeChannelStore> persistence;
    @Getter
    private final Observable<Integer> numVisibleChannels = new Observable<>(0);

    public PublicTradeChannelService(PersistenceService persistenceService,
                                     NetworkService networkService,
                                     UserIdentityService userIdentityService,
                                     UserProfileService userProfileService) {
        super(networkService, userIdentityService, userProfileService, ChatChannelDomain.TRADE);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    public void showChannel(PublicTradeChannel channel) {
        getVisibleChannelNames().add(channel.getChannelName());
        numVisibleChannels.set(getVisibleChannelNames().size());
        persist();
    }

    public void hidePublicTradeChannel(PublicTradeChannel channel) {
        getVisibleChannelNames().remove(channel.getChannelName());
        numVisibleChannels.set(getVisibleChannelNames().size());
        persist();
    }

    public boolean isVisible(PublicTradeChannel channel) {
        return getVisibleChannelNames().contains(channel.getChannelName());
    }

    public ObservableSet<String> getVisibleChannelNames() {
        return persistableStore.getVisibleChannelNames();
    }

    public Set<PublicTradeChannel> getVisibleChannels() {
        return getChannels().stream().filter(channel -> getVisibleChannelNames().contains(channel.getChannelName())).collect(Collectors.toSet());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicBisqEasyOfferChatMessage) {
            processAddedMessage((PublicBisqEasyOfferChatMessage) distributedData);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof PublicBisqEasyOfferChatMessage) {
            processRemovedMessage((PublicBisqEasyOfferChatMessage) distributedData);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PublicChannelService 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableArray<PublicTradeChannel> getChannels() {
        return persistableStore.getChannels();
    }

    @Override
    protected PublicBisqEasyOfferChatMessage createChatMessage(String text,
                                                               Optional<Quotation> quotedMessage,
                                                               PublicTradeChannel publicChannel,
                                                               UserProfile userProfile) {
        return new PublicBisqEasyOfferChatMessage(publicChannel.getChannelName(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(text),
                quotedMessage,
                new Date().getTime(),
                false);
    }

    @Override
    protected PublicBisqEasyOfferChatMessage createEditedChatMessage(PublicBisqEasyOfferChatMessage originalChatMessage,
                                                                     String editedText,
                                                                     UserProfile userProfile) {
        return new PublicBisqEasyOfferChatMessage(originalChatMessage.getChannelName(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(editedText),
                originalChatMessage.getQuotation(),
                originalChatMessage.getDate(),
                true);
    }

    @Override
    protected void maybeAddDefaultChannels() {
        if (!getChannels().isEmpty()) {
            return;
        }

        PublicTradeChannel defaultChannel = new PublicTradeChannel(MarketRepository.getDefault());
        showChannel(defaultChannel);
        maybeAddPublicTradeChannel(defaultChannel);
        List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
        allMarkets.remove(MarketRepository.getDefault());
        allMarkets.forEach(market -> maybeAddPublicTradeChannel(new PublicTradeChannel(market)));
    }

    private void maybeAddPublicTradeChannel(PublicTradeChannel channel) {
        if (!getChannels().contains(channel)) {
            channel.getChatChannelNotificationType().addObserver(value -> persist());
            getChannels().add(channel);
        }
    }
}