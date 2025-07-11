package bisq.http_api.rest_api;

import bisq.http_api.rest_api.domain.explorer.ExplorerRestApi;
import bisq.http_api.rest_api.domain.market_price.MarketPriceRestApi;
import bisq.http_api.rest_api.domain.offers.OfferbookRestApi;
import bisq.http_api.rest_api.domain.payment_accounts.PaymentAccountsRestApi;
import bisq.http_api.rest_api.domain.settings.SettingsRestApi;
import bisq.http_api.rest_api.domain.trades.TradeRestApi;
import bisq.http_api.rest_api.domain.user_identity.UserIdentityRestApi;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Getter
@Slf4j
public class RestApiResourceConfig extends BaseRestApiResourceConfig {
    public RestApiResourceConfig(String swaggerBaseUrl,
                                 OfferbookRestApi offerbookRestApi,
                                 TradeRestApi tradeRestApi,
                                 UserIdentityRestApi userIdentityRestApi ,
                                 MarketPriceRestApi marketPriceRestApi,
                                 SettingsRestApi settingsRestApi,
                                 ExplorerRestApi explorerRestApi,
                                 PaymentAccountsRestApi paymentAccountsRestApi) {
        super(swaggerBaseUrl);

        //todo apply filtering with whiteListEndPoints/whiteListEndPoints

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(OfferbookRestApi.class);
        register(TradeRestApi.class);
        register(UserIdentityRestApi.class);
        register(MarketPriceRestApi.class);
        register(SettingsRestApi.class);
        register(ExplorerRestApi.class);
        register(PaymentAccountsRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(offerbookRestApi).to(OfferbookRestApi.class);
                bind(tradeRestApi).to(TradeRestApi.class);
                bind(userIdentityRestApi).to(UserIdentityRestApi.class);
                bind(marketPriceRestApi).to(MarketPriceRestApi.class);
                bind(settingsRestApi).to(SettingsRestApi.class);
                bind(explorerRestApi).to(ExplorerRestApi.class);
                bind(paymentAccountsRestApi).to(PaymentAccountsRestApi.class);
            }
        });
    }
}
