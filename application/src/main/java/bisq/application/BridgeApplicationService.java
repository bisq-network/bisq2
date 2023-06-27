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

import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.oracle.node.bisq1_bridge.Bisq1BridgeHttpService;
import bisq.oracle.node.bisq1_bridge.Bisq1BridgeService;
import bisq.oracle.node.timestamp.TimestampService;
import bisq.oracle.service.OracleService;
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
    private final Bisq1BridgeService bisq1BridgeService;
    private final Bisq1BridgeHttpService bisq1BridgeHttpService;
    private final SecurityService securityService;
    private final NetworkService networkService;
    private final TimestampService timestampService;

    public BridgeApplicationService(String[] args) {
        super("default", args);

        securityService = new SecurityService(persistenceService);

        NetworkServiceConfig networkServiceConfig = NetworkServiceConfig.from(config.getBaseDir(), getConfig("network"));
        networkService = new NetworkService(networkServiceConfig,
                persistenceService,
                securityService.getKeyPairService(),
                securityService.getProofOfWorkService());

        identityService = new IdentityService(IdentityService.Config.from(getConfig("identity")),
                persistenceService,
                securityService,
                networkService
        );

        Bisq1BridgeHttpService.Config daoBridgeConfig = Bisq1BridgeHttpService.Config.from(getConfig("oracle.daoBridgeHttpService"));
        bisq1BridgeHttpService = new Bisq1BridgeHttpService(daoBridgeConfig, networkService);

        OracleService.Config oracleConfig = OracleService.Config.from(getConfig("oracle"));
        bisq1BridgeService = new Bisq1BridgeService(oracleConfig,
                networkService,
                identityService,
                persistenceService,
                bisq1BridgeHttpService);

        timestampService = new TimestampService(oracleConfig, persistenceService, identityService, networkService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> networkService.initialize())
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> bisq1BridgeHttpService.initialize())
                .thenCompose(result -> bisq1BridgeService.initialize())
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
                .thenCompose(result -> bisq1BridgeService.shutdown())
                .thenCompose(result -> bisq1BridgeHttpService.shutdown())
                        .thenCompose(result -> identityService.shutdown())
                        .thenCompose(result -> networkService.shutdown())
                        .thenCompose(result -> securityService.shutdown())
                        .orTimeout(2, TimeUnit.MINUTES)
                        .handle((result, throwable) -> throwable == null)
                        .join());
    }
}