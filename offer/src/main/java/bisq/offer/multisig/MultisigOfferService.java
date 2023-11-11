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

package bisq.offer.multisig;

import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.p2p.services.data.BroadCastDataResult;
import bisq.offer.Offer;
import bisq.offer.OfferMessageService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class MultisigOfferService implements Service {
    private final MyMultisigOffersService myMultisigOffersService;
    private final OfferMessageService offerMessageService;
    @Getter
    private final ObservableSet<MultisigOffer> offers = new ObservableSet<>();
    private final CollectionObserver<Offer<?, ?>> offersObserver;
    private Pin offersObserverPin;

    public MultisigOfferService(PersistenceService persistenceService,
                                OfferMessageService offerMessageService) {
        this.offerMessageService = offerMessageService;
        myMultisigOffersService = new MyMultisigOffersService(persistenceService);
        offersObserver = new CollectionObserver<>() {
            @Override
            public void add(Offer<?, ?> element) {
                if (element instanceof MultisigOffer) {
                    processAddedOffer((MultisigOffer) element);
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof MultisigOffer) {
                    processRemovedOffer((MultisigOffer) element);
                }
            }

            @Override
            public void clear() {
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        offersObserverPin = offerMessageService.getOffers().addObserver(offersObserver);

        republishMyOffers();
        // Do again once we assume we better connected
        // todo provide an API from network to get an event for that
        Scheduler.run(this::republishMyOffers).after(5000, TimeUnit.MILLISECONDS);

        return myMultisigOffersService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        offersObserverPin.unbind();
        return removeAllOfferFromNetwork().thenCompose(e -> myMultisigOffersService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadCastDataResult> publishOffer(String offerId) {
        return findOffer(offerId)
                .map(this::publishOffer)
                .orElse(CompletableFuture.failedFuture(new RuntimeException("Offer with not found. OfferID=" + offerId)));
    }

    public CompletableFuture<BroadCastDataResult> publishOffer(MultisigOffer offer) {
        myMultisigOffersService.add(offer);
        return offerMessageService.addToNetwork(offer);
    }

    public CompletableFuture<BroadCastDataResult> removeOffer(String offerId) {
        return findOffer(offerId)
                .map(this::removeOffer)
                .orElse(CompletableFuture.failedFuture(new RuntimeException("Offer with not found. OfferID=" + offerId)));
    }

    public CompletableFuture<BroadCastDataResult> removeOffer(MultisigOffer offer) {
        myMultisigOffersService.remove(offer);
        return offerMessageService.removeFromNetwork(offer);
    }

    public Optional<MultisigOffer> findOffer(String offerId) {
        return offers.stream().filter(offer -> offer.getId().equals(offerId)).findAny();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean processAddedOffer(MultisigOffer offer) {
        return offers.add(offer);
    }

    private boolean processRemovedOffer(MultisigOffer offer) {
        return offers.remove(offer);
    }

    private void republishMyOffers() {
        getOffers().forEach(offerMessageService::removeFromNetwork);
    }

    private CompletableFuture<Boolean> removeAllOfferFromNetwork() {
        return CompletableFutureUtils.allOf(getOffers().stream()
                        .map(offerMessageService::removeFromNetwork))
                .thenApply(resultList -> true);
    }
}