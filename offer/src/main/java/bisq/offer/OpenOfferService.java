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
import bisq.common.observable.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.offer.options.ListingOption;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.DataService.BroadCastDataResult;
import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class OpenOfferService implements PersistenceClient<OpenOfferStore> {
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("OpenOfferService.dispatcher");

    public interface Listener {
        void onOpenOfferAdded(OpenOffer openOffer);

        void onOpenOfferRemoved(OpenOffer openOffer);
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    @Getter
    private final OpenOfferStore persistableStore = new OpenOfferStore();
    @Getter
    private final Persistence<OpenOfferStore> persistence;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public OpenOfferService(NetworkService networkService,
                            IdentityService identityService,
                            PersistenceService persistenceService) {
        this.networkService = networkService;
        this.identityService = identityService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        republishMyOffers();
        return CompletableFuture.completedFuture(true);
    }


    public CompletableFuture<List<BroadCastDataResult>> shutdown() {
        return CompletableFutureUtils.allOf(getOpenOffers().stream()
                .map(openOffer -> removeFromNetwork(openOffer.getOffer())));
    }

    public ObservableSet<OpenOffer> getOpenOffers() {
        return persistableStore.getOpenOffers();
    }

    public void add(Offer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        persistableStore.add(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferAdded(openOffer)), DISPATCHER);
        persist();
    }

    public void remove(Offer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        persistableStore.remove(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferRemoved(openOffer)), DISPATCHER);
        persist();
    }

    public Optional<OpenOffer> findOpenOffer(String offerId) {
        return persistableStore.getOpenOffers().stream()
                .filter(openOffer -> openOffer.getOffer().getId().equals(offerId))
                .findAny();
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
        add(offer);
        return addToNetwork(offer);
    }

    private void republishMyOffers() {
        getOpenOffers().forEach(openOffer -> addToNetwork(openOffer.getOffer()));
    }

    public CompletableFuture<BroadCastDataResult> removeMyOffer(Offer offer) {
        remove(offer);
        return removeFromNetwork(offer);
    }

    public CompletableFuture<BroadCastDataResult> addToNetwork(Offer offer) {
        return identityService.getOrCreateIdentity(offer.getId())
                .thenCompose(identity -> networkService.addData(offer, identity.getNodeIdAndKeyPair()));
    }

    public CompletableFuture<BroadCastDataResult> removeFromNetwork(Offer offer) {
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        return identityService.findActiveIdentity(offer.getId())
                .map(identity -> networkService.removeData(offer, identity.getNodeIdAndKeyPair()))
                .orElse(CompletableFuture.completedFuture(new BroadCastDataResult()));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
