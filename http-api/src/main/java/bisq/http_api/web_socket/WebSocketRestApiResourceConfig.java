package bisq.http_api.web_socket;

import bisq.http_api.rest_api.RestApiResourceConfig;
import bisq.http_api.rest_api.domain.explorer.ExplorerRestApi;
import bisq.http_api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.http_api.rest_api.domain.offers.OfferbookRestApi;
import bisq.http_api.rest_api.domain.payment_accounts.PaymentAccountsRestApi;
import bisq.http_api.rest_api.domain.settings.SettingsRestApi;
import bisq.http_api.rest_api.domain.trades.TradeRestApi;
import bisq.http_api.rest_api.domain.user_identity.UserIdentityRestApi;
import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationPath("/api/v1")
public class WebSocketRestApiResourceConfig extends RestApiResourceConfig {
    public WebSocketRestApiResourceConfig(String swaggerBaseUrl,
                                          OfferbookRestApi offerbookRestApi,
                                          TradeRestApi tradeRestApi,
                                          UserIdentityRestApi userIdentityRestApi,
                                          MarketPriceRestApi marketPriceRestApi,
                                          SettingsRestApi settingsRestApi,
                                          ExplorerRestApi explorerRestApi,
                                          PaymentAccountsRestApi paymentAccountsRestApi) {
        super(swaggerBaseUrl, offerbookRestApi, tradeRestApi, userIdentityRestApi, marketPriceRestApi, settingsRestApi, explorerRestApi, paymentAccountsRestApi);
    }
}
