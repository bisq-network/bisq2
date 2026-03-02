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
import bisq.api.rest_api.endpoints.payment_accounts.FiatPaymentAccountsRestApi;
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
import bisq.common.observable.Pin;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.notifications.mobile.registration.DeviceRegistrationService;
import bisq.persistence.PersistenceService;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

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
    private final Object pairingQrCodeLock = new Object();
    @Nullable
    private Pin pairingCodePin;
    private Optional<Scheduler> pairingCodeScheduler = Optional.empty();

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
        FiatPaymentAccountsRestApi fiatPaymentAccountsRestApi = new FiatPaymentAccountsRestApi(accountService);
        UserProfileRestApi userProfileRestApi = new UserProfileRestApi(
                userService.getUserProfileService(),
                supportedService.getModerationRequestService(),
                userService.getRepublishUserProfileService());
        ExplorerRestApi explorerRestApi = new ExplorerRestApi(bondedRolesService.getExplorerService());
        ReputationRestApi reputationRestApi = new ReputationRestApi(reputationService, userService);
        DevicesRestApi devicesRestApi= new DevicesRestApi(deviceRegistrationService);

        ResourceConfig resourceConfig;
        if (apiConfig.isRestEnabled() || apiConfig.isWebsocketEnabled()) {
            // WebSocket REST bridge forwards requests to the local HTTP server,
            // so all endpoints must be registered when WebSocket is enabled.
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
                    fiatPaymentAccountsRestApi,
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
                            setupPairingCodeAutoRegeneration();
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
        pairingCodeScheduler.ifPresent(Scheduler::stop);
        pairingCodeScheduler = Optional.empty();
        if (pairingCodePin != null) {
            pairingCodePin.unbind();
            pairingCodePin = null;
        }
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        futures.add(apiAccessTransportService.shutdown());
        futures.add(httpServerBootstrapService.shutdown());
        return CompletableFutureUtils.allOf(futures)
                .thenApply(list -> {
                    setState(State.TERMINATED);
                    return list.stream().allMatch(e -> e);
                });
    }

    public void createPairingQrCode() {
        synchronized (pairingQrCodeLock) {
            if (webSocketService.isEmpty()) {
                log.warn("Cannot create pairing QR code: WebSocket service not initialized");
                return;
            }

            WebSocketService.State wsState = webSocketService.get().getState().get();
            if (wsState != WebSocketService.State.RUNNING) {
                log.warn("Cannot create pairing QR code: WebSocket service state is {} (expected RUNNING)", wsState);
                return;
            }

            pairingService.cleanupExpiredPairingCodes();

            String webSocketUrl;
            if (apiConfig.useTor()) {
                Address onionAddress = apiAccessTransportService.getOnionAddress().get();
                if (onionAddress == null) {
                    log.warn("Cannot create pairing QR code: onion address not available yet");
                    return;
                }
                webSocketUrl = apiConfig.getWebSocketProtocol() + "://" + onionAddress.getHost() + ":" + onionAddress.getPort();
            } else {
                String host = apiConfig.getBindHost();
                if ("0.0.0.0".equals(host)) {
                    // 0.0.0.0 is not a reachable address — attempt auto-detection for the QR code.
                    Address lanAddress = apiAccessTransportService.findLanAddress();
                    webSocketUrl = apiConfig.getWebSocketProtocol() + "://" + lanAddress.getHost() + ":" + lanAddress.getPort();
                    log.warn("CLEAR mode: bind host is 0.0.0.0, using auto-detected LAN address {} for pairing QR code. " +
                            "For reliable pairing, set bind.host to a specific IP.", lanAddress);
                } else {
                    // Use the configured bind host directly — whether it's a specific LAN IP
                    // or loopback (loopback only works for emulators, warned at startup).
                    webSocketUrl = apiConfig.getWebSocketServerUrl();
                }
            }

            Set<Permission> grantedPermissions = apiConfig.getGrantedPermissions();
            PairingCode pairingCode = pairingService.createPairingCode(grantedPermissions);
            pairingService.createPairingQrCode(pairingCode,
                    webSocketUrl,
                    tlsContextService.getTlsContext(),
                    apiAccessTransportService.getTorContext());

            if (pairingService.getPairingQrCode().get() != null) {
                log.info("Pairing QR code created. Code ID: {} (expires at: {})",
                        pairingCode.getId(), pairingCode.getExpiresAt());
            } else {
                log.warn("Failed to create pairing QR code for Code ID: {}", pairingCode.getId());
            }
        }
    }

    private void setupPairingCodeAutoRegeneration() {
        // Schedule periodic regeneration before TTL expires.
        // Use at most 5 minutes interval so headless users get fresh codes promptly.
        int ttlMinutes = pairingService.getPairingCodeTtlInSeconds() / 60;
        int regenerationIntervalMinutes = Math.max(1, Math.min(ttlMinutes - 1, 5));
        pairingCodeScheduler = Optional.of(Scheduler.run(this::createPairingQrCode)
                .host(this)
                .runnableName("regeneratePairingCode")
                .periodically(regenerationIntervalMinutes, TimeUnit.MINUTES));
        log.info("Pairing code auto-regeneration scheduled every {} minutes (TTL: {} minutes)",
                regenerationIntervalMinutes, ttlMinutes);

        // Regenerate immediately when a pairing code is consumed
        pairingCodePin = pairingService.getPairingCode().addObserver(pairingCode -> {
            if (pairingCode == null && state.get() == State.RUNNING) {
                log.info("Pairing code was consumed, regenerating immediately");
                createPairingQrCode();
            } else if (pairingCode == null) {
                log.warn("Pairing code was consumed but ApiService state is {} — skipping regeneration", state.get());
            }
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
