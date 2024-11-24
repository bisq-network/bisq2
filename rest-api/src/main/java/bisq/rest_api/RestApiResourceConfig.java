package bisq.rest_api;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceRestApi;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.OfferbookRestApi;
import bisq.network.NetworkService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityRestApi;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
public class RestApiResourceConfig extends BaseRestApiResourceConfig {
    public RestApiResourceConfig(RestApiService.Config config,
                                 NetworkService networkService,
                                 UserService userService,
                                 BondedRolesService bondedRolesService,
                                 ChatService chatService) {
        super(config);

        //todo apply filtering with whiteListEndPoints/whiteListEndPoints

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(UserIdentityRestApi.class);
        register(OfferbookRestApi.class);
        register(MarketPriceRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new UserIdentityRestApi(userService.getUserIdentityService())).to(UserIdentityRestApi.class);
                bind(new MarketPriceRestApi(bondedRolesService.getMarketPriceService())).to(MarketPriceRestApi.class);
                bind(new OfferbookRestApi(chatService.getBisqEasyOfferbookChannelService())).to(OfferbookRestApi.class);
            }
        });
    }
}
