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
import bisq.common.observable.collection.ObservableSet;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MyBisqEasyOffersService implements PersistenceClient<MyBisqEasyOffersStore>, Service {
    @Getter
    private final MyBisqEasyOffersStore persistableStore = new MyBisqEasyOffersStore();
    @Getter
    private final Persistence<MyBisqEasyOffersStore> persistence;

    public MyBisqEasyOffersService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");


        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(BisqEasyOffer offer) {
        persistableStore.add(offer);
        persist();
    }

    public void remove(BisqEasyOffer offer) {
        persistableStore.remove(offer);
        persist();
    }

    public ObservableSet<BisqEasyOffer> getOffers() {
        return persistableStore.getOffers();
    }

    public Optional<BisqEasyOffer> findOffer(String offerId) {
        return getOffers().stream()
                .filter(offer -> offer.getId().equals(offerId))
                .findAny();
    }
}
