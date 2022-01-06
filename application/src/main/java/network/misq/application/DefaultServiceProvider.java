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

package network.misq.application;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.currency.FiatCurrencyRepository;
import network.misq.common.locale.LocaleRepository;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.i18n.Res;
import network.misq.identity.IdentityService;
import network.misq.network.NetworkService;
import network.misq.network.NetworkServiceConfigFactory;
import network.misq.network.p2p.MockNetworkService;
import network.misq.offer.MarketPriceService;
import network.misq.offer.MarketPriceServiceConfigFactory;
import network.misq.offer.OfferService;
import network.misq.offer.OpenOfferService;
import network.misq.persistence.PersistenceService;
import network.misq.presentation.offer.OfferEntityService;
import network.misq.security.KeyPairService;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.runAsync;
import static network.misq.common.util.OsUtils.EXIT_FAILURE;
import static network.misq.common.util.OsUtils.EXIT_SUCCESS;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the completely setup instances to other clients (Api)
 */
@Getter
@Slf4j
public class DefaultServiceProvider extends ServiceProvider {
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    private final OfferService offerService;
    private final OpenOfferService openOfferService;
    private final OfferEntityService offerEntityService;
    private final IdentityService identityService;
    private final MarketPriceService marketPriceService;
    private final ApplicationOptions applicationOptions;
    private final PersistenceService persistenceService;

    public DefaultServiceProvider(ApplicationOptions applicationOptions, String[] args) {
        super("Misq");
        this.applicationOptions = applicationOptions;

        Locale locale = applicationOptions.getLocale();
        LocaleRepository.setDefaultLocale(locale);
        Res.initialize(locale);
        FiatCurrencyRepository.applyLocale(locale);

        persistenceService = new PersistenceService(applicationOptions.baseDir());
        keyPairService = new KeyPairService(persistenceService);

        identityService = new IdentityService(persistenceService);

        NetworkService.Config networkServiceConfig = NetworkServiceConfigFactory.getConfig(applicationOptions.baseDir(),
                getConfig("misq.networkServiceConfig"));
        networkService = new NetworkService(networkServiceConfig, keyPairService, persistenceService);


        // add data use case is not available yet at networkService
        MockNetworkService mockNetworkService = new MockNetworkService();
        offerService = new OfferService(mockNetworkService);
        openOfferService = new OpenOfferService(mockNetworkService);


        MarketPriceService.Config marketPriceServiceConf = MarketPriceServiceConfigFactory.getConfig();
        marketPriceService = new MarketPriceService(marketPriceServiceConf, networkService, Version.VERSION);
        offerEntityService = new OfferEntityService(offerService, marketPriceService);
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return persistenceService.readAllPersisted();
    }

    /**
     * Initializes all domain objects, services and repositories.
     * We do in parallel as far as possible. If there are dependencies we chain those as sequence.
     */
    @Override
    public CompletableFuture<Boolean> initialize() {
        return keyPairService.initialize()
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> networkService.bootstrap())
                .thenCompose(result -> marketPriceService.initialize())
                .thenCompose(result -> CompletableFutureUtils.allOf(offerService.initialize(),
                        openOfferService.initialize(),
                        offerEntityService.initialize()))
                .orTimeout(120, TimeUnit.SECONDS)
                .whenComplete((list, throwable) -> {
                    if (throwable != null) {
                        log.error("Error at startup", throwable);
                    } else {
                        log.info("Application initialized successfully");
                    }
                }).thenApply(list -> list.stream().allMatch(e -> e));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        //todo maybe chain async shutdown calls
        keyPairService.shutdown();
        identityService.shutdown();
        marketPriceService.shutdown();
        offerService.shutdown();
        openOfferService.shutdown();
        offerEntityService.shutdown();
        return networkService.shutdown()
                .whenComplete((__, throwable) -> {
                    if (throwable != null) {
                        log.error("Error at shutdown", throwable);
                        System.exit(EXIT_FAILURE);
                    } else {
                        // In case the application is a JavaFXApplication give it chance to trigger the exit
                        // via Platform.exit()
                        runAsync(() -> System.exit(EXIT_SUCCESS));
                    }
                });
    }
}
