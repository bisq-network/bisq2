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

import bisq.network.NetworkServiceConfig;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static bisq.common.util.OsUtils.EXIT_FAILURE;
import static bisq.common.util.OsUtils.EXIT_SUCCESS;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the complete setup instances to other clients (Api)
 */
@Getter
@Slf4j
public class NetworkApplicationService extends ServiceProvider {
    protected final NetworkService networkService;
    protected final ApplicationConfig applicationConfig;
    protected final PersistenceService persistenceService;
    protected final SecurityService securityService;

    public NetworkApplicationService(String[] args) {
        super("Seed");

        applicationConfig = ApplicationConfigFactory.getConfig(getConfig("bisq.application"), args);
        ApplicationSetup.initialize(applicationConfig);

        persistenceService = new PersistenceService(applicationConfig.getBaseDir());

        securityService = new SecurityService(persistenceService);

        NetworkServiceConfig networkServiceConfig = NetworkServiceConfig.from(
                applicationConfig.getBaseDir(),
                getConfig("bisq.network"));
        networkService = new NetworkService(networkServiceConfig, persistenceService, securityService.getKeyPairService());
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return persistenceService.readAllPersisted();
    }

    /**
     * Initializes all domain objects.
     * We do in parallel as far as possible. If there are dependencies we chain those as sequence.
     */
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> networkService.bootstrapToNetwork())
                .orTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
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
