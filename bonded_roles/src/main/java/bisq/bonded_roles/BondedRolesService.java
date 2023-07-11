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
import bisq.bonded_roles.registration.BondedRoleRegistrationService;
import bisq.bonded_roles.service.explorer.ExplorerService;
import bisq.bonded_roles.service.market_price.MarketPriceService;
import bisq.common.application.Service;
import bisq.network.NetworkService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class BondedRolesService implements Service {


    @Getter
    public static class Config {
        private final com.typesafe.config.Config marketPrice;
        private final com.typesafe.config.Config blockchainExplorer;

        public Config(com.typesafe.config.Config marketPrice,
                      com.typesafe.config.Config blockchainExplorer) {
            this.marketPrice = marketPrice;
            this.blockchainExplorer = blockchainExplorer;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getConfig("marketPrice"),
                    config.getConfig("blockchainExplorer"));
        }
    }

    private final BondedRoleRegistrationService bondedRoleRegistrationService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MarketPriceService marketPriceService;
    private final ExplorerService explorerService;
    private final AlertService alertService;

    public BondedRolesService(Config config, String applicationVersion, NetworkService networkService) {
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService);
        bondedRoleRegistrationService = new BondedRoleRegistrationService(networkService, authorizedBondedRolesService);
        marketPriceService = new MarketPriceService(MarketPriceService.Config.from(config.getMarketPrice()),
                networkService,
                applicationVersion);
        explorerService = new ExplorerService(ExplorerService.Config.from(config.getBlockchainExplorer()),
                networkService,
                applicationVersion);

        alertService = new AlertService(networkService, authorizedBondedRolesService);
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
                .thenCompose(result -> alertService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return authorizedBondedRolesService.shutdown()
                .thenCompose(result -> bondedRoleRegistrationService.shutdown())
                .thenCompose(result -> marketPriceService.shutdown())
                .thenCompose(result -> explorerService.shutdown())
                .thenCompose(result -> alertService.shutdown());
    }
}