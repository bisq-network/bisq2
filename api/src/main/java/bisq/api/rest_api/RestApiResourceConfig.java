package bisq.api.rest_api;

import bisq.api.ApiConfig;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Getter
@Slf4j
public class RestApiResourceConfig extends BaseRestApiResourceConfig {
    public RestApiResourceConfig(ApiConfig apiConfig,
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
        super(apiConfig, permissionService, sessionAuthenticationService);

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(OfferbookRestApi.class);
        register(TradeRestApi.class);
        register(TradeChatMessagesRestApi.class);
        register(UserIdentityRestApi.class);
        register(MarketPriceRestApi.class);
        register(SettingsRestApi.class);
        register(ExplorerRestApi.class);
        register(PaymentAccountsRestApi.class);
        register(ReputationRestApi.class);
        register(UserProfileRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(offerbookRestApi).to(OfferbookRestApi.class);
                bind(tradeRestApi).to(TradeRestApi.class);
                bind(tradeChatMessagesRestApi).to(TradeChatMessagesRestApi.class);
                bind(userIdentityRestApi).to(UserIdentityRestApi.class);
                bind(marketPriceRestApi).to(MarketPriceRestApi.class);
                bind(settingsRestApi).to(SettingsRestApi.class);
                bind(explorerRestApi).to(ExplorerRestApi.class);
                bind(paymentAccountsRestApi).to(PaymentAccountsRestApi.class);
                bind(reputationRestApi).to(ReputationRestApi.class);
                bind(userProfileRestApi).to(UserProfileRestApi.class);
            }
        });
    }
}
