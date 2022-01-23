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

import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.NetworkPayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class OfferRepository implements DataService.Listener {
    private final NetworkService networkService;
    private final Set<Offer> offers = new CopyOnWriteArraySet<>();

    public OfferRepository(NetworkService networkService) {
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        networkService.addDataServiceListener(this);
        return future;
    }

    public void shutdown() {
        networkService.removeDataServiceListener(this);
    }

    @Override
    public void onNetworkPayloadAdded(NetworkPayload networkPayload) {
        if (networkPayload instanceof Offer offer) {
            offers.add(offer);
        }
    }

    @Override
    public void onNetworkPayloadRemoved(NetworkPayload networkPayload) {
        if (networkPayload instanceof Offer offer) {
            offers.remove(offer);
        }
    }
}
