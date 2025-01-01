package bisq.http_api.rest_api;

import bisq.http_api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.http_api.rest_api.domain.offer.OfferRestApi;
import bisq.http_api.rest_api.domain.trade.TradeRestApi;
import bisq.http_api.rest_api.domain.user_identity.UserIdentityRestApi;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Getter
@Slf4j
public class RestApiResourceConfig extends BaseRestApiResourceConfig {
    public RestApiResourceConfig(String swaggerBaseUrl,
                                 OfferRestApi offerRestApi,
                                 TradeRestApi tradeRestApi,
                                 UserIdentityRestApi userIdentityRestApi ,
                                 MarketPriceRestApi marketPriceRestApi) {
        super(swaggerBaseUrl);

        //todo apply filtering with whiteListEndPoints/whiteListEndPoints

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(OfferRestApi.class);
        register(TradeRestApi.class);
        register(UserIdentityRestApi.class);
        register(MarketPriceRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(offerRestApi).to(OfferRestApi.class);
                bind(tradeRestApi).to(TradeRestApi.class);
                bind(userIdentityRestApi).to(UserIdentityRestApi.class);
                bind(marketPriceRestApi).to(MarketPriceRestApi.class);
            }
        });
    }
}
