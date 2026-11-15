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

package bisq.wallet;

import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

// Records whether this install created or opened a wallet on the daemon; the daemon itself cannot tell.
class WalletStoreService implements PersistenceClient<WalletStore> {
    @Getter
    private final WalletStore persistableStore = new WalletStore();
    @Getter
    private final Persistence<WalletStore> persistence;
    private volatile boolean unsaved;

    WalletStoreService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    boolean isWalletExists() {
        return persistableStore.isWalletExists();
    }

    void setWalletExists() {
        if (persistableStore.setWalletExists() || unsaved) {
            persistWalletRecord();
        }
    }

    // Nothing else writes this store, so a failed write is repeated at the next open and at shutdown
    CompletableFuture<Boolean> persistIfUnsaved() {
        return unsaved ? persistWalletRecord() : CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> persistWalletRecord() {
        unsaved = true;
        return persist().thenApply(saved -> {
            if (saved) {
                unsaved = false;
            }
            return saved;
        });
    }
}
