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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.explorer.ExplorerService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.registration.BondedRoleRegistrationService;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService;
import bisq.common.application.Service;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class BondedRolesService implements Service {
    @Getter
    public static class Config {
        private final com.typesafe.config.Config blockchainExplorer;
        private final boolean ignoreSecurityManager;
        private final com.typesafe.config.Config marketPrice;

        public Config(com.typesafe.config.Config marketPrice,
                      com.typesafe.config.Config blockchainExplorer,
                      boolean ignoreSecurityManager) {
            this.marketPrice = marketPrice;
            this.blockchainExplorer = blockchainExplorer;
            this.ignoreSecurityManager = ignoreSecurityManager;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getConfig("marketPrice"),
                    config.getConfig("blockchainExplorer"),
                    config.getBoolean("ignoreSecurityManager"));
        }
    }

    private final BondedRoleRegistrationService bondedRoleRegistrationService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MarketPriceService marketPriceService;
    private final ExplorerService explorerService;
    private final AlertService alertService;
    private final DifficultyAdjustmentService difficultyAdjustmentService;
    private final ReleaseNotificationsService releaseNotificationsService;

    public BondedRolesService(Config config, PersistenceService persistenceService, NetworkService networkService) {
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, config.isIgnoreSecurityManager());
        bondedRoleRegistrationService = new BondedRoleRegistrationService(networkService, authorizedBondedRolesService);
        marketPriceService = new MarketPriceService(config.getMarketPrice(), persistenceService, networkService, authorizedBondedRolesService);
        explorerService = new ExplorerService(ExplorerService.Config.from(config.getBlockchainExplorer()), networkService);
        alertService = new AlertService(authorizedBondedRolesService);
        difficultyAdjustmentService = new DifficultyAdjustmentService(authorizedBondedRolesService);
        releaseNotificationsService = new ReleaseNotificationsService(authorizedBondedRolesService);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return difficultyAdjustmentService.initialize()
                .thenCompose(result -> alertService.initialize())
                .thenCompose(result -> bondedRoleRegistrationService.initialize())
                .thenCompose(result -> marketPriceService.initialize())
                .thenCompose(result -> explorerService.initialize())
                .thenCompose(result -> releaseNotificationsService.initialize())
                .thenCompose(result -> authorizedBondedRolesService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return authorizedBondedRolesService.shutdown()
                .thenCompose(result -> difficultyAdjustmentService.shutdown())
                .thenCompose(result -> alertService.shutdown())
                .thenCompose(result -> bondedRoleRegistrationService.shutdown())
                .thenCompose(result -> marketPriceService.shutdown())
                .thenCompose(result -> explorerService.shutdown())
                .thenCompose(result -> releaseNotificationsService.shutdown());
    }
}