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

package bisq.api;

import bisq.account.AccountService;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.http.PairingRequestHandler;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.access.session.SessionService;
import bisq.api.access.transport.ApiAccessTransportService;
import bisq.api.rest_api.RestApiResourceConfig;
import bisq.api.rest_api.domain.chat.trade.TradeChatMessagesRestApi;
import bisq.api.rest_api.domain.explorer.ExplorerRestApi;
import bisq.api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.api.rest_api.domain.offers.OfferbookRestApi;
import bisq.api.rest_api.domain.payment_accounts.PaymentAccountsRestApi;
import bisq.api.rest_api.domain.reputation.ReputationRestApi;
import bisq.api.rest_api.domain.settings.SettingsRestApi;
import bisq.api.rest_api.domain.trades.TradeRestApi;
import bisq.api.rest_api.domain.user_identity.UserIdentityRestApi;
import bisq.api.rest_api.domain.user_profile.UserProfileRestApi;
import bisq.api.web_socket.WebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.ResourceConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Swagger docs at: http://localhost:8090/doc/v1/index.html if rest is enabled
 */
@Slf4j
public class ApiService implements Service {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    @Getter
    private final ApiConfig apiConfig;
    @Getter
    private final Optional<WebSocketService> webSocketService;
    @Getter
    private final ApiAccessTransportService apiAccessTransportService;
    @Getter
    private final PairingService pairingService;
    @Getter
    private final PermissionService<RestPermissionMapping> permissionService;
    @Getter
    private final SessionService sessionService;
    @Getter
    private final HttpServerBootstrapService httpServerBootstrapService;

    private final Observable<State> state = new Observable<>(State.NEW);

    public ApiService(ApiConfig apiConfig,
                      Path appDataDirPath,
                      SecurityService securityService,
                      NetworkService networkService,
                      UserService userService,
                      BondedRolesService bondedRolesService,
                      ChatService chatService,
                      SupportService supportedService,
                      TradeService tradeService,
                      SettingsService settingsService,
                      BisqEasyService bisqEasyService,
                      OpenTradeItemsService openTradeItemsService,
                      AccountService accountService,
                      ReputationService reputationService) {
        this.apiConfig = apiConfig;

        int bindPort = apiConfig.getBindPort();

        apiAccessTransportService = new ApiAccessTransportService(apiConfig,
                appDataDirPath,
                networkService,
                securityService.getKeyBundleService(),
                bindPort,
                apiConfig.getOnionServicePort());

        permissionService = new PermissionService<>(new RestPermissionMapping());
        pairingService = new PairingService(permissionService);
        sessionService = new SessionService();

        PairingRequestHandler pairingRequestHandler = new PairingRequestHandler(pairingService, sessionService);
        SessionAuthenticationService sessionAuthenticationService = new SessionAuthenticationService(pairingService, sessionService);

        OfferbookRestApi offerbookRestApi = new OfferbookRestApi(chatService,
                bondedRolesService.getMarketPriceService(),
                userService);
        TradeRestApi tradeRestApi = new TradeRestApi(chatService,
                bondedRolesService.getMarketPriceService(),
                userService,
                supportedService,
                tradeService);
        TradeChatMessagesRestApi tradeChatMessagesRestApi = new TradeChatMessagesRestApi(chatService, userService);
        UserIdentityRestApi userIdentityRestApi = new UserIdentityRestApi(securityService, userService.getUserIdentityService(), bisqEasyService);
        MarketPriceRestApi marketPriceRestApi = new MarketPriceRestApi(bondedRolesService.getMarketPriceService());
        SettingsRestApi settingsRestApi = new SettingsRestApi(settingsService);
        PaymentAccountsRestApi paymentAccountsRestApi = new PaymentAccountsRestApi(accountService);
        UserProfileRestApi userProfileRestApi = new UserProfileRestApi(
                userService.getUserProfileService(),
                supportedService.getModerationRequestService(),
                userService.getRepublishUserProfileService());
        ExplorerRestApi explorerRestApi = new ExplorerRestApi(bondedRolesService.getExplorerService());
        ReputationRestApi reputationRestApi = new ReputationRestApi(reputationService, userService);

        Optional<ResourceConfig> restApiResourceConfig;
        if (apiConfig.isRestEnabled()) {
            restApiResourceConfig = Optional.of(new RestApiResourceConfig(apiConfig,
                    permissionService,
                    sessionAuthenticationService,
                    offerbookRestApi,
                    tradeRestApi,
                    tradeChatMessagesRestApi,
                    userIdentityRestApi,
                    marketPriceRestApi,
                    settingsRestApi,
                    explorerRestApi,
                    paymentAccountsRestApi,
                    reputationRestApi,
                    userProfileRestApi));
        } else {
            restApiResourceConfig = Optional.empty();
        }

        if (apiConfig.isWebsocketEnabled()) {
            webSocketService = Optional.of(new WebSocketService(apiConfig,
                    bondedRolesService,
                    chatService,
                    tradeService,
                    userService,
                    bisqEasyService,
                    openTradeItemsService));
        } else {
            webSocketService = Optional.empty();
        }

        httpServerBootstrapService = new HttpServerBootstrapService(apiConfig,
                apiAccessTransportService,
                restApiResourceConfig,
                webSocketService,
                pairingRequestHandler,
                sessionAuthenticationService,
                permissionService);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        setState(State.STARTING);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        // REST API and Websocket are handled inside httpServerBootstrapService
        futures.add(httpServerBootstrapService.initialize());

        futures.add(apiAccessTransportService.initialize());

        return CompletableFutureUtils.allOf(futures)
                .thenApply(list -> {
                    boolean allSucceeded = list.stream().allMatch(e -> e);
                    if (allSucceeded) {
                        setState(State.RUNNING);
                    } else {
                        log.warn("Api service initialisation did not succeed. We call shutdown.");
                    }
                    return allSucceeded;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to initialize ApiService. We call shutdown.", throwable);
                    return null; // Signal failure, handled below
                })
                .thenCompose(result -> {
                    if (result == null || !result) {
                        return shutdown().thenApply(r -> false);
                    }
                    return CompletableFuture.completedFuture(true);
                });
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        setState(State.STOPPING);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        futures.add(apiAccessTransportService.shutdown());
        futures.add(httpServerBootstrapService.shutdown());
        return CompletableFutureUtils.allOf(futures)
                .thenApply(list -> {
                    setState(State.TERMINATED);
                    return list.stream().allMatch(e -> e);
                });
    }

    public ReadOnlyObservable<State> getState() {
        return state;
    }

    private void setState(State value) {
        log.info("New state: {}", value);
        state.set(value);
    }
}
