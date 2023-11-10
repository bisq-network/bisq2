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

package bisq.bonded_roles;

import bisq.bonded_roles.alert.AlertService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.explorer.ExplorerService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.registration.BondedRoleRegistrationService;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.common.application.Service;
import bisq.common.util.Version;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class BondedRolesService implements Service {
    @Getter
    public static class Config {
        private final List<? extends com.typesafe.config.Config> marketPriceServiceProviders;
        private final com.typesafe.config.Config blockchainExplorer;
        private final boolean ignoreSecurityManager;

        public Config(List<? extends com.typesafe.config.Config> marketPriceServiceProviders,
                      com.typesafe.config.Config blockchainExplorer,
                      boolean ignoreSecurityManager) {
            this.marketPriceServiceProviders = marketPriceServiceProviders;
            this.blockchainExplorer = blockchainExplorer;
            this.ignoreSecurityManager = ignoreSecurityManager;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getConfigList("marketPriceServiceProviders"),
                    config.getConfig("blockchainExplorer"),
                    config.getBoolean("ignoreSecurityManager"));
        }
    }

    private final BondedRoleRegistrationService bondedRoleRegistrationService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MarketPriceService marketPriceService;
    private final ExplorerService explorerService;
    private final AlertService alertService;
    private final ReleaseNotificationsService releaseNotificationsService;

    public BondedRolesService(Config config, Version version, PersistenceService persistenceService, NetworkService networkService) {
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, config.isIgnoreSecurityManager());
        bondedRoleRegistrationService = new BondedRoleRegistrationService(networkService, authorizedBondedRolesService);
        List<? extends com.typesafe.config.Config> marketPriceServiceProviders = config.getMarketPriceServiceProviders();
        marketPriceService = new MarketPriceService(marketPriceServiceProviders, version, persistenceService, networkService);
        explorerService = new ExplorerService(ExplorerService.Config.from(config.getBlockchainExplorer()),
                networkService,
                version);

        alertService = new AlertService(networkService, authorizedBondedRolesService);
        releaseNotificationsService = new ReleaseNotificationsService(networkService, authorizedBondedRolesService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return authorizedBondedRolesService.initialize()
                .thenCompose(result -> bondedRoleRegistrationService.initialize())
                .thenCompose(result -> marketPriceService.initialize())
                .thenCompose(result -> explorerService.initialize())
                .thenCompose(result -> alertService.initialize())
                .thenCompose(result -> releaseNotificationsService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return authorizedBondedRolesService.shutdown()
                .thenCompose(result -> bondedRoleRegistrationService.shutdown())
                .thenCompose(result -> marketPriceService.shutdown())
                .thenCompose(result -> explorerService.shutdown())
                .thenCompose(result -> alertService.shutdown())
                .thenCompose(result -> releaseNotificationsService.shutdown());
    }
}