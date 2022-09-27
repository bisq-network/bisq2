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

package bisq.security;

import bisq.common.application.Service;
import bisq.persistence.PersistenceService;
import bisq.security.pow.EquihashProofOfWorkService;
import bisq.security.pow.ProofOfWorkService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class SecurityService implements Service {
    @Getter
    private final KeyPairService keyPairService;
    @Getter
    private final ProofOfWorkService proofOfWorkService;

    public SecurityService(PersistenceService persistenceService) {
        keyPairService = new KeyPairService(persistenceService);
        proofOfWorkService = new EquihashProofOfWorkService();
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        proofOfWorkService.initialize();
        return keyPairService.initialize();
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }
}
