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

package bisq.offer;

import bisq.account.settlement.BitcoinSettlement;
import bisq.account.settlement.FiatSettlement;
import bisq.account.settlement.Settlement;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.offer.options.ListingOption;
import bisq.account.protocol.SwapProtocolType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OfferService {
    private final NetworkService networkService;
    private final IdentityService identityService;

    public OfferService(NetworkService networkService, IdentityService identityService) {
        this.networkService = networkService;
        this.identityService = identityService;
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        return future;
    }

    public void shutdown() {
    }

    public CompletableFuture<Offer> createOffer(Market selectedMarket,
                                                Direction direction,
                                                Monetary baseSideAmount,
                                                Monetary quoteSideAmount,
                                                Quote fixPrice,
                                                SwapProtocolType selectedProtocolTyp,
                                                Settlement.Method selectedBaseSideSettlementMethod,
                                                Settlement.Method selectedQuoteSideSettlementMethod) {
        String offerId = StringUtils.createUid();
        return identityService.getOrCreateIdentity(offerId).thenApply(identity ->
        {
            log.error("identity {}", identity);
            NetworkId makerNetworkId = identity.networkId();
            ArrayList<SwapProtocolType> protocolTypes = new ArrayList<>(List.of(selectedProtocolTyp));

            ArrayList<Settlement<? extends Settlement.Method>> baseSettlements = new ArrayList<>(List.of(BitcoinSettlement.BTC_MAINCHAIN));
            ArrayList<Settlement<? extends Settlement.Method>> quoteSettlements = new ArrayList<>(List.of(FiatSettlement.ZELLE));
            HashSet<ListingOption> listingOptions = new HashSet<>();

            //todo serialization does not work correctly.... 
            baseSettlements = new ArrayList<>();
            quoteSettlements = new ArrayList<>();
            listingOptions = null;

            FixPrice priceSpec = new FixPrice(fixPrice.getValue());

            ArrayList<SettlementSpec> baseSideSettlementSpecs = new ArrayList<>();
            ArrayList<SettlementSpec> quoteSideSettlementSpecs = new ArrayList<>();

            
            return new Offer(offerId,
                    new Date().getTime(),
                    makerNetworkId,
                    selectedMarket,
                    direction,
                    baseSideAmount.getValue(),
                    priceSpec,
                    protocolTypes,
                    baseSideSettlementSpecs,
                    quoteSideSettlementSpecs,
                    listingOptions
            );
        });
    }

    public CompletableFuture<CompletableFuture<List<CompletableFuture<BroadcastResult>>>> publishOffer(Offer offer) {
        return identityService.getOrCreateIdentity(offer.getId())
                .thenApply(identity -> {
                    log.error("identity {}", identity);
                    NetworkIdWithKeyPair nodeIdAndKeyPair = identity.getNodeIdAndKeyPair();
                    return networkService.addData(offer, nodeIdAndKeyPair);
                });
    }
}
