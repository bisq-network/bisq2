package bisq.api.web_socket;

import bisq.api.ApiConfig;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
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
import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationPath("/api/v1")
public class WebSocketRestApiResourceConfig extends RestApiResourceConfig {
    public WebSocketRestApiResourceConfig(ApiConfig apiConfig,
                                          PermissionService<RestPermissionMapping> permissionService,
                                          SessionAuthenticationService sessionAuthenticationService,
                                          OfferbookRestApi offerbookRestApi,
                                          TradeRestApi tradeRestApi,
                                          TradeChatMessagesRestApi tradeChatMessagesRestApi,
                                          UserIdentityRestApi userIdentityRestApi,
                                          MarketPriceRestApi marketPriceRestApi,
                                          SettingsRestApi settingsRestApi,
                                          ExplorerRestApi explorerRestApi,
                                          PaymentAccountsRestApi paymentAccountsRestApi,
                                          ReputationRestApi reputationRestApi,
                                          UserProfileRestApi userProfileRestApi) {
        super(apiConfig,
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
                userProfileRestApi);
    }
}
