package bisq.http_api.web_socket;

import bisq.http_api.rest_api.RestApiResourceConfig;
import bisq.http_api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.http_api.rest_api.domain.offer.OfferRestApi;
import bisq.http_api.rest_api.domain.trade.TradeRestApi;
import bisq.http_api.rest_api.domain.user_identity.UserIdentityRestApi;
import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationPath("/api/v1")
public class WebSocketRestApiResourceConfig extends RestApiResourceConfig {
    public WebSocketRestApiResourceConfig(String swaggerBaseUrl,
                                          OfferRestApi offerRestApi,
                                          TradeRestApi tradeRestApi,
                                          UserIdentityRestApi userIdentityRestApi,
                                          MarketPriceRestApi marketPriceRestApi) {
        super(swaggerBaseUrl, offerRestApi, tradeRestApi, userIdentityRestApi, marketPriceRestApi);
    }
}
