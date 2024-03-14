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

import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class PocOfferService implements Service {
    private final PocOpenOfferService openOfferService;
    private final PocOfferBookService offerBookService;

    public PocOfferService(NetworkService networkService, IdentityService identityService, PersistenceService persistenceService) {
        openOfferService = new PocOpenOfferService(networkService, identityService, persistenceService);
        offerBookService = new PocOfferBookService(networkService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        return openOfferService.initialize()
                .thenCompose(result -> offerBookService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        return openOfferService.shutdown().thenApply(list -> true);
    }
}