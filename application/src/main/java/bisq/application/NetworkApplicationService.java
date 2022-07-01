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

import bisq.common.threading.ExecutorFactory;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.security.SecurityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the complete setup instances to other clients (Api)
 */
@Getter
@Slf4j
public class NetworkApplicationService extends ApplicationService {
    protected final NetworkService networkService;
    protected final SecurityService securityService;

    public NetworkApplicationService(String[] args) {
        super("Seed", args);

        securityService = new SecurityService(persistenceService);

        NetworkServiceConfig networkServiceConfig = NetworkServiceConfig.from(config.getBaseDir(), getConfig("network"));
        networkService = new NetworkService(networkServiceConfig, persistenceService, securityService.getKeyPairService());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> networkService.initialize())
                .orTimeout(5, TimeUnit.MINUTES)
                .whenComplete((success, throwable) -> {
                    if (success) {
                        log.info("NetworkApplicationService initialized");
                    } else {
                        log.error("Initializing networkApplicationService failed", throwable);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        // We shut down services in opposite order as they are initialized
        return supplyAsync(() -> networkService.shutdown()
                        .thenCompose(result -> securityService.shutdown())
                        .orTimeout(2, TimeUnit.MINUTES)
                        .handle((result, throwable) -> throwable == null)
                        .join(),
                ExecutorFactory.newSingleThreadExecutor("Shutdown"));
    }
}
