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

package bisq.offer.poc;

import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class PocOfferBookService {
    @Getter
    private final ObservableSet<PocOffer> offers = new ObservableSet<>();
    private final DataService dataService;

    public PocOfferBookService(NetworkService networkService) {
        checkArgument(networkService.getDataService().isPresent(),
                "networkService.getDataService() is expected to be present if OfferBookService is used");
        dataService = networkService.getDataService().get();
        dataService.addListener(new DataService.Listener() {
            @Override
            public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
                if (authenticatedData.getDistributedData() instanceof PocOffer) {
                    offers.add((PocOffer) authenticatedData.getDistributedData());
                }
            }

            @Override
            public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
                if (authenticatedData.getDistributedData() instanceof PocOffer) {
                    offers.remove((PocOffer) authenticatedData.getDistributedData());
                }
            }
        });
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        offers.addAll(dataService.getAuthenticatedPayloadStreamByStoreName("Offer")
                .filter(payload -> payload.getDistributedData() instanceof PocOffer)
                .map(payload -> (PocOffer) payload.getDistributedData())
                .toList());
        return CompletableFuture.completedFuture(true);
    }
}