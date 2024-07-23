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
import bisq.common.observable.collection.ObservableSet;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MyBisqMuSigOffersService implements PersistenceClient<MyBisqMuSigOffersStore>, Service {
    @Getter
    private final MyBisqMuSigOffersStore persistableStore = new MyBisqMuSigOffersStore();
    @Getter
    private final Persistence<MyBisqMuSigOffersStore> persistence;

    public MyBisqMuSigOffersService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void add(BisqMuSigOffer offer) {
        persistableStore.add(offer);
        persist();
    }

    public void remove(BisqMuSigOffer offer) {
        persistableStore.remove(offer);
        persist();
    }

    public ObservableSet<BisqMuSigOffer> getOffers() {
        return persistableStore.getOffers();
    }

    public Optional<BisqMuSigOffer> findOffer(String offerId) {
        return getOffers().stream()
                .filter(offer -> offer.getId().equals(offerId))
                .findAny();
    }
}
