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

package bisq.offer.poc;

import bisq.account.accounts.Account;
import bisq.account.protocol_type.ProtocolType;
import bisq.account.settlement.Settlement;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkService;
import bisq.offer.Direction;
import bisq.offer.SettlementSpec;
import bisq.offer.offer_options.OfferOption;
import bisq.offer.price_spec.FixPriceSpec;
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

    public interface Listener {
        void onOpenOfferAdded(OpenOffer openOffer);

        void onOpenOfferRemoved(OpenOffer openOffer);
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ExecutorService executorService = ExecutorFactory.newSingleThreadExecutor("OpenOfferService.dispatcher");
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
        log.info("shutdown");
        return CompletableFutureUtils.allOf(getOpenOffers().stream()
                        .map(openOffer -> removeFromNetwork(openOffer.getOffer())))
                .thenApply(removeOfferBroadCastDataResults -> {
                    executorService.shutdownNow();
                    return removeOfferBroadCastDataResults;
                });
    }

    public ObservableSet<OpenOffer> getOpenOffers() {
        return persistableStore.getOpenOffers();
    }

    public void add(PocOffer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        persistableStore.add(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferAdded(openOffer)), executorService);
        persist();
    }

    public void remove(PocOffer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        persistableStore.remove(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferRemoved(openOffer)), executorService);
        persist();
    }

    public Optional<OpenOffer> findOpenOffer(String offerId) {
        return persistableStore.getOpenOffers().stream()
                .filter(openOffer -> openOffer.getOffer().getId().equals(offerId))
                .findAny();
    }

    public CompletableFuture<PocOffer> createOffer(Market selectedMarket,
                                                   Direction direction,
                                                   Monetary baseSideAmount,
                                                   Quote fixPrice,
                                                   ProtocolType selectedProtocolTyp,
                                                   List<Account<?, ? extends Settlement<?>>> selectedBaseSideAccounts,
                                                   List<Account<?, ? extends Settlement<?>>> selectedQuoteSideAccounts,
                                                   List<Settlement.Method> selectedBaseSideSettlementMethods,
                                                   List<Settlement.Method> selectedQuoteSideSettlementMethods) {
        String offerId = StringUtils.createUid();
        return identityService.getOrCreateIdentity(offerId).thenApply(identity -> {
            NetworkId makerNetworkId = identity.getNetworkId();
            List<ProtocolType> protocolTypes = new ArrayList<>(List.of(selectedProtocolTyp));

            FixPriceSpec priceSpec = new FixPriceSpec(fixPrice.getValue());

            List<SettlementSpec> baseSideSettlementSpecs;
            if (!selectedBaseSideAccounts.isEmpty()) {
                baseSideSettlementSpecs = selectedBaseSideAccounts.stream()
                        .map(e -> new SettlementSpec(e.getSettlement().getSettlementMethodName(), Optional.of(e.getAccountName())))
                        .collect(Collectors.toList());
            } else {
                baseSideSettlementSpecs = selectedBaseSideSettlementMethods.stream()
                        .map(e -> new SettlementSpec(e.name(), Optional.empty()))
                        .collect(Collectors.toList());
            }
            List<SettlementSpec> quoteSideSettlementSpecs;
            if (!selectedBaseSideAccounts.isEmpty()) {
                quoteSideSettlementSpecs = selectedQuoteSideAccounts.stream()
                        .map(e -> new SettlementSpec(e.getSettlement().getSettlementMethodName(), Optional.of(e.getAccountName())))
                        .collect(Collectors.toList());
            } else {
                quoteSideSettlementSpecs = selectedQuoteSideSettlementMethods.stream()
                        .map(e -> new SettlementSpec(e.name(), Optional.empty()))
                        .collect(Collectors.toList());
            }

            List<OfferOption> offerOptions = new ArrayList<>();

            return new PocOffer(offerId,
                    new Date().getTime(),
                    makerNetworkId,
                    selectedMarket,
                    direction,
                    baseSideAmount.getValue(),
                    priceSpec,
                    protocolTypes,
                    baseSideSettlementSpecs,
                    quoteSideSettlementSpecs,
                    offerOptions
            );
        });
    }

    public CompletableFuture<BroadCastDataResult> publishOffer(PocOffer offer) {
        add(offer);
        return addToNetwork(offer);
    }

    private void republishMyOffers() {
        getOpenOffers().forEach(openOffer -> addToNetwork(openOffer.getOffer()));
    }

    public CompletableFuture<BroadCastDataResult> removeMyOffer(PocOffer offer) {
        remove(offer);
        return removeFromNetwork(offer);
    }

    public CompletableFuture<BroadCastDataResult> addToNetwork(PocOffer offer) {
        return identityService.getOrCreateIdentity(offer.getId())
                .thenCompose(identity -> networkService.publishAuthenticatedData(offer, identity.getNodeIdAndKeyPair()));
    }

    public CompletableFuture<BroadCastDataResult> removeFromNetwork(PocOffer offer) {
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        return identityService.findActiveIdentity(offer.getId())
                .map(identity -> networkService.removeAuthenticatedData(offer, identity.getNodeIdAndKeyPair()))
                .orElse(CompletableFuture.completedFuture(new BroadCastDataResult()));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
