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

package bisq.oracle_node_app;

import bisq.application.ApplicationService;
import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.oracle_node.OracleNodeService;
import bisq.security.SecurityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
@Getter
public class OracleNodeApplicationService extends ApplicationService {
    private final IdentityService identityService;
    private final SecurityService securityService;
    private final NetworkService networkService;
    private final OracleNodeService oracleNodeService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public OracleNodeApplicationService(String[] args) {
        super("oracle_node", args);

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

        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService);

        OracleNodeService.Config oracleNodeConfig = OracleNodeService.Config.from(getConfig("oracleNode"));
        oracleNodeService = new OracleNodeService(oracleNodeConfig,
                identityService,
                networkService,
                persistenceService,
                securityService,
                authorizedBondedRolesService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> networkService.initialize())
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> authorizedBondedRolesService.initialize())
                .thenCompose(result -> oracleNodeService.initialize())
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
        return supplyAsync(() -> oracleNodeService.shutdown()
                .thenCompose(result -> authorizedBondedRolesService.shutdown())
                .thenCompose(result -> identityService.shutdown())
                .thenCompose(result -> networkService.shutdown())
                .thenCompose(result -> securityService.shutdown())
                .orTimeout(2, TimeUnit.MINUTES)
                .handle((result, throwable) -> throwable == null)
                .join());
    }
}