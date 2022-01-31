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

import bisq.account.accounts.Account;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService.BroadCastDataResult;
import bisq.offer.options.ListingOption;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class OfferService {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final OpenOfferService openOfferService;

    public OfferService(NetworkService networkService, IdentityService identityService, OpenOfferService openOfferService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.openOfferService = openOfferService;
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);

        //todo
        Scheduler.run(()->openOfferService.getOpenOffers().forEach(openOffer -> publishOffer(openOffer.getOffer()))).after(1000);
        
        return future;
    }

    public void shutdown() {
    }

    public CompletableFuture<Offer> createOffer(Market selectedMarket,
                                                Direction direction,
                                                Monetary baseSideAmount,
                                                Quote fixPrice,
                                                SwapProtocolType selectedProtocolTyp,
                                                List<Account<? extends SettlementMethod>> selectedBaseSideAccounts,
                                                List<Account<? extends SettlementMethod>> selectedQuoteSideAccounts,
                                                List<SettlementMethod> selectedBaseSideSettlementMethods,
                                                List<SettlementMethod> selectedQuoteSideSettlementMethods) {
        String offerId = StringUtils.createUid();
        return identityService.getOrCreateIdentity(offerId).thenApply(identity -> {
            NetworkId makerNetworkId = identity.networkId();
            List<SwapProtocolType> protocolTypes = new ArrayList<>(List.of(selectedProtocolTyp));

            FixPrice priceSpec = new FixPrice(fixPrice.getValue());

            List<SettlementSpec> baseSideSettlementSpecs;
            if (!selectedBaseSideAccounts.isEmpty()) {
                baseSideSettlementSpecs = selectedBaseSideAccounts.stream()
                        .map(e -> new SettlementSpec(e.getSettlementMethod().name(), e.getId()))
                        .collect(Collectors.toList());
            } else {
                baseSideSettlementSpecs = selectedBaseSideSettlementMethods.stream()
                        .map(e -> new SettlementSpec(e.name(), null))
                        .collect(Collectors.toList());
            }
            List<SettlementSpec> quoteSideSettlementSpecs;
            if (!selectedBaseSideAccounts.isEmpty()) {
                quoteSideSettlementSpecs = selectedQuoteSideAccounts.stream()
                        .map(e -> new SettlementSpec(e.getSettlementMethod().name(), e.getId()))
                        .collect(Collectors.toList());
            } else {
                quoteSideSettlementSpecs = selectedQuoteSideSettlementMethods.stream()
                        .map(e -> new SettlementSpec(e.name(), null))
                        .collect(Collectors.toList());
            }

            List<ListingOption> listingOptions = new ArrayList<>();

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

    public CompletableFuture<BroadCastDataResult> publishOffer(Offer offer) {
        openOfferService.add(offer);
        return identityService.getOrCreateIdentity(offer.getId())
                .thenCompose(identity -> {
                    NetworkIdWithKeyPair nodeIdAndKeyPair = identity.getNodeIdAndKeyPair();
                    return networkService.addData(offer, nodeIdAndKeyPair);
                });
    }

    public CompletableFuture<BroadCastDataResult> removeMyOffer(Offer offer) {
        openOfferService.remove(offer);
        return removeMyOfferFromNetwork(offer);
    }

    public CompletableFuture<BroadCastDataResult> removeMyOfferFromNetwork(Offer offer) {
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        return identityService.findActiveIdentity(offer.getId())
                .map(identity -> networkService.removeData(offer, identity.getNodeIdAndKeyPair()))
                .orElse(CompletableFuture.completedFuture(new BroadCastDataResult()));
        // return networkService.removeData(offer, identity.getNodeIdAndKeyPair());
    }

    public CompletableFuture<List<BroadCastDataResult>> removeMyOffersFromNetwork() {
        return CompletableFutureUtils.allOf(openOfferService.getOpenOffers().stream()
                .map(openOffer -> removeMyOfferFromNetwork(openOffer.getOffer()))
                .collect(Collectors.toList()));
    }
}
