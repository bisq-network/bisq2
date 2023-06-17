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

import bisq.common.application.Service;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.offer.bisq_easy.BisqEasyOfferService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class OfferService implements Service {
    private final BisqEasyOfferService bisqEasyOfferService;
    private final OfferMessageService offerMessageService;

    public OfferService(NetworkService networkService, IdentityService identityService, PersistenceService persistenceService) {
        offerMessageService = new OfferMessageService(networkService, identityService);
        bisqEasyOfferService = new BisqEasyOfferService(persistenceService, offerMessageService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return offerMessageService.initialize()
                .thenCompose(result -> bisqEasyOfferService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return bisqEasyOfferService.shutdown()
                .thenCompose(result -> offerMessageService.shutdown());
    }
}