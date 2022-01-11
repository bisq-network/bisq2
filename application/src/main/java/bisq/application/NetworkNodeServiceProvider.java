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

package bisq.application;

import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfigFactory;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static bisq.common.util.OsUtils.EXIT_FAILURE;
import static bisq.common.util.OsUtils.EXIT_SUCCESS;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the completely setup instances to other clients (Api)
 */
@Getter
@Slf4j
public class NetworkNodeServiceProvider extends ServiceProvider {
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    private final ApplicationOptions applicationOptions;
    private final PersistenceService persistenceService;

    public NetworkNodeServiceProvider(ApplicationOptions applicationOptions) {
        super("Seed");
        this.applicationOptions = applicationOptions;

        persistenceService = new PersistenceService(applicationOptions.baseDir());
        keyPairService = new KeyPairService(persistenceService);

        NetworkService.Config networkServiceConfig = NetworkServiceConfigFactory.getConfig(applicationOptions.baseDir(),
                getConfig("bisq.networkServiceConfig"));
        networkService = new NetworkService(networkServiceConfig, persistenceService, keyPairService);
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return persistenceService.readAllPersisted();
    }

    /**
     * Initializes all domain objects.
     * We do in parallel as far as possible. If there are dependencies we chain those as sequence.
     */
    public CompletableFuture<Boolean> initialize() {
        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();
        // Assuming identityRepository depends on keyPairRepository being initialized... 
        allFutures.add(keyPairService.initialize());
        allFutures.add(networkService.bootstrapToNetwork());
        // Once all have successfully completed our initialize is complete as well
        return CompletableFutureUtils.allOf(allFutures)
                .thenApply(success -> success.stream().allMatch(e -> e))
                .orTimeout(10, TimeUnit.SECONDS)
                .thenCompose(CompletableFuture::completedFuture);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        keyPairService.shutdown();
        return networkService.shutdown()
                .whenComplete((__, throwable) -> {
                    if (throwable == null) {
                        System.exit(EXIT_SUCCESS);
                    } else {
                        log.error("Error at shutdown", throwable);
                        System.exit(EXIT_FAILURE);
                    }
                });
    }
}
