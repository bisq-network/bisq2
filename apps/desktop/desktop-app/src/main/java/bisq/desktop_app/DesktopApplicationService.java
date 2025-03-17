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
import bisq.application.ShutDownHandler;
import bisq.application.State;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.platform.OS;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.ExceptionUtil;
import bisq.contract.ContractService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.webcam.WebcamAppService;
import bisq.evolution.updater.UpdaterService;
import bisq.http_api.HttpApiService;
import bisq.http_api.rest_api.RestApiService;
import bisq.http_api.web_socket.WebSocketService;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.identity.IdentityService;
import bisq.java_se.application.JavaSeApplicationService;
import bisq.network.NetworkService;
import bisq.network.NetworkServiceConfig;
import bisq.offer.OfferService;
import bisq.os_specific.notifications.linux.LinuxNotificationService;
import bisq.os_specific.notifications.osx.OsxNotificationService;
import bisq.os_specific.notifications.other.AwtNotificationService;
import bisq.presentation.notifications.OsSpecificNotificationService;
import bisq.presentation.notifications.SystemNotificationService;
import bisq.security.SecurityService;
import bisq.settings.DontShowAgainService;
import bisq.settings.FavouriteMarketsService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.wallets.core.BitcoinWalletSelection;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 */

@Slf4j
public class DesktopApplicationService extends JavaSeApplicationService {
    public static final long STARTUP_TIMEOUT_SEC = 300;
    public static final long SHUTDOWN_TIMEOUT_SEC = 10;

    @Getter
    private final ServiceProvider serviceProvider;
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
    private final SystemNotificationService systemNotificationService;
    private final TradeService tradeService;
    private final UpdaterService updaterService;
    private final BisqEasyService bisqEasyService;
    private final AlertNotificationsService alertNotificationsService;
    private final FavouriteMarketsService favouriteMarketsService;
    private final DontShowAgainService dontShowAgainService;
    private final WebcamAppService webcamAppService;
    private final HttpApiService httpApiService;
    private final OpenTradeItemsService openTradeItemsService;

    public DesktopApplicationService(String[] args, ShutDownHandler shutDownHandler) {
        super("desktop", args);

        securityService = new SecurityService(persistenceService, SecurityService.Config.from(getConfig("security")));

        com.typesafe.config.Config bitcoinWalletConfig = getConfig("bitcoinWallet");
        BitcoinWalletSelection bitcoinWalletSelection = bitcoinWalletConfig.getEnum(BitcoinWalletSelection.class, "bitcoinWalletSelection");
        //noinspection SwitchStatementWithTooFewBranches
        switch (bitcoinWalletSelection) {
           /* case BITCOIND:
                walletService = Optional.of(new BitcoinWalletService(BitcoinWalletService.Config.from(bitcoinWalletConfig.getConfig("bitcoind")), getPersistenceService()));
                break;
            case ELECTRUM:
                walletService = Optional.of(new ElectrumWalletService(ElectrumWalletService.Config.from(bitcoinWalletConfig.getConfig("electrum")), config.getBaseDir()));
                break;*/
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
                securityService.getEquihashProofOfWorkService(),
                memoryReportService);

        identityService = new IdentityService(persistenceService,
                securityService.getKeyBundleService(),
                networkService);

        bondedRolesService = new BondedRolesService(BondedRolesService.Config.from(getConfig("bondedRoles")),
                getPersistenceService(),
                networkService);

        accountService = new AccountService(persistenceService);

        contractService = new ContractService(securityService);

        userService = new UserService(persistenceService,
                securityService,
                identityService,
                networkService,
                bondedRolesService);

        settingsService = new SettingsService(persistenceService);

        systemNotificationService = new SystemNotificationService(findSystemNotificationDelegate());

        offerService = new OfferService(networkService, identityService, persistenceService);

        chatService = new ChatService(persistenceService,
                networkService,
                userService,
                settingsService,
                systemNotificationService);

        supportService = new SupportService(SupportService.Config.from(getConfig("support")),
                persistenceService,
                networkService,
                chatService,
                userService,
                bondedRolesService);

        tradeService = new TradeService(networkService, identityService, persistenceService, offerService,
                contractService, supportService, chatService, bondedRolesService, userService, settingsService);

        updaterService = new UpdaterService(getConfig(),
                settingsService,
                bondedRolesService.getReleaseNotificationsService(),
                bondedRolesService.getAlertService());

        bisqEasyService = new BisqEasyService(persistenceService,
                securityService,
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
                systemNotificationService,
                tradeService);

        alertNotificationsService = new AlertNotificationsService(settingsService, bondedRolesService.getAlertService());

        favouriteMarketsService = new FavouriteMarketsService(settingsService);

        dontShowAgainService = new DontShowAgainService(settingsService);
        webcamAppService = new WebcamAppService(config);

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
                systemNotificationService,
                tradeService,
                updaterService,
                bisqEasyService,
                alertNotificationsService,
                favouriteMarketsService,
                dontShowAgainService,
                webcamAppService);

        openTradeItemsService = new OpenTradeItemsService(chatService, tradeService, userService);

        var restApiConfig = RestApiService.Config.from(getConfig("restApi"));
        var websocketConfig = WebSocketService.Config.from(getConfig("websocket"));
        httpApiService = new HttpApiService(restApiConfig,
                websocketConfig,
                securityService,
                networkService,
                userService,
                bondedRolesService,
                chatService,
                supportService,
                tradeService,
                settingsService,
                openTradeItemsService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return memoryReportService.initialize()
                .thenCompose(result -> securityService.initialize())
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
                .thenCompose(result -> systemNotificationService.initialize())
                .thenCompose(result -> supportService.initialize())
                .thenCompose(result -> tradeService.initialize())
                .thenCompose(result -> updaterService.initialize())
                .thenCompose(result -> bisqEasyService.initialize())
                .thenCompose(result -> alertNotificationsService.initialize())
                .thenCompose(result -> favouriteMarketsService.initialize())
                .thenCompose(result -> dontShowAgainService.initialize())
                .thenCompose(result -> webcamAppService.initialize())
                .thenCompose(result -> openTradeItemsService.initialize())
                .thenCompose(result -> httpApiService.initialize())
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
                        startupErrorMessage.set(ExceptionUtil.getRootCauseMessage(throwable));
                    }
                    setState(State.FAILED);
                    return false;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        // We shut down services in opposite order as they are initialized
        // In case a shutdown method completes exceptionally we log the error and map the result to `false` to not
        // interrupt the shutdown sequence.
        return supplyAsync(() -> httpApiService.shutdown().exceptionally(this::logError)
                .thenCompose(result -> openTradeItemsService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> webcamAppService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> dontShowAgainService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> favouriteMarketsService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> alertNotificationsService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> bisqEasyService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> updaterService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> tradeService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> supportService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> systemNotificationService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> chatService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> offerService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> settingsService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> userService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> contractService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> accountService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> bondedRolesService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> identityService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> networkService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> walletService.map(service -> service.shutdown().exceptionally(this::logError))
                        .orElse(CompletableFuture.completedFuture(true)))
                .thenCompose(result -> securityService.shutdown().exceptionally(this::logError))
                .thenCompose(result -> memoryReportService.shutdown().exceptionally(this::logError))
                .orTimeout(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        if (result != null && result) {
                            log.info("ApplicationService shutdown completed");
                            return true;
                        } else {
                            startupErrorMessage.set("Shutdown applicationService failed with result=false");
                            log.error(shutDownErrorMessage.get());
                        }
                    } else {
                        log.error("Shutdown applicationService failed", throwable);
                        shutDownErrorMessage.set(ExceptionUtil.getRootCauseMessage(throwable));
                    }
                    return false;
                })
                .join());
    }


    private Optional<OsSpecificNotificationService> findSystemNotificationDelegate() {
        try {
            switch (OS.getOS()) {
                case LINUX:
                    return Optional.of(new LinuxNotificationService(config.getBaseDir(), settingsService));
                case MAC_OS:
                    return Optional.of(new OsxNotificationService());
                case WINDOWS:
                    return Optional.of(new AwtNotificationService());
                default:
                case ANDROID:
                    return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Could not create SystemNotificationDelegate for {}", OS.getOsName());
            return Optional.empty();
        }
    }

    private boolean logError(Throwable throwable) {
        log.error("Exception at shutdown", throwable);
        return false;
    }
}
