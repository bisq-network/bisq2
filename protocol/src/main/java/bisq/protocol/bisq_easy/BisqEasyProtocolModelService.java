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

package bisq.protocol.bisq_easy;

import bisq.common.application.Service;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BisqEasyProtocolModelService implements Service, PersistenceClient<BisqEasyProtocolModelStore> {
    @Getter
    private final BisqEasyProtocolModelStore persistableStore = new BisqEasyProtocolModelStore();
    @Getter
    private final Persistence<BisqEasyProtocolModelStore> persistence;

    public BisqEasyProtocolModelService(PersistenceService persistenceService) {

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean addBisqEasyProtocolModel(BisqEasyProtocolModel protocolModel) {
        BisqEasyProtocolModel previous = persistableStore.getProtocolModelById().putIfAbsent(protocolModel.getId(), protocolModel);
        if (previous == null) {
            persist();
            return true;
        } else {
            return false;
        }
    }

    public Optional<BisqEasyProtocolModel> findBisqEasyProtocolModel(String id) {
        return Optional.ofNullable(persistableStore.getProtocolModelById().get(id));
    }

    public boolean deleteBisqEasyProtocolModel(String id) {
        return findBisqEasyProtocolModel(id)
                .map(model -> {
                    BisqEasyProtocolModel previous = persistableStore.getProtocolModelById().remove(id);
                    boolean modelExisted = previous != null;
                    if (modelExisted) {
                        persist();
                    }
                    return modelExisted;
                })
                .orElse(false);
    }
}