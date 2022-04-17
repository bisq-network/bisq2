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

package bisq.social.offer;

import bisq.common.monetary.Market;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.social.chat.ChatService;
import bisq.social.chat.MarketChannel;
import bisq.social.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class MarketChatOfferService implements PersistenceClient<MarketChatOfferStore> {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;
    private final MarketChatOfferStore persistableStore = new MarketChatOfferStore();
    private final Persistence<MarketChatOfferStore> persistence;

    public MarketChatOfferService(NetworkService networkService,
                                  IdentityService identityService,
                                  ChatService chatService,
                                  PersistenceService persistenceService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.chatService = chatService;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<DataService.BroadCastDataResult> publishMarketChatOffer(Market selectedMarket,
                                                                                     long baseSideAmount,
                                                                                     Set<String> selectedPaymentMethods,
                                                                                     String makersTradeTerms) {
        UserProfile userProfile = chatService.getUserProfileService().getSelectedUserProfile();
        MarketChatOffer marketChatOffer = new MarketChatOffer(baseSideAmount,
                selectedMarket.quoteCurrencyCode(),
                selectedPaymentMethods,
                makersTradeTerms);
        MarketChannel marketChannel = chatService.findMarketChannel(selectedMarket).orElseThrow();
        return chatService.publishMarketChatOffer(marketChatOffer, marketChannel, userProfile);
    }
}
