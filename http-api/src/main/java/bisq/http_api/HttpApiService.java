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
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.util.CompletableFutureUtils;
import bisq.http_api.rest_api.RestApiResourceConfig;
import bisq.http_api.rest_api.RestApiService;
import bisq.http_api.rest_api.domain.explorer.ExplorerRestApi;
import bisq.http_api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.http_api.rest_api.domain.offers.OfferbookRestApi;
import bisq.http_api.rest_api.domain.payment_accounts.PaymentAccountsRestApi;
import bisq.http_api.rest_api.domain.settings.SettingsRestApi;
import bisq.http_api.rest_api.domain.trades.TradeRestApi;
import bisq.http_api.rest_api.domain.user_identity.UserIdentityRestApi;
import bisq.http_api.web_socket.WebSocketRestApiResourceConfig;
import bisq.http_api.web_socket.WebSocketService;
import bisq.http_api.web_socket.domain.OpenTradeItemsService;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8090/doc/v1/index.html or http://localhost:8082/doc/v1/index.html in case RestAPI
 * is used without websockets
 */
@Slf4j
public class HttpApiService implements Service {
    private final Optional<RestApiService> restApiService;
    private final Optional<WebSocketService> webSocketService;

    public HttpApiService(RestApiService.Config restApiConfig,
                          WebSocketService.Config webSocketConfig,
                          SecurityService securityService,
                          NetworkService networkService,
                          UserService userService,
                          BondedRolesService bondedRolesService,
                          ChatService chatService,
                          SupportService supportedService,
                          TradeService tradeService,
                          SettingsService settingsService,
                          OpenTradeItemsService openTradeItemsService,
                          AccountService accountService) {
        boolean restApiConfigEnabled = restApiConfig.isEnabled();
        boolean webSocketConfigEnabled = webSocketConfig.isEnabled();
        if (restApiConfigEnabled || webSocketConfigEnabled) {
            OfferbookRestApi offerbookRestApi = new OfferbookRestApi(chatService,
                    bondedRolesService.getMarketPriceService(),
                    userService);
            TradeRestApi tradeRestApi = new TradeRestApi(chatService,
                    bondedRolesService.getMarketPriceService(),
                    userService,
                    supportedService,
                    tradeService);
            UserIdentityRestApi userIdentityRestApi = new UserIdentityRestApi(securityService, userService.getUserIdentityService());
            MarketPriceRestApi marketPriceRestApi = new MarketPriceRestApi(bondedRolesService.getMarketPriceService());
            SettingsRestApi settingsRestApi = new SettingsRestApi(settingsService);
            PaymentAccountsRestApi paymentAccountsRestApi = new PaymentAccountsRestApi(accountService);
            ExplorerRestApi explorerRestApi = new ExplorerRestApi(bondedRolesService.getExplorerService());
            if (restApiConfigEnabled) {
                var restApiResourceConfig = new RestApiResourceConfig(restApiConfig.getRestApiBaseUrl(),
                        offerbookRestApi,
                        tradeRestApi,
                        userIdentityRestApi,
                        marketPriceRestApi,
                        settingsRestApi,
                        explorerRestApi,
                        paymentAccountsRestApi);
                this.restApiService = Optional.of(new RestApiService(restApiConfig, restApiResourceConfig));
            } else {
                this.restApiService = Optional.empty();
            }

            if (webSocketConfigEnabled) {
                var webSocketResourceConfig = new WebSocketRestApiResourceConfig(webSocketConfig.getRestApiBaseUrl(),
                        offerbookRestApi,
                        tradeRestApi,
                        userIdentityRestApi,
                        marketPriceRestApi,
                        settingsRestApi,
                        explorerRestApi,
                        paymentAccountsRestApi);
                this.webSocketService = Optional.of(new WebSocketService(webSocketConfig,
                        webSocketConfig.getRestApiBaseAddress(),
                        webSocketResourceConfig,
                        bondedRolesService,
                        chatService,
                        tradeService,
                        userService,
                        openTradeItemsService));
            } else {
                this.webSocketService = Optional.empty();
            }

        } else {
            this.restApiService = Optional.empty();
            this.webSocketService = Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFutureUtils.allOf(
                        restApiService.map(RestApiService::initialize)
                                .orElse(CompletableFuture.completedFuture(true)),
                        webSocketService.map(WebSocketService::initialize)
                                .orElse(CompletableFuture.completedFuture(true)))
                .thenApply(list -> list.stream().allMatch(e -> e));
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFutureUtils.allOf(
                        restApiService.map(RestApiService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)),
                        webSocketService.map(WebSocketService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)))
                .thenApply(list -> list.stream().allMatch(e -> e));
    }
}
