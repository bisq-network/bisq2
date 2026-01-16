package bisq.api.rest_api;

import bisq.api.ApiConfig;
import bisq.api.access.filter.authn.RestApiAuthenticationFilter;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.filter.authz.RestApiAuthorizationFilter;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.rest_api.util.SwaggerResolution;
import org.glassfish.jersey.internal.inject.AbstractBinder;

public abstract class BaseRestApiResourceConfig extends BaseResourceConfig {
    public BaseRestApiResourceConfig(ApiConfig apiConfig,
                                     PermissionService<RestPermissionMapping> permissionService,
                                     SessionAuthenticationService sessionAuthenticationService) {
        super();

        if (apiConfig.isAuthRequired()) {
            register(new RestApiAuthenticationFilter(sessionAuthenticationService));
            register(new RestApiAuthorizationFilter(permissionService,
                    apiConfig.getRestAllowEndpoints(),
                    apiConfig.getRestDenyEndpoints()));
        }

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(SwaggerResolution.class);

        String swaggerBaseUrl = apiConfig.getRestServerApiBasePath();
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new SwaggerResolution(swaggerBaseUrl)).to(SwaggerResolution.class);
            }
        });
    }
}
