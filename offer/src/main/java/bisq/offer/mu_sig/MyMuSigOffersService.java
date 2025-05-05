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

package bisq.offer.mu_sig;

import bisq.common.application.Service;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MyMuSigOffersService implements PersistenceClient<MyMuSigOffersStore>, Service {
    @Getter
    private final MyMuSigOffersStore persistableStore = new MyMuSigOffersStore();
    @Getter
    private final Persistence<MyMuSigOffersStore> persistence;

    public MyMuSigOffersService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void addOffer(MuSigOffer offer) {
        persistableStore.addOffer(offer);
        persist();
    }

    public void removeOffer(MuSigOffer offer) {
        persistableStore.removeOffer(offer);
        persist();
    }

    public void activateOffer(MuSigOffer offer) {
        persistableStore.activateOffer(offer);
        persist();
    }

    public void deactivateOffer(MuSigOffer offer) {
        persistableStore.deactivateOffer(offer);
        persist();
    }

    public Set<MuSigOffer> getOffers() {
        return persistableStore.getOffers();
    }

    public Set<MuSigOffer> getActivatedOffers() {
        return persistableStore.getActivatedOffers();
    }

    public ReadOnlyObservableSet<MuSigOffer> getObservableOffers() {
        return persistableStore.getOffersAsObservableSet();
    }

    public Optional<MuSigOffer> findOffer(String offerId) {
        return getOffers().stream()
                .filter(offer -> offer.getId().equals(offerId))
                .findAny();
    }
}
