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

package bisq.social.intent;

import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService.BroadCastDataResult;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUser;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

// Note: will get probably removed
@Slf4j
public class TradeIntentService {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final TradeIntentListingsService tradeIntentListingsService;
    private final ChatService chatService;

    public TradeIntentService(NetworkService networkService,
                              IdentityService identityService,
                              TradeIntentListingsService tradeIntentListingsService,
                              ChatService chatService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.tradeIntentListingsService = tradeIntentListingsService;
        this.chatService = chatService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        // republishMyTradeIntents();
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<TradeIntent> createTradeIntent(String ask, String bid) {
        String tradeIntentId = StringUtils.createUid();
        return identityService.getOrCreateIdentity(tradeIntentId).thenApply(identity -> {
            NetworkId makerNetworkId = identity.networkId();
            String userName = chatService.findUserName(tradeIntentId).orElse("Maker@" + StringUtils.truncate(tradeIntentId));
            ChatUser maker = new ChatUser(makerNetworkId);
            return new TradeIntent(tradeIntentId, maker, ask, bid, new Date().getTime());
        });
    }

    public CompletableFuture<BroadCastDataResult> publishTradeIntent(TradeIntent tradeIntent) {
        // openOfferService.add(tradeIntent);
        return addToNetwork(tradeIntent);
    }

    private void republishMyTradeIntents() {
        // openOfferService.getOpenOffers().forEach(openOffer -> addToNetwork(openOffer.getOffer()));
    }

    public CompletableFuture<BroadCastDataResult> removeMyTradeIntent(TradeIntent tradeIntent) {
        // openOfferService.remove(tradeIntent);
        return removeFromNetwork(tradeIntent);
    }

    public CompletableFuture<BroadCastDataResult> addToNetwork(TradeIntent tradeIntent) {
        return identityService.getOrCreateIdentity(tradeIntent.id())
                .thenCompose(identity -> {
                    NetworkIdWithKeyPair nodeIdAndKeyPair = identity.getNodeIdAndKeyPair();
                    return networkService.addData(tradeIntent, nodeIdAndKeyPair);
                });
    }

    public CompletableFuture<BroadCastDataResult> removeFromNetwork(TradeIntent tradeIntent) {
        return identityService.findActiveIdentity(tradeIntent.id())
                .map(identity -> networkService.removeData(tradeIntent, identity.getNodeIdAndKeyPair()))
                .orElse(CompletableFuture.completedFuture(new BroadCastDataResult()));
    }
}
