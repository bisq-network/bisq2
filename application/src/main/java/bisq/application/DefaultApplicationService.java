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
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.offer.OfferService;
import bisq.oracle.OracleService;
import bisq.persistence.PersistenceService;
import bisq.protocol.ProtocolService;
import bisq.security.KeyPairService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.social.SocialService;
import bisq.wallets.bitcoind.BitcoinWalletService;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.elementsd.LiquidWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 */
@Getter
@Slf4j
public class DefaultApplicationService extends ServiceProvider {
    public static final ExecutorService DISPATCHER = ExecutorFactory.newSingleThreadExecutor("DefaultApplicationService.dispatcher");
    private final OracleService oracleService;

    public enum State {
        CREATED,
        SECURITY_SERVICE_INITIALIZED,
        NETWORK_BOOTSTRAPPED,
        IDENTITY_SERVICE_INITIALIZED,
        ORACLE_SERVICE_INITIALIZED,
        DAO_BRIDGE_SERVICE_INITIALIZED,
        MARKET_PRICE_SERVICE_INITIALIZED,
        ACCOUNT_AGE_WITNESS_SERVICE_INITIALIZED,
        PROTOCOL_SERVICE_INITIALIZED,
        SOCIAL_SERVICE_INITIALIZED,
        INIT_COMPLETE,
        INIT_FAILED
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ApplicationConfig appConfig;
    private final PersistenceService persistenceService;
    private final SettingsService settingsService;
    private final ProtocolService protocolService;
    private final AccountService accountService;
    private final BitcoinWalletService bitcoinWalletService;
    private final LiquidWalletService liquidWalletService;
    private final OfferService offerService;
    private final SocialService socialService;
    private final SecurityService securityService;
    private final Observable<State> state = new Observable<>(State.CREATED);

    public DefaultApplicationService(String[] args) {
        super("Bisq");

        appConfig = ApplicationConfigFactory.getConfig(getConfig("bisq.application"), args);
        ApplicationSetup.initialize(appConfig);

        persistenceService = new PersistenceService(appConfig.getBaseDir());

        securityService = new SecurityService(persistenceService);

        settingsService = new SettingsService(persistenceService);

        networkService = new NetworkService(NetworkServiceConfig.from(appConfig.getBaseDir(), getConfig("bisq.network")),
                persistenceService,
                securityService.getKeyPairService());

        identityService = new IdentityService(IdentityService.Config.from(getConfig("bisq.identity")),
                persistenceService,
                securityService,
                networkService);

        oracleService = new OracleService(OracleService.Config.from(getConfig("bisq.oracle")),
                appConfig.getVersion(),
                networkService,
                identityService,
                persistenceService);

        accountService = new AccountService(networkService, persistenceService, identityService);

        socialService = new SocialService(SocialService.Config.from(getConfig("bisq.social")),
                persistenceService,
                identityService,
                securityService,
                oracleService.getOpenTimestampService(),
                networkService);

        offerService = new OfferService(networkService, identityService, persistenceService);

        protocolService = new ProtocolService(networkService, identityService, persistenceService, offerService.getOpenOfferService());

        bitcoinWalletService = new BitcoinWalletService(persistenceService, appConfig.getBaseDir(), appConfig.isBitcoindRegtest());

        liquidWalletService = new LiquidWalletService(persistenceService, appConfig.getBaseDir(), appConfig.isElementsdRegtest());
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
        securityService.initialize()
                .thenCompose(result -> networkService.bootstrapToNetwork())
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> oracleService.initialize())
                .thenCompose(result -> accountService.initialize())
                .thenCompose(result -> protocolService.initialize())
                .thenCompose(result -> offerService.initialize())
                .thenCompose(result -> protocolService.initialize())
                .thenCompose(result -> protocolService.initialize())
                .thenCompose(result -> socialService.initialize());


        return CompletableFuture.completedFuture(true)
                .thenCompose(result -> setStateAfter(securityService.initialize(), State.SECURITY_SERVICE_INITIALIZED))
                .thenCompose(result -> setStateAfter(networkService.bootstrapToNetwork(), State.NETWORK_BOOTSTRAPPED))
                .thenCompose(result -> setStateAfter(identityService.initialize(), State.IDENTITY_SERVICE_INITIALIZED))
                .thenCompose(result -> setStateAfter(oracleService.initialize(), State.ORACLE_SERVICE_INITIALIZED))
                .thenCompose(result -> setStateAfter(accountService.initialize(), State.ACCOUNT_AGE_WITNESS_SERVICE_INITIALIZED))
                .thenCompose(result -> setStateAfterList(protocolService.initialize(), State.PROTOCOL_SERVICE_INITIALIZED))
                .thenCompose(result -> setStateAfter(socialService.initialize(), State.SOCIAL_SERVICE_INITIALIZED))
                .thenCompose(result -> CompletableFutureUtils.allOf(
                        offerService.initialize(),
                        bitcoinWalletService.initialize(),
                        liquidWalletService.initialize()))
                // TODO Needs to increase if using embedded I2P router (i2p internal bootstrap timeouts after 5 min)
                .orTimeout(5, TimeUnit.MINUTES)
                .thenApply(list -> list.stream().allMatch(e -> e))
                .whenComplete((list, throwable) -> {
                    if (throwable == null) {
                        log.info("All application services initialized");
                        setState(State.INIT_COMPLETE);
                    } else {
                        log.error("Error at initializing application services", throwable);
                        setState(State.INIT_FAILED);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        //todo add service shutdown calls
        return supplyAsync(() -> socialService.shutdown()
                        .thenCompose(list -> CompletableFutureUtils.allOf(
                                networkService.shutdown())
                        )
                        .thenCompose(list -> CompletableFutureUtils.allOf(
                                bitcoinWalletService.shutdown(),
                                liquidWalletService.shutdown())
                        )
                        .handle((list, throwable) -> throwable == null)
                        .join(),
                ExecutorFactory.newSingleThreadExecutor("Shutdown"));
    }

    public KeyPairService getKeyPairService() {
        return securityService.getKeyPairService();
    }

    //todo move to Bisq.conf
    private Optional<RpcConfig> createRegtestWalletConfig(int port) {
        var walletConfig = RpcConfig.builder()
                .hostname("localhost")
                .port(port)
                .user("bisq")
                .password("bisq")
                .build();
        return Optional.of(walletConfig);
    }

    private CompletableFuture<Boolean> setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);

        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> setStateAfter(CompletableFuture<Boolean> initTask, State stateAfter) {
        return initTask.thenCompose(result -> setState(stateAfter));
    }

    private CompletableFuture<Boolean> setStateAfterList(CompletableFuture<List<Boolean>> initTaskList, State stateAfter) {
        return initTaskList.thenCompose(result -> setState(stateAfter));
    }
}
