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

package network.misq.presentation.offer;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import network.misq.offer.MarketPriceService;
import network.misq.offer.Offer;
import network.misq.offer.OfferRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class OfferEntityRepository {
    protected final OfferRepository offerRepository;
    protected final List<OfferEntity> offerEntities = new CopyOnWriteArrayList<>();
    protected final PublishSubject<OfferEntity> offerEntityAddedSubject;
    protected final PublishSubject<OfferEntity> offerEntityRemovedSubject;
    private final MarketPriceService marketPriceService;
    private Disposable oferAddedDisposable, oferRemovedDisposable;

    public OfferEntityRepository(OfferRepository offerRepository, MarketPriceService marketPriceService) {
        this.offerRepository = offerRepository;
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
        offerEntities.addAll(offerRepository.getOffers().stream()
                .map(offer -> new OfferEntity((Offer) offer, marketPriceService.getMarketPriceSubject()))
                .collect(Collectors.toList()));
        future.complete(true);
        return future;
    }

    public void activate() {
        oferAddedDisposable = offerRepository.getOfferAddedSubject().subscribe(offer -> {
            offerEntities.stream()
                    .filter(e -> e.getOffer().equals(offer))
                    .findAny()
                    .ifPresent(offerEntity -> {
                        offerEntities.remove(offerEntity);
                        offerEntityRemovedSubject.onNext(offerEntity);
                    });
        });
        oferRemovedDisposable = offerRepository.getOfferRemovedSubject().subscribe(offer -> {
            if (offer instanceof Offer) {
                OfferEntity offerEntity = new OfferEntity((Offer) offer, marketPriceService.getMarketPriceSubject());
                offerEntities.add(offerEntity);
                offerEntityAddedSubject.onNext(offerEntity);
            }
        });

    /*    offerEntities.addAll(offerRepository.getOffers().stream()
                .map(offer -> new OfferEntity((Offer) offer, networkService.getMarketPriceSubject()))
                .collect(Collectors.toList()));*/
    }

    public void deactivate() {
        oferAddedDisposable.dispose();
        oferRemovedDisposable.dispose();
    }

    public List<OfferEntity> getOfferEntities() {
        return offerEntities;
    }

    public PublishSubject<OfferEntity> getOfferEntityAddedSubject() {
        return offerEntityAddedSubject;
    }

    public PublishSubject<OfferEntity> getOfferEntityRemovedSubject() {
        return offerEntityRemovedSubject;
    }

    public void shutdown() {

    }
}
