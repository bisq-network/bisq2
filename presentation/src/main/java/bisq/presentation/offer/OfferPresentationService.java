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

package bisq.presentation.offer;


import bisq.offer.OfferService;
import bisq.oracle.marketprice.MarketPriceService;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OfferPresentationService {
    protected final OfferService offerService;
    protected final List<OfferPresentation> offerEntities = new CopyOnWriteArrayList<>();
    protected final PublishSubject<OfferPresentation> offerEntityAddedSubject;
    protected final PublishSubject<OfferPresentation> offerEntityRemovedSubject;
    private final MarketPriceService marketPriceService;
    private Disposable oferAddedDisposable, oferRemovedDisposable;

    public OfferPresentationService(OfferService offerService, MarketPriceService marketPriceService) {
        this.offerService = offerService;
        this.marketPriceService = marketPriceService;

        offerEntityAddedSubject = PublishSubject.create();
        offerEntityRemovedSubject = PublishSubject.create();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        //todo
      /*  offerEntities.addAll(offerService.getOffers().stream()
                .map(offer -> new OfferPresentation((SwapOffer) offer, marketPriceService.getMarketPriceSubject()))
                .collect(Collectors.toList()));*/
        future.complete(true);
        return future;
    }

    public void activate() {
        oferAddedDisposable = offerService.getOfferAddedSubject().subscribe(offer -> {
            offerEntities.stream()
                    .filter(e -> e.getOffer().equals(offer))
                    .findAny()
                    .ifPresent(offerEntity -> {
                        offerEntities.remove(offerEntity);
                        offerEntityRemovedSubject.onNext(offerEntity);
                    });
        });
        //todo
     /*   oferRemovedDisposable = offerService.getOfferRemovedSubject().subscribe(offer -> {
            if (offer instanceof SwapOffer) {
                OfferPresentation offerEntity = new OfferPresentation((SwapOffer) offer, marketPriceService.getMarketPriceSubject());
                offerEntities.add(offerEntity);
                offerEntityAddedSubject.onNext(offerEntity);
            }
        });*/

    /*    offerEntities.addAll(offerRepository.getOffers().stream()
                .map(offer -> new OfferEntity((Offer) offer, networkService.getMarketPriceSubject()))
                .collect(Collectors.toList()));*/
    }

    public void deactivate() {
        oferAddedDisposable.dispose();
        oferRemovedDisposable.dispose();
    }

    public List<OfferPresentation> getOfferEntities() {
        return offerEntities;
    }

    public PublishSubject<OfferPresentation> getOfferEntityAddedSubject() {
        return offerEntityAddedSubject;
    }

    public PublishSubject<OfferPresentation> getOfferEntityRemovedSubject() {
        return offerEntityRemovedSubject;
    }

    public void shutdown() {

    }
}
