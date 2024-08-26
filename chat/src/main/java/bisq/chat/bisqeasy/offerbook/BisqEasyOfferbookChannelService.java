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
import bisq.chat.reactions.BisqEasyOfferbookMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class BisqEasyOfferbookChannelService extends PublicChatChannelService<BisqEasyOfferbookMessage,
        BisqEasyOfferbookChannel, BisqEasyOfferbookChannelStore, BisqEasyOfferbookMessageReaction> {
    @Getter
    private final BisqEasyOfferbookChannelStore persistableStore = new BisqEasyOfferbookChannelStore();
    @Getter
    private final Persistence<BisqEasyOfferbookChannelStore> persistence;

    public BisqEasyOfferbookChannelService(PersistenceService persistenceService,
                                           NetworkService networkService,
                                           UserService userService) {
        super(networkService, userService, ChatChannelDomain.BISQ_EASY_OFFERBOOK);
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.CACHE, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        handleAuthenticatedDataAdded(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof BisqEasyOfferbookMessage) {
            processRemovedMessage((BisqEasyOfferbookMessage) distributedData);
        } else if (distributedData instanceof BisqEasyOfferbookMessageReaction) {
            processRemovedReaction((BisqEasyOfferbookMessageReaction) distributedData);
        }
    }

    @Override
    protected void handleAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof BisqEasyOfferbookMessage) {
            processAddedMessage((BisqEasyOfferbookMessage) distributedData);
        } else if (distributedData instanceof BisqEasyOfferbookMessageReaction) {
            processAddedReaction((BisqEasyOfferbookMessageReaction) distributedData);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API 
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ObservableSet<BisqEasyOfferbookChannel> getChannels() {
        return persistableStore.getChannels();
    }

    public Optional<BisqEasyOfferbookChannel> findChannel(Market market) {
        return findChannel(BisqEasyOfferbookChannel.createId(market));
    }

    @Override
    public Optional<BisqEasyOfferbookChannel> getDefaultChannel() {
        Market defaultMarket = MarketRepository.getDefault();
        return getChannels().stream()
                .filter(channel -> defaultMarket.equals(channel.getMarket()))
                .findAny()
                .or(super::getDefaultChannel);
    }

    public Optional<BisqEasyOfferbookMessage> findMessageByOffer(BisqEasyOffer offer) {
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
    protected BisqEasyOfferbookMessage createChatMessage(String text,
                                                         Optional<Citation> citation,
                                                         BisqEasyOfferbookChannel publicChannel,
                                                         UserProfile userProfile) {
        return new BisqEasyOfferbookMessage(publicChannel.getId(),
                userProfile.getId(),
                Optional.empty(),
                Optional.of(text),
                citation,
                new Date().getTime(),
                false);
    }

    @Override
    protected BisqEasyOfferbookMessage createEditedChatMessage(BisqEasyOfferbookMessage originalChatMessage,
                                                               String editedText,
                                                               UserProfile userProfile) {
        return new BisqEasyOfferbookMessage(originalChatMessage.getChannelId(),
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
            BisqEasyOfferbookChannel defaultChannel = new BisqEasyOfferbookChannel(MarketRepository.getDefault());
            maybeAddPublicTradeChannel(defaultChannel);

            List<Market> allMarkets = MarketRepository.getAllFiatMarkets();
            allMarkets.remove(MarketRepository.getDefault());
            allMarkets.forEach(market -> maybeAddPublicTradeChannel(new BisqEasyOfferbookChannel(market)));
        }
    }

    @Override
    protected BisqEasyOfferbookMessageReaction createChatMessageReaction(BisqEasyOfferbookMessage message,
                                                                         Reaction reaction,
                                                                         UserIdentity userIdentity) {
        return new BisqEasyOfferbookMessageReaction(
                StringUtils.createUid(),
                userIdentity.getId(),
                message.getChannelId(),
                message.getChatChannelDomain(),
                message.getId(),
                reaction.ordinal(),
                new Date().getTime());
    }

    private void maybeAddPublicTradeChannel(BisqEasyOfferbookChannel channel) {
        if (!getChannels().contains(channel)) {
            getChannels().add(channel);
        }
    }
}
