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

import bisq.common.currency.Market;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.PublicTradeChannel;
import bisq.social.user.ChatUserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class TradeChatOfferService implements PersistenceClient<TradeChatOfferStore> {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;
    private final TradeChatOfferStore persistableStore = new TradeChatOfferStore();
    private final Persistence<TradeChatOfferStore> persistence;

    public TradeChatOfferService(NetworkService networkService,
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

    public CompletableFuture<DataService.BroadCastDataResult> publishTradeChatOffer(Market selectedMarket,
                                                                                    long baseSideAmount,
                                                                                    Set<String> selectedPaymentMethods,
                                                                                    String makersTradeTerms,
                                                                                    long requiredTotalReputationScore) {
        ChatUserIdentity chatUserIdentity = chatService.getChatUserService().getSelectedUserProfile().get();
        TradeChatOffer tradeChatOffer = new TradeChatOffer(baseSideAmount,
                selectedMarket,
                selectedPaymentMethods,
                makersTradeTerms,
                requiredTotalReputationScore);
        PublicTradeChannel publicTradeChannel = chatService.findPublicTradeChannel(selectedMarket.toString()).orElseThrow();
        return chatService.publishTradeChatOffer(tradeChatOffer, publicTradeChannel, chatUserIdentity);
    }
}
