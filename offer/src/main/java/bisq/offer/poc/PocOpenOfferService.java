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
import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.offer.Direction;
import bisq.offer.options.OfferOption;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.persistence.DbSubDirectory;
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

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class PocOpenOfferService implements PersistenceClient<PocOpenOfferStore> {

    public interface Listener {
        void onOpenOfferAdded(PocOpenOffer openOffer);

        void onOpenOfferRemoved(PocOpenOffer openOffer);
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ExecutorService executorService = ExecutorFactory.newSingleThreadExecutor("OpenOfferService.dispatcher");
    @Getter
    private final PocOpenOfferStore persistableStore = new PocOpenOfferStore();
    @Getter
    private final Persistence<PocOpenOfferStore> persistence;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public PocOpenOfferService(NetworkService networkService,
                               IdentityService identityService,
                               PersistenceService persistenceService) {
        this.networkService = networkService;
        this.identityService = identityService;
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        republishMyOffers();
        return CompletableFuture.completedFuture(true);
    }


    public CompletableFuture<List<BroadcastResult>> shutdown() {
        log.info("shutdown");
        return CompletableFutureUtils.allOf(getOpenOffers().stream()
                        .map(openOffer -> removeFromNetwork(openOffer.getOffer())))
                .thenApply(removeOfferBroadCastDataResults -> {
                    executorService.shutdownNow();
                    return removeOfferBroadCastDataResults;
                });
    }

    public ObservableSet<PocOpenOffer> getOpenOffers() {
        return persistableStore.getOpenOffers();
    }

    public void add(PocOffer offer) {
        PocOpenOffer openOffer = new PocOpenOffer(offer);
        persistableStore.add(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferAdded(openOffer)), executorService);
        persist();
    }

    public void remove(PocOffer offer) {
        PocOpenOffer openOffer = new PocOpenOffer(offer);
        persistableStore.remove(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferRemoved(openOffer)), executorService);
        persist();
    }

    public Optional<PocOpenOffer> findOpenOffer(String offerId) {
        return persistableStore.getOpenOffers().stream()
                .filter(openOffer -> openOffer.getOffer().getId().equals(offerId))
                .findAny();
    }

    public CompletableFuture<PocOffer> createOffer(Market selectedMarket,
                                                   Direction direction,
                                                   Monetary baseSideAmount,
                                                   PriceQuote fixPrice,
                                                   TradeProtocolType selectedProtocolTyp,
                                                   List<Account<?, BitcoinPaymentMethod>> selectedBaseSideAccounts,
                                                   List<Account<?, FiatPaymentMethod>> selectedQuoteSideAccounts,
                                                   List<BitcoinPaymentRail> selectedBaseSidePaymentPaymentRails,
                                                   List<FiatPaymentRail> selectedQuoteSidePaymentPaymentRails) {
        String offerId = StringUtils.createUid();
        Identity identity = identityService.findActiveIdentity(offerId).orElseThrow();
        NetworkId makerNetworkId = identity.getNetworkId();
        List<TradeProtocolType> protocolTypes = new ArrayList<>(List.of(selectedProtocolTyp));

        FixPriceSpec priceSpec = new FixPriceSpec(fixPrice);

        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs;
        if (!selectedBaseSideAccounts.isEmpty()) {
            baseSidePaymentMethodSpecs = selectedBaseSideAccounts.stream()
                    .map(e -> new BitcoinPaymentMethodSpec(e.getPaymentMethod(), Optional.of(e.getAccountName())))
                    .collect(Collectors.toList());
        } else {
            baseSidePaymentMethodSpecs = selectedBaseSidePaymentPaymentRails.stream()
                    .map(e -> new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(e), Optional.empty()))
                    .collect(Collectors.toList());
        }
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs;
        if (!selectedBaseSideAccounts.isEmpty()) {
            quoteSidePaymentMethodSpecs = selectedQuoteSideAccounts.stream()
                    .map(e -> new FiatPaymentMethodSpec(e.getPaymentMethod(), Optional.of(e.getAccountName())))
                    .collect(Collectors.toList());
        } else {
            quoteSidePaymentMethodSpecs = selectedQuoteSidePaymentPaymentRails.stream()
                    .map(e -> new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(e), Optional.empty()))
                    .collect(Collectors.toList());
        }

        List<OfferOption> offerOptions = new ArrayList<>();

        return CompletableFuture.completedFuture(new PocOffer(offerId,
                new Date().getTime(),
                makerNetworkId,
                selectedMarket,
                direction,
                baseSideAmount.getValue(),
                priceSpec,
                protocolTypes,
                baseSidePaymentMethodSpecs,
                quoteSidePaymentMethodSpecs,
                offerOptions
        ));
    }

    public CompletableFuture<BroadcastResult> publishOffer(PocOffer offer) {
        add(offer);
        return addToNetwork(offer);
    }

    private void republishMyOffers() {
        getOpenOffers().forEach(openOffer -> addToNetwork(openOffer.getOffer()));
    }

    public CompletableFuture<BroadcastResult> removeMyOffer(PocOffer offer) {
        remove(offer);
        return removeFromNetwork(offer);
    }

    public CompletableFuture<BroadcastResult> addToNetwork(PocOffer offer) {
        Identity identity = identityService.findActiveIdentity(offer.getId()).orElseThrow();
        return networkService.publishAuthenticatedData(offer, identity.getNetworkIdWithKeyPair().getKeyPair());
    }

    public CompletableFuture<BroadcastResult> removeFromNetwork(PocOffer offer) {
        // We do not retire the identity as it might be still used in the chat. For a mature implementation we would
        // need to check if there is any usage still for that identity and if not retire it.
        return identityService.findActiveIdentity(offer.getId())
                .map(identity -> networkService.removeAuthenticatedData(offer, identity.getNetworkIdWithKeyPair().getKeyPair()))
                .orElse(CompletableFuture.completedFuture(new BroadcastResult()));
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
