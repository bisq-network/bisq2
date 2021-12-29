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
import network.misq.id.IdentityRepository;
import network.misq.network.NetworkService;
import network.misq.network.NetworkServiceConfigFactory;
import network.misq.network.p2p.MockNetworkService;
import network.misq.offer.MarketPriceService;
import network.misq.offer.MarketPriceServiceConfigFactory;
import network.misq.offer.OfferRepository;
import network.misq.offer.OpenOfferRepository;
import network.misq.presentation.offer.OfferEntityRepository;
import network.misq.security.KeyPairRepository;
import network.misq.security.KeyPairRepositoryConfigFactory;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the completely setup instances to other clients (Api)
 */
@Getter
@Slf4j
public class DefaultApplicationSetup extends ApplicationSetup {
    private final KeyPairRepository keyPairRepository;
    private final NetworkService networkService;
    private final OfferRepository offerRepository;
    private final OpenOfferRepository openOfferRepository;
    private final OfferEntityRepository offerEntityRepository;
    private final IdentityRepository identityRepository;
    private final MarketPriceService marketPriceService;
    private final ApplicationOptions applicationOptions;

    public DefaultApplicationSetup(ApplicationOptions applicationOptions, String[] args) {
        super("Misq");
        this.applicationOptions = applicationOptions;

        Locale locale = applicationOptions.getLocale();
        LocaleRepository.setDefaultLocale(locale);
        FiatCurrencyRepository.applyLocale(locale);

        KeyPairRepository.Conf keyPairRepositoryConf = new KeyPairRepositoryConfigFactory(applicationOptions.baseDir()).get();
        keyPairRepository = new KeyPairRepository(keyPairRepositoryConf);

        NetworkService.Config networkServiceConfig = new NetworkServiceConfigFactory(applicationOptions.baseDir(),
                getConfig("misq.networkServiceConfig"), args).get();
        networkService = new NetworkService(networkServiceConfig, keyPairRepository);

        identityRepository = new IdentityRepository(networkService);

        // add data use case is not available yet at networkService
        MockNetworkService mockNetworkService = new MockNetworkService();
        offerRepository = new OfferRepository(mockNetworkService);
        openOfferRepository = new OpenOfferRepository(mockNetworkService);


        MarketPriceService.Config marketPriceServiceConf = new MarketPriceServiceConfigFactory().get();
        marketPriceService = new MarketPriceService(marketPriceServiceConf, networkService, Version.VERSION);
        offerEntityRepository = new OfferEntityRepository(offerRepository, marketPriceService);
    }

    /**
     * Initializes all domain objects, services and repositories.
     * We do in parallel as far as possible. If there are dependencies we chain those as sequence.
     */
    public CompletableFuture<Boolean> initialize() {
        return keyPairRepository.initialize()
                .thenCompose(result -> identityRepository.initialize())
                .thenCompose(result -> networkService.initialize() // We need to get at least the default nodes server initialized before we move on
                        .whenComplete((res, t) -> networkService.initializePeerGroup())) // But we do not wait for the initializePeerGroup 
                .thenCompose(result -> marketPriceService.initialize())
                .thenCompose(result -> CompletableFutureUtils.allOf(offerRepository.initialize(),
                        openOfferRepository.initialize(),
                        offerEntityRepository.initialize()))
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((list, throwable) -> {
                    if (throwable != null) {
                        log.error("Error at startup", throwable);
                    } else {
                        log.error("Application initialized successfully");
                    }
                }).thenApply(list -> list.stream().allMatch(e -> e));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        //todo maybe chain async shutdown calls
        keyPairRepository.shutdown();
        identityRepository.shutdown();
        marketPriceService.shutdown();
        offerRepository.shutdown();
        openOfferRepository.shutdown();
        offerEntityRepository.shutdown();

        return networkService.shutdown();
    }
}
