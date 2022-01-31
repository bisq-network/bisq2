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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OfferBook {
    private final Optional<DataService> dataService;
    private final NetworkService networkService;

    public OfferBook(NetworkService networkService) {
        this.networkService = networkService;
        dataService = networkService.getDataService();
        //ObservableList
    }

    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);


        return future;
    }

    public void shutdown() {
    }
}