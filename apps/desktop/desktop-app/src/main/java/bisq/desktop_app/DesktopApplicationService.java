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

package bisq.desktop_app;

import bisq.account.AccountService;
import bisq.application.ApplicationService;
import bisq.application.ShutDownHandler;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.ExceptionUtil;
import bisq.contract.ContractService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.State;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.offer.OfferService;
import bisq.presentation.notifications.SendNotificationService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.updater.UpdaterService;
import bisq.user.UserService;
import bisq.wallets.bitcoind.BitcoinWalletService;
import bisq.wallets.core.BitcoinWalletSelection;
import bisq.wallets.core.WalletService;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class DesktopApplicationService extends ApplicationService {
    public static final long STARTUP_TIMEOUT_SEC = 300;
    public static final long SHUTDOWN_TIMEOUT_SEC = 10;

    @Getter
    private final ServiceProvider serviceProvider;
    @Getter
    private final Observable<State> state = new Observable<>(State.INITIALIZE_APP);
    @Getter
    private final Observable<String> shutDownErrorMessage = new Observable<>();
    @Getter
    private final Observable<String> startupErrorMessage = new Observable<>();

    private final SecurityService securityService;
    private final Optional<WalletService> walletService;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final BondedRolesService bondedRolesService;
    private final AccountService accountService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final UserService userService;
    private final ChatService chatService;
    private final SettingsService settingsService;
    private final SupportService supportService;
    private final SendNotificationService sendNotificationService;
    private final TradeService tradeService;
    private final UpdaterService updaterService;
    private final BisqEasyService bisqEasyService;
    private final AlertNotificationsService alertNotificationsService;

    public DesktopApplicationService(String[] args, ShutDownHandler shutDownHandler) {
        super("desktop", args);

        securityService = new SecurityService(persistenceService, SecurityService.Config.from(getConfig("security")));
        com.typesafe.config.Config bitcoinWalletConfig = getConfig("bitcoinWallet");
        BitcoinWalletSelection bitcoinWalletSelection = bitcoinWalletConfig.getEnum(BitcoinWalletSelection.class, "bitcoinWalletSelection");
        switch (bitcoinWalletSelection) {
            case BITCOIND:
                walletService = Optional.of(new BitcoinWalletService(BitcoinWalletService.Config.from(bitcoinWalletConfig.getConfig("bitcoind")), getPersistenceService()));
                break;
            case ELECTRUM:
                walletService = Optional.of(new ElectrumWalletService(ElectrumWalletService.Config.from(bitcoinWalletConfig.getConfig("electrum")), config.getBaseDir()));
                break;
            case NONE:
            default:
                walletService = Optional.empty();
                break;
        }

        networkService = new NetworkService(NetworkServiceConfig.from(config.getBaseDir(),
                getConfig("network")),
                persistenceService,
                securityService.getKeyBundleService(),
                securityService.getHashCashProofOfWorkService(),
                securityService.getEquihashProofOfWorkService());

        identityService = new IdentityService(persistenceService,
                securityService.getKeyBundleService(),
                networkService);

        bondedRolesService = new BondedRolesService(BondedRolesService.Config.from(getConfig("bondedRoles")),
                config.getVersion(),
                getPersistenceService(),
                networkService);

        accountService = new AccountService(persistenceService);

        contractService = new ContractService(securityService);

        userService = new UserService(UserService.Config.from(getConfig("user")),
                persistenceService,
                securityService,
                identityService,
                networkService,
                bondedRolesService);

        settingsService = new SettingsService(persistenceService);

        sendNotificationService = new SendNotificationService(config.getBaseDir(), settingsService);

        offerService = new OfferService(networkService, identityService, persistenceService);

        chatService = new ChatService(persistenceService,
                networkService,
                userService,
                settingsService,
                sendNotificationService);

        supportService = new SupportService(SupportService.Config.from(getConfig("support")),
                persistenceService,
                networkService,
                chatService,
                userService,
                bondedRolesService);

        tradeService = new TradeService(networkService, identityService, persistenceService, offerService,
                contractService, supportService, chatService, bondedRolesService, userService, settingsService);

        updaterService = new UpdaterService(getConfig(), settingsService, bondedRolesService.getReleaseNotificationsService());

        bisqEasyService = new BisqEasyService(persistenceService,
                securityService,
                walletService,
                networkService,
                identityService,
                bondedRolesService,
                accountService,
                offerService,
                contractService,
                userService,
                chatService,
                settingsService,
                supportService,
                sendNotificationService,
                tradeService);

        alertNotificationsService = new AlertNotificationsService(settingsService, bondedRolesService.getAlertService());

        // TODO (refactor, low prio): Not sure if ServiceProvider is still needed as we added BisqEasyService which exposes most of the services.
        serviceProvider = new ServiceProvider(shutDownHandler,
                getConfig(),
                persistenceService,
                securityService,
                walletService,
                networkService,
                identityService,
                bondedRolesService,
                accountService,
                offerService,
                contractService,
                userService,
                chatService,
                settingsService,
                supportService,
                sendNotificationService,
                tradeService,
                updaterService,
                bisqEasyService,
                alertNotificationsService);
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
                .thenCompose(result -> bondedRolesService.initialize())
                .thenCompose(result -> accountService.initialize())
                .thenCompose(result -> contractService.initialize())
                .thenCompose(result -> userService.initialize())
                .thenCompose(result -> settingsService.initialize())
                .thenCompose(result -> offerService.initialize())
                .thenCompose(result -> chatService.initialize())
                .thenCompose(result -> sendNotificationService.initialize())
                .thenCompose(result -> supportService.initialize())
                .thenCompose(result -> tradeService.initialize())
                .thenCompose(result -> updaterService.initialize())
                .thenCompose(result -> bisqEasyService.initialize())
                .thenCompose(result -> alertNotificationsService.initialize())
                .orTimeout(STARTUP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        if (result != null && result) {
                            setState(State.APP_INITIALIZED);
                            log.info("ApplicationService initialized");
                            return true;
                        } else {
                            startupErrorMessage.set("Initializing applicationService failed with result=false");
                            log.error(startupErrorMessage.get());
                        }
                    } else {
                        log.error("Initializing applicationService failed", throwable);
                        startupErrorMessage.set(ExceptionUtil.getMessageOrToString(throwable));
                    }
                    setState(State.FAILED);
                    return false;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        // We shut down services in opposite order as they are initialized
        return supplyAsync(() -> bisqEasyService.shutdown()
                .thenCompose(result -> updaterService.shutdown())
                .thenCompose(result -> tradeService.shutdown())
                .thenCompose(result -> supportService.shutdown())
                .thenCompose(result -> sendNotificationService.shutdown())
                .thenCompose(result -> chatService.shutdown())
                .thenCompose(result -> offerService.shutdown())
                .thenCompose(result -> alertNotificationsService.shutdown())
                .thenCompose(result -> settingsService.shutdown())
                .thenCompose(result -> userService.shutdown())
                .thenCompose(result -> contractService.shutdown())
                .thenCompose(result -> accountService.shutdown())
                .thenCompose(result -> bondedRolesService.shutdown())
                .thenCompose(result -> identityService.shutdown())
                .thenCompose(result -> networkService.shutdown())
                .thenCompose(result -> walletService.map(Service::shutdown)
                        .orElse(CompletableFuture.completedFuture(true)))
                .thenCompose(result -> securityService.shutdown())
                .orTimeout(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Error at shutdown", throwable);
                        shutDownErrorMessage.set(ExceptionUtil.getMessageOrToString(throwable));
                        return false;
                    } else if (!result) {
                        shutDownErrorMessage.set("Shutdown failed with result=false");
                        log.error(startupErrorMessage.get());
                        return false;
                    }
                    return true;
                })
                .join());
    }

    private void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
    }
}
