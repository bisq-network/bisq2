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
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.offer.OfferService;
import bisq.oracle.OracleService;
import bisq.protocol.ProtocolService;
import bisq.security.KeyPairService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.social.SocialService;
import bisq.wallets.bitcoind.BitcoinWalletService;
import bisq.wallets.elementsd.LiquidWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
public class DefaultApplicationService extends ApplicationService {
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
        super("Bisq", args);

        bitcoinWalletService = new BitcoinWalletService(persistenceService, config.getBaseDir(), config.isBitcoindRegtest());
        liquidWalletService = new LiquidWalletService(persistenceService, config.getBaseDir(), config.isElementsdRegtest());

        securityService = new SecurityService(persistenceService);

        networkService = new NetworkService(NetworkServiceConfig.from(config.getBaseDir(), getConfig("network")),
                persistenceService,
                securityService.getKeyPairService());

        identityService = new IdentityService(IdentityService.Config.from(getConfig("identity")),
                persistenceService,
                securityService,
                networkService);

        oracleService = new OracleService(OracleService.Config.from(getConfig("oracle")),
                config.getVersion(),
                networkService,
                identityService,
                persistenceService);
        accountService = new AccountService(networkService, persistenceService, identityService);

        offerService = new OfferService(networkService, identityService, persistenceService);

        socialService = new SocialService(SocialService.Config.from(getConfig("social")),
                persistenceService,
                identityService,
                securityService,
                oracleService.getOpenTimestampService(),
                networkService);
        settingsService = new SettingsService(persistenceService);

        protocolService = new ProtocolService(networkService, identityService, persistenceService, offerService.getOpenOfferService());
    }

   

    // At the moment we do not initialize in parallel to keep thing simple, but can be optimized later
    @Override
    public CompletableFuture<Boolean> initialize() {
        return bitcoinWalletService.initialize()
                .thenCompose(result -> liquidWalletService.initialize())
                .thenCompose(result -> securityService.initialize())
                .thenCompose(result -> networkService.initialize())
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> oracleService.initialize())
                .thenCompose(result -> accountService.initialize())
                .thenCompose(result -> offerService.initialize())
                .thenCompose(result -> socialService.initialize())
                .thenCompose(result -> settingsService.initialize())
                .thenCompose(result -> protocolService.initialize())
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
        return supplyAsync(() -> protocolService.shutdown()
                        .thenCompose(result -> settingsService.shutdown())
                        .thenCompose(result -> socialService.shutdown())
                        .thenCompose(result -> offerService.shutdown())
                        .thenCompose(result -> accountService.shutdown())
                        .thenCompose(result -> oracleService.shutdown())
                        .thenCompose(result -> identityService.shutdown())
                        .thenCompose(result -> networkService.shutdown())
                        .thenCompose(result -> securityService.shutdown())
                        .thenCompose(result -> liquidWalletService.shutdown())
                        .thenCompose(result -> bitcoinWalletService.shutdown())
                        .orTimeout(5, TimeUnit.MINUTES)
                        .handle((result, throwable) -> throwable == null)
                        .join(),
                ExecutorFactory.newSingleThreadExecutor("Shutdown"));
    }

    public KeyPairService getKeyPairService() {
        return securityService.getKeyPairService();
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
