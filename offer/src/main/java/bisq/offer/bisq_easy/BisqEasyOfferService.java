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

package bisq.offer.bisq_easy;

import bisq.common.application.Service;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.p2p.services.data.DataService;
import bisq.offer.Offer;
import bisq.offer.OfferMessageService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class BisqEasyOfferService implements Service {
    private final MyBisqEasyOffersService myBisqEasyOffersService;
    private final OfferMessageService offerMessageService;
    @Getter
    private final ObservableSet<BisqEasyOffer> offers = new ObservableSet<>();

    public BisqEasyOfferService(PersistenceService persistenceService,
                                OfferMessageService offerMessageService) {
        this.offerMessageService = offerMessageService;
        myBisqEasyOffersService = new MyBisqEasyOffersService(persistenceService);
        offerMessageService.getOffers().addListener(new CollectionObserver<>() {
            @Override
            public void add(Offer<?, ?> element) {
                if (element instanceof BisqEasyOffer) {
                    processAddedBisqEasyOffer((BisqEasyOffer) element);
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOffer) {
                    processRemovedBisqEasyOffer((BisqEasyOffer) element);
                }
            }

            @Override
            public void clear() {

            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        republishMyOffers();
        // Do again once we assume we better connected
        // todo provide an API from network to get an event for that
        Scheduler.run(this::republishMyOffers).after(5000, TimeUnit.MILLISECONDS);

        return myBisqEasyOffersService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return removeAllOfferFromNetwork().thenCompose(e -> myBisqEasyOffersService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<DataService.BroadCastDataResult> publish(BisqEasyOffer offer) {
        myBisqEasyOffersService.add(offer);
        return offerMessageService.publish(offer);
    }

    public CompletableFuture<DataService.BroadCastDataResult> remove(BisqEasyOffer offer) {
        myBisqEasyOffersService.remove(offer);
        return offerMessageService.remove(offer);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean processAddedBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        return offers.add(bisqEasyOffer);
    }

    private boolean processRemovedBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        return offers.remove(bisqEasyOffer);
    }

    private void republishMyOffers() {
        getOffers().forEach(offerMessageService::remove);
    }

    private CompletableFuture<Boolean> removeAllOfferFromNetwork() {
        return CompletableFutureUtils.allOf(getOffers().stream()
                        .map(offerMessageService::remove))
                .thenApply(resultList -> true);
    }

}