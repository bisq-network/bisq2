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

package bisq.http_api_node;

import bisq.account.AccountService;
import bisq.application.State;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.platform.OS;
import bisq.common.util.CompletableFutureUtils;
import bisq.contract.ContractService;
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
import bisq.security.keys.KeyBundleService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.wallets.core.BitcoinWalletSelection;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.SystemTray;
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
@Getter
@Slf4j
public class HttpApiApplicationService extends JavaSeApplicationService {
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
    private final BisqEasyService bisqEasyService;
    private final HttpApiService httpApiService;
    private final OpenTradeItemsService openTradeItemsService;

    public HttpApiApplicationService(String[] args) {
        super("http_api_app", args);

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
                persistenceService,
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
                persistenceService, networkService, chatService, userService, bondedRolesService);

        tradeService = new TradeService(networkService, identityService, persistenceService, offerService,
                contractService, supportService, chatService, bondedRolesService, userService, settingsService);

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
                openTradeItemsService,
                accountService);
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
                .thenCompose(result -> systemNotificationService.initialize())
                .thenCompose(result -> offerService.initialize())
                .thenCompose(result -> chatService.initialize())
                .thenCompose(result -> supportService.initialize())
                .thenCompose(result -> tradeService.initialize())
                .thenCompose(result -> bisqEasyService.initialize())
                .thenCompose(result -> openTradeItemsService.initialize())
                .thenCompose(result -> httpApiService.initialize())
                .orTimeout(5, TimeUnit.MINUTES)
                .whenComplete((success, throwable) -> {
                    if (throwable == null) {
                        if (success) {
                            setState(State.APP_INITIALIZED);

                            bondedRolesService.getDifficultyAdjustmentService().getMostRecentValueOrDefault().addObserver(mostRecentValueOrDefault -> networkService.getNetworkLoadServices().forEach(networkLoadService ->
                                    networkLoadService.setDifficultyAdjustmentFactor(mostRecentValueOrDefault)));

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
        return supplyAsync(() -> httpApiService.shutdown()
                .thenCompose(result -> openTradeItemsService.shutdown())
                .thenCompose(result -> bisqEasyService.shutdown())
                .thenCompose(result -> tradeService.shutdown())
                .thenCompose(result -> supportService.shutdown())
                .thenCompose(result -> chatService.shutdown())
                .thenCompose(result -> offerService.shutdown())
                .thenCompose(result -> systemNotificationService.shutdown())
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
                .thenCompose(result -> memoryReportService.shutdown())
                .orTimeout(10, TimeUnit.SECONDS)
                .handle((result, throwable) -> throwable == null)
                .join());
    }

    public KeyBundleService getKeyBundleService() {
        return securityService.getKeyBundleService();
    }

    private Optional<OsSpecificNotificationService> findSystemNotificationDelegate() {
        try {
            switch (OS.getOS()) {
                case LINUX:
                    return Optional.of(new LinuxNotificationService(config.getBaseDir(), settingsService));
                case MAC_OS:
                    return Optional.of(new OsxNotificationService());
                case WINDOWS:
                    return SystemTray.isSupported() ? Optional.of(new AwtNotificationService()) : Optional.empty();
                case ANDROID:
                default:
                    return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Could not create SystemNotificationDelegate for {}", OS.getOsName());
            return Optional.empty();
        }
    }
}
