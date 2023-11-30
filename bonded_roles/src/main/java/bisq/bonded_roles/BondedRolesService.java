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
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    private final NetworkService networkService;
    private final BondedRoleRegistrationService bondedRoleRegistrationService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final MarketPriceService marketPriceService;
    private final ExplorerService explorerService;
    private final AlertService alertService;
    private final ReleaseNotificationsService releaseNotificationsService;
    private DataService.Listener authorizedDataListener;

    public BondedRolesService(Config config, Version version, PersistenceService persistenceService, NetworkService networkService) {
        this.networkService = networkService;
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, config.isIgnoreSecurityManager());
        bondedRoleRegistrationService = new BondedRoleRegistrationService(networkService, authorizedBondedRolesService);
        marketPriceService = new MarketPriceService(config.getMarketPrice(), version, persistenceService, networkService);
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

        // We need to have received the bonded role data from the oracle node for verifying registered bonded roles.
        // As a quick fix we use a listener on the AuthorizedData and once we received those and after a delay
        // we start with the sub services.
        // TODO implement a proper solution
        // See https://github.com/bisq-network/bisq2/issues/1418
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (networkService.getDataService().isPresent()) {
            DataService dataService = networkService.getDataService().get();
            if (dataService.getAuthorizedData().count() > 1) {
                future.complete(null);
            } else {
                CountDownLatch latch = new CountDownLatch(1);
                authorizedDataListener = new DataService.Listener() {
                    @Override
                    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
                        latch.countDown();
                    }
                };
                dataService.addListener(authorizedDataListener);
                try {
                    latch.await(5, TimeUnit.SECONDS);
                    dataService.removeListener(authorizedDataListener);
                    Thread.sleep(2000);
                    future.complete(null);
                } catch (InterruptedException ignore) {
                }
            }
        } else {
            future.complete(null);
        }

        return future
                .orTimeout(5, TimeUnit.SECONDS)
                .handle((nil, throwable) -> {
                    if (throwable != null) {
                        log.error("Listening for inventory failed.", throwable);
                    }
                    return true;
                })
                .thenCompose(result -> authorizedBondedRolesService.initialize())
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