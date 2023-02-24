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
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.offer.OfferService;
import bisq.oracle.OracleService;
import bisq.presentation.notifications.NotificationsService;
import bisq.protocol.ProtocolService;
import bisq.security.KeyPairService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.user.UserService;
import bisq.wallets.bitcoind.BitcoinWalletService;
import bisq.wallets.core.WalletService;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    public enum State {
        INITIALIZE_APP,
        INITIALIZE_NETWORK,
        INITIALIZE_WALLET,
        INITIALIZE_SERVICES,
        APP_INITIALIZED,
        FAILED
    }

    private final SecurityService securityService;
    private final Optional<WalletService> walletService;
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
    private final NotificationsService notificationsService;

    private final Observable<State> state = new Observable<>(State.INITIALIZE_APP);

    public DefaultApplicationService(String[] args) {
        super("default", args);
        securityService = new SecurityService(persistenceService);
        com.typesafe.config.Config bitcoinWalletConfig = getConfig("bitcoinWallet");
        BitcoinWalletSelection bitcoinWalletSelection = bitcoinWalletConfig.getEnum(BitcoinWalletSelection.class, "bitcoinWalletSelection");
        switch (bitcoinWalletSelection) {
            case BITCOIND:
                walletService = Optional.of(new BitcoinWalletService(BitcoinWalletService.Config.from(bitcoinWalletConfig.getConfig("bitcoind")), getPersistenceService()));
                break;
            case ELECTRUM:
                walletService = Optional.of(new ElectrumWalletService(ElectrumWalletService.Config.from(bitcoinWalletConfig.getConfig("electrum")), Path.of(config.getBaseDir())));
                break;
            case NONE:
            default:
                walletService = Optional.empty();
                break;
        }

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

        notificationsService = new NotificationsService(persistenceService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return securityService.initialize()
                .thenCompose(result -> {
                    setState(State.INITIALIZE_NETWORK);

                    CompletableFuture<Boolean> networkFuture = networkService.initialize();
                    CompletableFuture<Boolean> walletFuture = walletService.map(Service::initialize)
                            .orElse(CompletableFuture.completedFuture(true));

                    networkFuture.whenComplete((r, throwable) -> {
                        if (throwable != null) {
                            log.error("Error at networkFuture.initialize", throwable);
                        } else if (!walletFuture.isDone()) {
                            setState(State.INITIALIZE_WALLET);
                        }
                    });
                    walletFuture.whenComplete((r, throwable) -> {
                        if (throwable != null) {
                            log.error("Error at walletService.initialize", throwable);
                        }
                    });
                    return CompletableFutureUtils.allOf(walletFuture, networkFuture).thenApply(list -> true);
                })
                .whenComplete((r, throwable) -> {
                    if (throwable == null) {
                        setState(State.INITIALIZE_SERVICES);
                    }
                })
                .thenCompose(result -> identityService.initialize())
                .thenCompose(result -> oracleService.initialize())
                .thenCompose(result -> accountService.initialize())
                .thenCompose(result -> offerService.initialize())
                .thenCompose(result -> userService.initialize())
                .thenCompose(result -> chatService.initialize())
                .thenCompose(result -> supportService.initialize())
                .thenCompose(result -> settingsService.initialize())
                .thenCompose(result -> protocolService.initialize())
                .thenCompose(result -> notificationsService.initialize())
                .orTimeout(5, TimeUnit.MINUTES)
                .whenComplete((success, throwable) -> {
                    if (throwable == null) {
                        if (success) {
                            setState(State.APP_INITIALIZED);
                            log.info("ApplicationService initialized");
                        } else {
                            setState(State.FAILED);
                            log.error("Initializing applicationService failed");
                        }
                    } else {
                        setState(State.FAILED);
                        log.error("Initializing applicationService failed", throwable);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        // We shut down services in opposite order as they are initialized
        return supplyAsync(() -> notificationsService.shutdown()
                        .thenCompose(result -> protocolService.shutdown())
                        .thenCompose(result -> settingsService.shutdown())
                        .thenCompose(result -> supportService.shutdown())
                        .thenCompose(result -> chatService.shutdown())
                        .thenCompose(result -> userService.shutdown())
                        .thenCompose(result -> offerService.shutdown())
                        .thenCompose(result -> accountService.shutdown())
                        .thenCompose(result -> oracleService.shutdown())
                        .thenCompose(result -> identityService.shutdown())
                        .thenCompose(result -> networkService.shutdown())
                        .thenCompose(result -> {
                            return walletService.map(Service::shutdown)
                                    .orElse(CompletableFuture.completedFuture(true));
                        })
                        .thenCompose(result -> securityService.shutdown())
                        .orTimeout(10, TimeUnit.SECONDS)
                        .handle((result, throwable) -> throwable == null)
                        .join());
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
