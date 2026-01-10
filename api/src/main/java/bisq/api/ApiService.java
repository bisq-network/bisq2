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
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.util.CompletableFutureUtils;
import bisq.api.rest_api.RestApiResourceConfig;
import bisq.api.rest_api.RestApiService;
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
import bisq.api.web_socket.WebSocketRestApiResourceConfig;
import bisq.api.web_socket.WebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8090/doc/v1/index.html or http://localhost:8082/doc/v1/index.html in case RestAPI
 * is used without websockets
 */
@Slf4j
public class ApiService implements Service {
    private final Optional<RestApiService> restApiService;
    @Getter
    private final Optional<WebSocketService> webSocketService;
    private final ApiConfig apiConfig;

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

        if (apiConfig.isEnabled()) {
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

            if (apiConfig.isRestEnabled()) {
                var restApiResourceConfig = new RestApiResourceConfig(apiConfig,
                        offerbookRestApi,
                        tradeRestApi,
                        tradeChatMessagesRestApi,
                        userIdentityRestApi,
                        marketPriceRestApi,
                        settingsRestApi,
                        explorerRestApi,
                        paymentAccountsRestApi,
                        reputationRestApi,
                        userProfileRestApi);
                restApiService = Optional.of(new RestApiService(apiConfig, restApiResourceConfig, appDataDirPath, securityService, networkService));
            } else {
                restApiService = Optional.empty();
            }

            if (apiConfig.isWebsocketEnabled()) {
                var webSocketResourceConfig = new WebSocketRestApiResourceConfig(apiConfig,
                        offerbookRestApi,
                        tradeRestApi,
                        tradeChatMessagesRestApi,
                        userIdentityRestApi,
                        marketPriceRestApi,
                        settingsRestApi,
                        explorerRestApi,
                        paymentAccountsRestApi,
                        reputationRestApi,
                        userProfileRestApi);
                webSocketService = Optional.of(new WebSocketService(apiConfig,
                        webSocketResourceConfig,
                        appDataDirPath,
                        securityService,
                        networkService,
                        bondedRolesService,
                        chatService,
                        tradeService,
                        userService,
                        bisqEasyService,
                        openTradeItemsService));
            } else {
                webSocketService = Optional.empty();
            }

        } else {
            restApiService = Optional.empty();
            webSocketService = Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }
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
        if (!apiConfig.isEnabled()) {
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFutureUtils.allOf(
                        restApiService.map(RestApiService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)),
                        webSocketService.map(WebSocketService::shutdown)
                                .orElse(CompletableFuture.completedFuture(true)))
                .thenApply(list -> list.stream().allMatch(e -> e));
    }
}
