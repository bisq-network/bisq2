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
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
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
    @Getter
    private final ServiceProvider serviceProvider;
    @Getter
    private final Observable<State> state = new Observable<>(State.INITIALIZE_APP);

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

    public DesktopApplicationService(String[] args, ShutDownHandler shutDownHandler) {
        super("desktop", args);

        securityService = new SecurityService(persistenceService);
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
                securityService.getProofOfWorkService());

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
                identityService,
                networkService,
                bondedRolesService,
                securityService.getProofOfWorkService());

        settingsService = new SettingsService(persistenceService);

        sendNotificationService = new SendNotificationService(config.getBaseDir(), settingsService);

        offerService = new OfferService(networkService, identityService, persistenceService);

        chatService = new ChatService(persistenceService,
                securityService,
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

        // TODO: Not sure if ServiceProvider is still needed as added BisqEasyService which exposes most of the services.
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
                bisqEasyService);
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
                .thenCompose(result -> sendNotificationService.initialize()) // We initialize after chatService to avoid flooding the notification center
                .thenCompose(result -> supportService.initialize())
                .thenCompose(result -> tradeService.initialize())
                .thenCompose(result -> updaterService.initialize())
                .thenCompose(result -> bisqEasyService.initialize())
                .orTimeout(5, TimeUnit.MINUTES)
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        if (result != null && result) {
                            setState(State.APP_INITIALIZED);
                            log.info("ApplicationService initialized");
                            return true;
                        } else {
                            log.error("Initializing applicationService failed");
                        }
                    } else {
                        log.error("Initializing applicationService failed", throwable);
                    }
                    setState(State.FAILED);
                    return false;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        // We shut down services in opposite order as they are initialized
        return supplyAsync(() -> bisqEasyService.shutdown()
                .thenCompose(result -> updaterService.shutdown())
                .thenCompose(result -> tradeService.shutdown())
                .thenCompose(result -> supportService.shutdown())
                .thenCompose(result -> chatService.shutdown())
                .thenCompose(result -> offerService.shutdown())
                .thenCompose(result -> sendNotificationService.shutdown())
                .thenCompose(result -> settingsService.shutdown())
                .thenCompose(result -> userService.shutdown())
                .thenCompose(result -> contractService.shutdown())
                .thenCompose(result -> accountService.shutdown())
                .thenCompose(result -> bondedRolesService.shutdown())
                .thenCompose(result -> identityService.shutdown())
                .thenCompose(result -> networkService.shutdown())
                .thenCompose(result -> {
                    return walletService.map(Service::shutdown)
                            .orElse(CompletableFuture.completedFuture(true));
                })
                .thenCompose(result -> securityService.shutdown())
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((result, throwable) -> throwable == null && result != null && result)
                .join());
    }

    private void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
    }
}
