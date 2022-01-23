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

import bisq.account.AccountService;
import bisq.account.settlement.Account;
import bisq.account.settlement.AccountPayload;
import bisq.account.settlement.FiatSettlement;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.util.CompletableFutureUtils;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfigFactory;
import bisq.offer.OfferRepository;
import bisq.offer.OfferService;
import bisq.offer.OpenOfferService;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.oracle.marketprice.MarketPriceServiceConfigFactory;
import bisq.persistence.PersistenceService;
import bisq.protocol.ProtocolService;
import bisq.security.KeyPairService;
import bisq.social.chat.ChatService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static bisq.common.util.OsUtils.EXIT_FAILURE;
import static bisq.common.util.OsUtils.EXIT_SUCCESS;
import static java.util.concurrent.CompletableFuture.runAsync;

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
    private final IdentityService identityService;
    private final MarketPriceService marketPriceService;
    private final ApplicationOptions applicationOptions;
    private final PersistenceService persistenceService;
    private final UserService userService;
    private final ChatService chatService;
    private final ProtocolService protocolService;
    private final OfferRepository offerRepository;
    private final AccountService accountService;

    public DefaultServiceProvider(ApplicationOptions applicationOptions, String[] args) {
        super("Bisq");
        this.applicationOptions = applicationOptions;

        Locale locale = applicationOptions.getLocale();
        LocaleRepository.setDefaultLocale(locale);
        Res.initialize(locale);
        FiatCurrencyRepository.applyLocale(locale);

        persistenceService = new PersistenceService(applicationOptions.baseDir());
        keyPairService = new KeyPairService(persistenceService);

        userService = new UserService(persistenceService);


        NetworkService.Config networkServiceConfig = NetworkServiceConfigFactory.getConfig(applicationOptions.baseDir(),
                getConfig("bisq.networkServiceConfig"));
        networkService = new NetworkService(networkServiceConfig, persistenceService, keyPairService);

        IdentityService.Config identityServiceConfig = IdentityService.Config.from(getConfig("bisq.identityServiceConfig"));
        identityService = new IdentityService(persistenceService, keyPairService, networkService, identityServiceConfig);

        chatService = new ChatService(persistenceService, identityService, networkService);

        accountService= new AccountService(persistenceService);

        // add data use case is not available yet at networkService
        offerService = new OfferService(networkService, identityService);
        openOfferService = new OpenOfferService(networkService);
        offerRepository= new OfferRepository(networkService);

        MarketPriceService.Config marketPriceServiceConf = MarketPriceServiceConfigFactory.getConfig();
        marketPriceService = new MarketPriceService(marketPriceServiceConf, networkService, ApplicationVersion.VERSION);
       // offerPresentationService = new OfferPresentationService(offerService, marketPriceService);

        protocolService= new ProtocolService();


        // add dummy accounts
        accountService.addAccount(new Account("SEPA-account-1",
                new AccountPayload(FiatSettlement.SEPA.getMethod().name(), "John Smith", "1234567890", "9876543")));
        accountService.addAccount(new Account("SEPA-account-2",
                new AccountPayload(FiatSettlement.SEPA.getMethod().name(),"Mary Smith", "00000222229999", "88888")));
        accountService.addAccount(new Account("revolut-account",
                new AccountPayload(FiatSettlement.REVOLUT.getMethod().name(),"Mary Smith", "00000222229999", "88888")));
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
                .thenCompose(result -> networkService.bootstrapToNetwork())
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> marketPriceService.initialize())
                .thenCompose(result -> CompletableFutureUtils.allOf(offerService.initialize(),
                        openOfferService.initialize(),
                        offerRepository.initialize()))
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
        offerRepository.shutdown();
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
