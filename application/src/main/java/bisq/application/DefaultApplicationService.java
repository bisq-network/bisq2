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
import bisq.chat.ChatService;
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
import bisq.support.SupportService;
import bisq.user.UserService;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
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

    public enum State {
        NEW,
        START_NETWORK,
        NETWORK_STARTED,
        INITIALIZE_COMPLETED,
        INITIALIZE_FAILED
    }

    private final SecurityService securityService;
    private final ElectrumWalletService walletService;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final OracleService oracleService;
    private final AccountService accountService;
    private final OfferService offerService;
    private final UserService userService;
    private final ChatService chatService;
    private final SettingsService settingsService;
    private final ProtocolService protocolService;
    private final SupportService supportService;

    private final Observable<State> state = new Observable<>(State.NEW);

    public DefaultApplicationService(String[] args) {
        super("default", args);
        securityService = new SecurityService(persistenceService);
        walletService = new ElectrumWalletService(config.isWalletEnabled(), Path.of(config.getBaseDir()));

        networkService = new NetworkService(NetworkServiceConfig.from(config.getBaseDir(), getConfig("network")),
                persistenceService,
                securityService.getKeyPairService(),
                securityService.getProofOfWorkService());

        identityService = new IdentityService(IdentityService.Config.from(getConfig("identity")),
                persistenceService,
                securityService,
                networkService);

        oracleService = new OracleService(OracleService.Config.from(getConfig("oracle")), config.getVersion(), networkService);

        accountService = new AccountService(networkService, persistenceService, identityService);

        offerService = new OfferService(networkService, identityService, persistenceService);

        userService = new UserService(UserService.Config.from(getConfig("user")),
                persistenceService,
                getKeyPairService(),
                identityService,
                networkService,
                securityService.getProofOfWorkService());

        chatService = new ChatService(persistenceService,
                securityService.getProofOfWorkService(),
                networkService,
                userService.getUserIdentityService(),
                userService.getUserProfileService());

        supportService = new SupportService(networkService, chatService, userService);

        settingsService = new SettingsService(persistenceService);

        protocolService = new ProtocolService(networkService, identityService, persistenceService, offerService.getOpenOfferService());
    }


    // At the moment we do not initialize in parallel to keep thing simple, but can be optimized later
    @Override
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> walletService.initialize())
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        setState(State.START_NETWORK);
                    } else {
                        log.error("Error at walletService.initialize", throwable);
                    }
                })
                .thenCompose(result -> networkService.initialize())
                .whenComplete((r, t) -> setState(State.NETWORK_STARTED))
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> oracleService.initialize())
                .thenCompose(result -> accountService.initialize())
                .thenCompose(result -> offerService.initialize())
                .thenCompose(result -> userService.initialize())
                .thenCompose(result -> chatService.initialize())
                .thenCompose(result -> supportService.initialize())
                .thenCompose(result -> settingsService.initialize())
                .thenCompose(result -> protocolService.initialize())
                .orTimeout(5, TimeUnit.MINUTES)
                .whenComplete((success, throwable) -> {
                    if (success) {
                        setState(State.INITIALIZE_COMPLETED);
                        log.info("NetworkApplicationService initialized");
                    } else {
                        setState(State.INITIALIZE_FAILED);
                        log.error("Initializing networkApplicationService failed", throwable);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        // We shut down services in opposite order as they are initialized
        return supplyAsync(() -> protocolService.shutdown()
                        .thenCompose(result -> settingsService.shutdown())
                        .thenCompose(result -> supportService.shutdown())
                        .thenCompose(result -> chatService.shutdown())
                        .thenCompose(result -> userService.shutdown())
                        .thenCompose(result -> offerService.shutdown())
                        .thenCompose(result -> accountService.shutdown())
                        .thenCompose(result -> oracleService.shutdown())
                        .thenCompose(result -> identityService.shutdown())
                        .thenCompose(result -> networkService.shutdown())
                        .thenCompose(result -> walletService.shutdown())
                        .thenCompose(result -> securityService.shutdown())
                        .orTimeout(10, TimeUnit.SECONDS)
                        .handle((result, throwable) -> throwable == null)
                        .join(),
                ExecutorFactory.newSingleThreadExecutor("Shutdown"));
    }

    public KeyPairService getKeyPairService() {
        return securityService.getKeyPairService();
    }


    private void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
    }
}
