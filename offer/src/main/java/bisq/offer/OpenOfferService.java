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

package bisq.offer;

import bisq.common.threading.ExecutorFactory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.runAsync;

public class OpenOfferService implements PersistenceClient<OpenOfferStore> {
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("OpenOfferService.dispatcher");

    public interface Listener {
        void onOpenOfferAdded(OpenOffer openOffer);

        void onOpenOfferRemoved(OpenOffer openOffer);
    }

    @Getter
    private final OpenOfferStore persistableStore = new OpenOfferStore();
    @Getter
    private final Persistence<OpenOfferStore> persistence;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public OpenOfferService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public Set<OpenOffer> getOpenOffers() {
        return persistableStore.getOpenOffers();
    }

    public void add(Offer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        persistableStore.add(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferAdded(openOffer)), DISPATCHER);
        persist();
    }

    public void remove(Offer offer) {
        OpenOffer openOffer = new OpenOffer(offer);
        persistableStore.remove(openOffer);
        runAsync(() -> listeners.forEach(l -> l.onOpenOfferRemoved(openOffer)), DISPATCHER);
        persist();
    }

    public Optional<OpenOffer> findOpenOffer(String offerId) {
        return persistableStore.getOpenOffers().stream()
                .filter(openOffer -> openOffer.getOffer().getId().equals(offerId))
                .findAny();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
