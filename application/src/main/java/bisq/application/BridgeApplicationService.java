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
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.oracle.OracleService;
import bisq.oracle.daobridge.DaoBridgeHttpService;
import bisq.oracle.daobridge.DaoBridgeService;
import bisq.oracle.timestamp.TimestampService;
import bisq.security.SecurityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
@Getter
public class BridgeApplicationService extends ApplicationService {
    private final IdentityService identityService;
    private final DaoBridgeService daoBridgeService;
    private final DaoBridgeHttpService daoBridgeHttpService;
    private final SecurityService securityService;
    private final NetworkService networkService;
    private final TimestampService timestampService;

    public BridgeApplicationService(String[] args) {
        super("default", args);

        securityService = new SecurityService(persistenceService);

        NetworkServiceConfig networkServiceConfig = NetworkServiceConfig.from(config.getBaseDir(), getConfig("network"));
        networkService = new NetworkService(networkServiceConfig, persistenceService, securityService.getKeyPairService());

        identityService = new IdentityService(IdentityService.Config.from(getConfig("identity")),
                persistenceService,
                securityService,
                networkService
        );

        DaoBridgeHttpService.Config daoBridgeConfig = DaoBridgeHttpService.Config.from(getConfig("oracle.daoBridgeHttpService"));
        daoBridgeHttpService = new DaoBridgeHttpService(daoBridgeConfig, networkService);

        OracleService.Config oracleConfig = OracleService.Config.from(getConfig("oracle"));
        daoBridgeService = new DaoBridgeService(oracleConfig,
                networkService,
                identityService,
                daoBridgeHttpService);

        timestampService = new TimestampService(oracleConfig, getPersistenceService(), identityService, networkService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> networkService.initialize())
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> daoBridgeHttpService.initialize())
                .thenCompose(result -> daoBridgeService.initialize())
                .thenCompose(result -> timestampService.initialize())
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
        return supplyAsync(() -> timestampService.shutdown()
                        .thenCompose(result -> daoBridgeService.shutdown())
                        .thenCompose(result -> daoBridgeHttpService.shutdown())
                        .thenCompose(result -> identityService.shutdown())
                        .thenCompose(result -> networkService.shutdown())
                        .thenCompose(result -> securityService.shutdown())
                        .orTimeout(2, TimeUnit.MINUTES)
                        .handle((result, throwable) -> throwable == null)
                        .join(),
                ExecutorFactory.newSingleThreadExecutor("Shutdown"));
    }
}