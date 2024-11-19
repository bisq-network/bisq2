package bisq.rest_api_node;

import bisq.bonded_roles.BondedRolesService;
import bisq.network.NetworkService;
import bisq.rest_api.BaseRestApiResourceConfig;
import bisq.rest_api.RestApiService;
import bisq.rest_api_node.report.ReportRestApi;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityRestApi;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
public class RestApiResourceConfig extends BaseRestApiResourceConfig {
    public RestApiResourceConfig(RestApiService.Config config,
                                 NetworkService networkService,
                                 UserService userService,
                                 BondedRolesService bondedRolesService) {
        super(config);

        //todo apply filtering with whiteListEndPoints/whiteListEndPoints

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(UserIdentityRestApi.class);
        register(ReportRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new UserIdentityRestApi(userService.getUserIdentityService())).to(UserIdentityRestApi.class);
                bind(new ReportRestApi(networkService, bondedRolesService)).to(ReportRestApi.class);
            }
        });
    }
}
