package bisq.http_api.web_socket;

import bisq.http_api.config.CommonApiConfig;
import bisq.http_api.rest_api.RestApiResourceConfig;
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
import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ApplicationPath("/api/v1")
public class WebSocketRestApiResourceConfig extends RestApiResourceConfig {
    public WebSocketRestApiResourceConfig(CommonApiConfig config,
                                          OfferbookRestApi offerbookRestApi,
                                          TradeRestApi tradeRestApi,
                                          TradeChatMessagesRestApi tradeChatMessagesRestApi,
                                          UserIdentityRestApi userIdentityRestApi,
                                          MarketPriceRestApi marketPriceRestApi,
                                          SettingsRestApi settingsRestApi,
                                          ExplorerRestApi explorerRestApi,
                                          FiatPaymentAccountsRestApi fiatPaymentAccountsRestApi,
                                          ReputationRestApi reputationRestApi,
                                          UserProfileRestApi userProfileRestApi,
                                          DevicesRestApi devicesRestApi,
                                          Optional<AccessApi> accessApi) {
        super(config, offerbookRestApi, tradeRestApi, tradeChatMessagesRestApi, userIdentityRestApi, marketPriceRestApi, settingsRestApi, explorerRestApi, fiatPaymentAccountsRestApi, reputationRestApi, userProfileRestApi, devicesRestApi, accessApi);
    }
}
