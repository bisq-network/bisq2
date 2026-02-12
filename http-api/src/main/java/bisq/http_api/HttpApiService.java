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

package bisq.http_api;

import bisq.account.AccountService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.util.CompletableFutureUtils;
import bisq.http_api.access.ApiAccessService;
import bisq.http_api.access.pairing.PairingCode;
import bisq.http_api.access.pairing.PairingService;
import bisq.http_api.access.permissions.Permission;
import bisq.http_api.access.permissions.PermissionService;
import bisq.http_api.access.permissions.RestPermissionMapping;
import bisq.http_api.access.persistence.ApiAccessStoreService;
import bisq.http_api.access.session.SessionService;
import bisq.http_api.access.transport.TlsContext;
import bisq.http_api.access.transport.TlsContextService;
import bisq.http_api.access.transport.TorContext;
import bisq.security.tls.TlsException;
import bisq.common.timer.Scheduler;
import bisq.http_api.rest_api.RestApiResourceConfig;
import bisq.http_api.rest_api.RestApiService;
import bisq.http_api.push_notification.DeviceRegistrationService;
import bisq.http_api.push_notification.PushNotificationConfig;
import bisq.http_api.push_notification.PushNotificationService;
import bisq.http_api.rest_api.domain.chat.trade.TradeChatMessagesRestApi;
import bisq.http_api.rest_api.domain.devices.DevicesRestApi;
import bisq.http_api.rest_api.domain.explorer.ExplorerRestApi;
import bisq.http_api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.http_api.rest_api.domain.offers.OfferbookRestApi;
import bisq.http_api.rest_api.domain.payment_accounts.FiatPaymentAccountsRestApi;
import bisq.http_api.rest_api.domain.reputation.ReputationRestApi;
import bisq.http_api.rest_api.domain.settings.SettingsRestApi;
import bisq.http_api.rest_api.domain.trades.TradeRestApi;
import bisq.http_api.rest_api.domain.user_identity.UserIdentityRestApi;
import bisq.http_api.rest_api.domain.user_profile.UserProfileRestApi;
import bisq.http_api.rest_api.endpoints.access.AccessApi;
import bisq.http_api.web_socket.WebSocketRestApiResourceConfig;
import bisq.http_api.web_socket.WebSocketService;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8090/doc/v1/index.html or http://localhost:8082/doc/v1/index.html in case RestAPI
 * is used without websockets
 */
@Slf4j
public class HttpApiService implements Service {
    private final Optional<RestApiService> restApiService;
    @Getter
    private final Optional<WebSocketService> webSocketService;
    private final Optional<DeviceRegistrationService> deviceRegistrationService;
    private final Optional<PushNotificationService> pushNotificationService;

    // Pairing infrastructure
    private final PairingConfig pairingConfig;
    private final Path appDataDirPath;
    private final Optional<PairingService> pairingService;
    private final Optional<SessionService> sessionService;
    private final Optional<ApiAccessService> apiAccessService;
    private final Optional<TlsContextService> tlsContextService;
    private Optional<Scheduler> pairingCodeScheduler = Optional.empty();

    public HttpApiService(RestApiService.Config restApiConfig,
                          WebSocketService.Config webSocketConfig,
                          PushNotificationConfig pushNotificationConfig,
                          PairingConfig pairingConfig,
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
                          ReputationService reputationService) {
        this.pairingConfig = pairingConfig;
        this.appDataDirPath = appDataDirPath;

        // Initialize pairing infrastructure if enabled
        if (pairingConfig.isEnabled()) {
            ApiAccessStoreService apiAccessStoreService = new ApiAccessStoreService(persistenceService);
            PermissionService<RestPermissionMapping> permissionService = new PermissionService<>(
                    apiAccessStoreService,
                    new RestPermissionMapping()
            );
            pairingService = Optional.of(new PairingService(
                    appDataDirPath,
                    pairingConfig.isWritePairingQrCodeToDisk(),
                    apiAccessStoreService,
                    permissionService
            ));
            sessionService = Optional.of(new SessionService(pairingConfig.getSessionTtlInMinutes()));
            apiAccessService = Optional.of(new ApiAccessService(pairingService.get(), sessionService.get()));
            log.info("Pairing service enabled (writeToDisk: {})", pairingConfig.isWritePairingQrCodeToDisk());
        } else {
            pairingService = Optional.empty();
            sessionService = Optional.empty();
            apiAccessService = Optional.empty();
            log.info("Pairing service disabled");
        }

        // Initialize TLS context service if TLS is required by either config
        if (webSocketConfig.isTlsRequired() || restApiConfig.isTlsRequired()) {
            // Use websocket config's TLS settings as primary (most common use case)
            var tlsConfig = webSocketConfig.isTlsRequired() ? webSocketConfig : restApiConfig;
            tlsContextService = Optional.of(new TlsContextService(
                    true,
                    tlsConfig.getTlsKeyStorePassword(),
                    tlsConfig.getTlsKeyStoreSan(),
                    appDataDirPath));
        } else {
            tlsContextService = Optional.empty();
        }

        // Get TLS context if available
        Optional<TlsContext> tlsContext = Optional.empty();
        if (tlsContextService.isPresent()) {
            try {
                tlsContext = tlsContextService.get().getOrCreateTlsContext();
            } catch (TlsException e) {
                log.error("Failed to create TLS context", e);
            }
        }

        boolean restApiConfigEnabled = restApiConfig.isEnabled();
        boolean webSocketConfigEnabled = webSocketConfig.isEnabled();
        if (restApiConfigEnabled || webSocketConfigEnabled) {
            // Initialize device registration service for push notifications
            deviceRegistrationService = Optional.of(new DeviceRegistrationService(appDataDirPath));
            // Initialize push notification service if enabled
            if (pushNotificationConfig.isEnabled()) {
                pushNotificationService = Optional.of(new PushNotificationService(
                        deviceRegistrationService.get(),
                        pushNotificationConfig.getBisqRelayUrl(),
                        Optional.of(networkService),
                        appDataDirPath));
                log.info("Push notification service enabled with Bisq Relay URL: {}", pushNotificationConfig.getBisqRelayUrl());
            } else {
                pushNotificationService = Optional.empty();
                log.info("Push notification service disabled");
            }
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
            FiatPaymentAccountsRestApi fiatPaymentAccountsRestApi = new FiatPaymentAccountsRestApi(accountService);
            UserProfileRestApi userProfileRestApi = new UserProfileRestApi(
                    userService.getUserProfileService(),
                    supportedService.getModerationRequestService(),
                    userService.getRepublishUserProfileService());
            ExplorerRestApi explorerRestApi = new ExplorerRestApi(bondedRolesService.getExplorerService());
            ReputationRestApi reputationRestApi = new ReputationRestApi(reputationService, userService);
            DevicesRestApi devicesRestApi = new DevicesRestApi(deviceRegistrationService.get());

            // Create AccessApi if pairing is enabled
            AccessApi accessApi = apiAccessService.map(AccessApi::new).orElse(null);

            if (restApiConfigEnabled) {
                var restApiResourceConfig = new RestApiResourceConfig(restApiConfig,
                        offerbookRestApi,
                        tradeRestApi,
                        tradeChatMessagesRestApi,
                        userIdentityRestApi,
                        marketPriceRestApi,
                        settingsRestApi,
                        explorerRestApi,
                        fiatPaymentAccountsRestApi,
                        reputationRestApi,
                        userProfileRestApi,
                        devicesRestApi,
                        Optional.ofNullable(accessApi));
                restApiService = Optional.of(new RestApiService(restApiConfig, restApiResourceConfig, appDataDirPath, securityService, networkService));
            } else {
                restApiService = Optional.empty();
            }

            if (webSocketConfigEnabled) {
                var webSocketResourceConfig = new WebSocketRestApiResourceConfig(webSocketConfig,
                        offerbookRestApi,
                        tradeRestApi,
                        tradeChatMessagesRestApi,
                        userIdentityRestApi,
                        marketPriceRestApi,
                        settingsRestApi,
                        explorerRestApi,
                        fiatPaymentAccountsRestApi,
                        reputationRestApi,
                        userProfileRestApi,
                        devicesRestApi,
                        Optional.ofNullable(accessApi));
                webSocketService = Optional.of(new WebSocketService(webSocketConfig,
                        webSocketResourceConfig,
                        appDataDirPath,
                        securityService,
                        networkService,
                        bondedRolesService,
                        chatService,
                        tradeService,
                        userService,
                        bisqEasyService,
                        openTradeItemsService,
                        pushNotificationService,
                        tlsContext));
            } else {
                webSocketService = Optional.empty();
            }

        } else {
            restApiService = Optional.empty();
            webSocketService = Optional.empty();
            deviceRegistrationService = Optional.empty();
            pushNotificationService = Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFutureUtils.allOf(
                        deviceRegistrationService.map(DeviceRegistrationService::initialize)
                                .orElse(CompletableFuture.completedFuture(true)),
                        pushNotificationService.map(PushNotificationService::initialize)
                                .orElse(CompletableFuture.completedFuture(true)),
                        restApiService.map(RestApiService::initialize)
                                .orElse(CompletableFuture.completedFuture(true)),
                        webSocketService.map(WebSocketService::initialize)
                                .orElse(CompletableFuture.completedFuture(true)))
                .thenApply(list -> list.stream().allMatch(e -> e))
                .thenApply(result -> {
                    // Generate pairing QR code after WebSocket service is initialized
                    if (result && pairingConfig.isEnabled() && webSocketService.isPresent()) {
                        try {
                            createPairingQrCode();
                            // Schedule periodic regeneration every 4 minutes (before 5-minute TTL expires)
                            pairingCodeScheduler = Optional.of(Scheduler.run(this::createPairingQrCode)
                                    .host(this)
                                    .runnableName("regeneratePairingCode")
                                    .periodically(4, TimeUnit.MINUTES));
                            log.info("Pairing code will be regenerated every 4 minutes");
                        } catch (Exception e) {
                            log.error("Failed to create pairing QR code", e);
                        }
                    }
                    return result;
                });
    }

    private void createPairingQrCode() {
        if (pairingService.isEmpty()) {
            log.warn("Cannot create pairing QR code: pairing service not initialized");
            return;
        }

        // Get WebSocket URL
        String webSocketUrl = getWebSocketUrl();
        if (webSocketUrl == null) {
            log.warn("Cannot create pairing QR code: WebSocket URL not available");
            return;
        }

        // Create pairing code with all permissions
        Set<Permission> allPermissions = Arrays.stream(Permission.values()).collect(Collectors.toSet());
        PairingCode pairingCode = pairingService.get().createPairingCode(allPermissions);

        // Get TLS and Tor contexts
        Optional<TlsContext> pairingTlsContext = tlsContextService
                .flatMap(TlsContextService::getTlsContext);
        Optional<TorContext> torContext = getTorContext();

        // Generate and write QR code
        pairingService.get().createPairingQrCode(pairingCode, webSocketUrl, pairingTlsContext, torContext);

        log.info("Pairing QR code created for WebSocket URL: {} with pairing code ID: {} (expires at: {})",
                webSocketUrl, pairingCode.getId(), pairingCode.getExpiresAt());
    }

    private String getWebSocketUrl() {
        if (webSocketService.isEmpty()) {
            return null;
        }

        WebSocketService wsService = webSocketService.get();
        WebSocketService.Config wsConfig = wsService.getConfig();

        // Check if Tor onion service is published
        if (wsConfig.isPublishOnionService() && wsService.getApiTorOnionService().getPublishedAddress().isPresent()) {
            Address onionAddress = wsService.getApiTorOnionService().getPublishedAddress().get();
            return wsConfig.getProtocol() + onionAddress.getHost() + ":" + onionAddress.getPort();
        }

        // Fall back to clearnet address
        return wsConfig.getProtocol() + wsConfig.getHost() + ":" + wsConfig.getPort();
    }

    private Optional<TorContext> getTorContext() {
        if (webSocketService.isEmpty()) {
            return Optional.empty();
        }

        WebSocketService wsService = webSocketService.get();
        if (!wsService.getConfig().isPublishOnionService()) {
            return Optional.empty();
        }

        Optional<Address> publishedAddress = wsService.getApiTorOnionService().getPublishedAddress();
        if (publishedAddress.isEmpty()) {
            return Optional.empty();
        }

        // For now, we don't have client auth secret in this branch
        // The TorContext will be created with just the onion address
        String onionAddress = publishedAddress.get().getHost();
        return Optional.of(new TorContext(onionAddress, null));
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        pairingCodeScheduler.ifPresent(Scheduler::stop);
        return CompletableFutureUtils.allOf(
                        deviceRegistrationService.map(DeviceRegistrationService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)),
                        pushNotificationService.map(PushNotificationService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)),
                        restApiService.map(RestApiService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)),
                        webSocketService.map(WebSocketService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)))
                .thenApply(list -> list.stream().allMatch(e -> e));
    }
}
