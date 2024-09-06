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

package bisq.offer.bisq_musig;

import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.p2p.services.data.BroadcastResult;
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
public class BisqMuSigOfferService implements Service {
    private final MyBisqMuSigOffersService myBisqMuSigOffersService;
    private final OfferMessageService offerMessageService;
    @Getter
    private final ObservableSet<BisqMuSigOffer> offers = new ObservableSet<>();
    private final CollectionObserver<Offer<?, ?>> offersObserver;
    private Pin offersObserverPin;

    public BisqMuSigOfferService(PersistenceService persistenceService,
                                 OfferMessageService offerMessageService) {
        this.offerMessageService = offerMessageService;
        myBisqMuSigOffersService = new MyBisqMuSigOffersService(persistenceService);
        offersObserver = new CollectionObserver<>() {
            @Override
            public void add(Offer<?, ?> element) {
                if (element instanceof BisqMuSigOffer) {
                    processAddedOffer((BisqMuSigOffer) element);
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqMuSigOffer) {
                    processRemovedOffer((BisqMuSigOffer) element);
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
        offersObserverPin = offerMessageService.getOffers().addObserver(offersObserver);

        republishMyOffers();
        // Do again once we assume we better connected
        Scheduler.run(this::republishMyOffers)
                .host(this)
                .runnableName("republishMyOffers")
                .after(5000, TimeUnit.MILLISECONDS);

        return myBisqMuSigOffersService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        offersObserverPin.unbind();
        return removeAllOfferFromNetwork().thenCompose(e -> myBisqMuSigOffersService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> publishOffer(String offerId) {
        return findOffer(offerId)
                .map(this::publishOffer)
                .orElse(CompletableFuture.failedFuture(new RuntimeException("Offer with not found. OfferID=" + offerId)));
    }

    public CompletableFuture<BroadcastResult> publishOffer(BisqMuSigOffer offer) {
        myBisqMuSigOffersService.add(offer);
        return offerMessageService.addToNetwork(offer);
    }

    public CompletableFuture<BroadcastResult> removeOffer(String offerId) {
        return findOffer(offerId)
                .map(this::removeOffer)
                .orElse(CompletableFuture.failedFuture(new RuntimeException("Offer with not found. OfferID=" + offerId)));
    }

    public CompletableFuture<BroadcastResult> removeOffer(BisqMuSigOffer offer) {
        myBisqMuSigOffersService.remove(offer);
        return offerMessageService.removeFromNetwork(offer);
    }

    public Optional<BisqMuSigOffer> findOffer(String offerId) {
        return offers.stream().filter(offer -> offer.getId().equals(offerId)).findAny();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean processAddedOffer(BisqMuSigOffer offer) {
        return offers.add(offer);
    }

    private boolean processRemovedOffer(BisqMuSigOffer offer) {
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