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
import bisq.api.access.ApiAccessService;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.pairing.PairingCode;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.access.persistence.ApiAccessStoreService;
import bisq.api.access.session.SessionService;
import bisq.api.access.transport.ApiAccessTransportService;
import bisq.api.access.transport.TlsContextService;
import bisq.api.rest_api.PairingApiResourceConfig;
import bisq.api.rest_api.RestApiResourceConfig;
import bisq.api.rest_api.endpoints.access.AccessApi;
import bisq.api.rest_api.endpoints.chat.trade.TradeChatMessagesRestApi;
import bisq.api.rest_api.endpoints.devices.DevicesRestApi;
import bisq.api.rest_api.endpoints.explorer.ExplorerRestApi;
import bisq.api.rest_api.endpoints.market_price.MarketPriceRestApi;
import bisq.api.rest_api.endpoints.offers.OfferbookRestApi;
import bisq.api.rest_api.endpoints.payment_accounts.PaymentAccountsRestApi;
import bisq.api.rest_api.endpoints.reputation.ReputationRestApi;
import bisq.api.rest_api.endpoints.settings.SettingsRestApi;
import bisq.api.rest_api.endpoints.trades.TradeRestApi;
import bisq.api.rest_api.endpoints.user_identity.UserIdentityRestApi;
import bisq.api.rest_api.endpoints.user_profile.UserProfileRestApi;
import bisq.api.web_socket.WebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.notifications.mobile.registration.DeviceRegistrationService;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.tls.TlsException;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
    @Getter
    private final TlsContextService tlsContextService;
    private final Observable<State> state = new Observable<>(State.NEW);

    public ApiService(ApiConfig apiConfig,
                      Path appDataDirPath,
                      PersistenceService persistenceService,
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
                      ReputationService reputationService,
                      DeviceRegistrationService deviceRegistrationService) {
        this.apiConfig = apiConfig;

        int bindPort = apiConfig.getBindPort();

        apiAccessTransportService = new ApiAccessTransportService(apiConfig,
                appDataDirPath,
                networkService,
                securityService.getKeyBundleService(),
                bindPort,
                apiConfig.getOnionServicePort());

        ApiAccessStoreService apiAccessStoreService = new ApiAccessStoreService(persistenceService);
        permissionService = new PermissionService<>(apiAccessStoreService, new RestPermissionMapping());
        pairingService = new PairingService(apiConfig, appDataDirPath, apiAccessStoreService, permissionService);
        sessionService = new SessionService(apiConfig.getSessionTtlInMinutes());
        tlsContextService = new TlsContextService(apiConfig, appDataDirPath);

        SessionAuthenticationService sessionAuthenticationService = new SessionAuthenticationService(pairingService, sessionService);

        ApiAccessService apiAccessService = new ApiAccessService(pairingService, sessionService);
        AccessApi accessApi = new AccessApi(apiAccessService);

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
        DevicesRestApi devicesRestApi= new DevicesRestApi(deviceRegistrationService);

        ResourceConfig resourceConfig;
        if (apiConfig.isRestEnabled()) {
            resourceConfig = new RestApiResourceConfig(apiConfig,
                    permissionService,
                    sessionAuthenticationService,
                    accessApi,
                    offerbookRestApi,
                    tradeRestApi,
                    tradeChatMessagesRestApi,
                    userIdentityRestApi,
                    marketPriceRestApi,
                    settingsRestApi,
                    explorerRestApi,
                    paymentAccountsRestApi,
                    reputationRestApi,
                    userProfileRestApi,
                    devicesRestApi);
        } else {
            resourceConfig = new PairingApiResourceConfig(accessApi);
        }

        if (apiConfig.isWebsocketEnabled()) {
            webSocketService = Optional.of(new WebSocketService(apiConfig,
                    tlsContextService,
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
                resourceConfig,
                webSocketService,
                sessionAuthenticationService,
                permissionService,
                tlsContextService);
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

                    if (webSocketService.isEmpty() || !apiConfig.isWebsocketEnabled()) {
                        return CompletableFuture.completedFuture(true);
                    } else {
                        try {
                            createPairingQrCode();
                            return CompletableFuture.completedFuture(true);
                        } catch (Exception e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    }
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

    public void createPairingQrCode() throws TlsException {
        checkArgument(webSocketService.isPresent(),
                "webSocketService must be present");

        checkArgument(webSocketService.get().getState().get() == WebSocketService.State.RUNNING,
                "webSocketServiceState must be RUNNING");

        String webSocketUrl;
        if (apiConfig.useTor()) {
            Address onionAddress = apiAccessTransportService.getOnionAddress().get();
            checkNotNull(onionAddress, "OnionAddress must not be null");
            webSocketUrl = apiConfig.getWebSocketProtocol() + "://" + onionAddress.getHost() + ":" + onionAddress.getPort();
        } else {
            webSocketUrl = apiConfig.getWebSocketServerUrl();
        }
        Set<Permission> grantedPermissions = apiConfig.getGrantedPermissions();
        PairingCode pairingCode = pairingService.createPairingCode(grantedPermissions);
        pairingService.createPairingQrCode(pairingCode,
                webSocketUrl,
                tlsContextService.getOrCreateTlsContext(),
                apiAccessTransportService.getTorContext());
    }

    public ReadOnlyObservable<State> getState() {
        return state;
    }

    private void setState(State value) {
        log.info("New state: {}", value);
        state.set(value);
    }
}
